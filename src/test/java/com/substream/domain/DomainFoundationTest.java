package com.substream.domain;

import com.substream.exception.InvalidCustomerDataException;
import com.substream.exception.InvalidPlanTransitionException;
import com.substream.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for the M1–M3 foundations required by the service milestones. */
class DomainFoundationTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 31);

    @Test
    void customerTrimsNameNormalizesEmailAndUsesIdForEquality() {
        Customer customer = new Customer("customer", "  Acme  Corp \t", " BILLING@ACME.COM ", START);
        Customer sameId = new Customer("customer", "Other", "other@example.com", START.plusDays(1));

        assertEquals("Acme  Corp", customer.getName());
        assertEquals("billing@acme.com", customer.getEmail());
        assertEquals(START, customer.getCreatedAt());
        assertEquals(customer, sameId);
        assertEquals(customer.hashCode(), sameId.hashCode());
    }

    @Test
    void invalidCustomerValuesAreRejectedButMinimalValidEmailIsAllowed() {
        assertThrows(InvalidCustomerDataException.class,
                () -> new Customer("id", " \t\n", "a@b.c", START));
        assertThrows(InvalidCustomerDataException.class,
                () -> new Customer("id", "a".repeat(101), "a@b.c", START));
        assertThrows(InvalidCustomerDataException.class,
                () -> new Customer("id", "Acme", "a@@b.c", START));
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid("a b@c.d"));
        assertFalse(EmailValidator.isValid("a@bc"));
        assertTrue(EmailValidator.isValid(" a@. "));
    }

    @Test
    void seatCountsMustFitEachPlanIncludingPositiveMinimum() {
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Subscription("id", "customer", plan, 0, START));
            assertThrows(IllegalArgumentException.class,
                    () -> new Subscription("id", "customer", plan, -1, START));
            assertEquals(plan.getUserLimit(),
                    new Subscription("id", "customer", plan, plan.getUserLimit(), START).getSeatCount());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new Subscription("id", "customer", SubscriptionPlan.PRO, 11, START));
    }

    @Test
    void renewalDatesStayAnchoredAtMonthEnd() {
        Subscription subscription = subscription(SubscriptionPlan.PRO, 1);
        assertEquals(LocalDate.of(2024, 2, 29), subscription.getRenewalDate());
        assertFalse(subscription.isDue(LocalDate.of(2024, 2, 28)));
        assertTrue(subscription.isDue(LocalDate.of(2024, 2, 29)));
        subscription.recordPaymentSuccess();
        assertEquals(LocalDate.of(2024, 3, 31), subscription.getRenewalDate());
        subscription.recordPaymentSuccess();
        assertEquals(LocalDate.of(2024, 4, 30), subscription.getRenewalDate());
    }

    @Test
    void failureSuspendsAtLimitAndForbiddenCallsLeaveStateUnchanged() {
        Subscription subscription = subscription(SubscriptionPlan.PRO, 1);
        LocalDate renewal = subscription.getRenewalDate();
        assertFalse(subscription.recordPaymentFailure(3));
        assertFalse(subscription.recordPaymentFailure(3));
        assertTrue(subscription.recordPaymentFailure(3));
        assertEquals(3, subscription.getFailedPaymentAttempts());
        assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
        assertEquals(renewal, subscription.getRenewalDate());
        assertThrows(InvalidStatusTransitionException.class, subscription::recordPaymentSuccess);
        assertThrows(InvalidStatusTransitionException.class, () -> subscription.recordPaymentFailure(3));
        assertEquals(0, subscription.getBillingCycle());
        assertEquals(3, subscription.getFailedPaymentAttempts());
        subscription.cancel();
        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
        assertThrows(InvalidStatusTransitionException.class, subscription::cancel);
    }

    @Test
    void successClearsConsecutiveFailuresAndAdvancesOneCycle() {
        Subscription subscription = subscription(SubscriptionPlan.PRO, 1);
        subscription.recordPaymentFailure(3);
        subscription.recordPaymentFailure(3);
        subscription.recordPaymentSuccess();
        assertEquals(0, subscription.getFailedPaymentAttempts());
        assertEquals(1, subscription.getBillingCycle());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void planChangesHonorCapacityAndLifecycleWithoutPartialChanges() {
        Subscription subscription = subscription(SubscriptionPlan.ENTERPRISE, 10);
        subscription.changePlan(SubscriptionPlan.PRO);
        assertEquals(SubscriptionPlan.PRO, subscription.getPlan());
        assertThrows(InvalidPlanTransitionException.class, () -> subscription.changePlan(SubscriptionPlan.FREE));
        assertThrows(InvalidPlanTransitionException.class, () -> subscription.changePlan(SubscriptionPlan.PRO));
        assertEquals(SubscriptionPlan.PRO, subscription.getPlan());
        subscription.changePlan(SubscriptionPlan.ENTERPRISE);
        subscription.cancel();
        assertThrows(InvalidPlanTransitionException.class, () -> subscription.changePlan(SubscriptionPlan.PRO));
        assertEquals(SubscriptionPlan.ENTERPRISE, subscription.getPlan());
    }

    @Test
    void invoiceUsesScaleTwoAndStrictOverdueBoundaryAndCannotBePaidTwice() {
        LocalDate due = START.plusDays(14);
        Invoice invoice = new Invoice("invoice", "subscription", START, new BigDecimal("29"), START, due);
        assertEquals(2, invoice.getAmount().scale());
        assertEquals(0, new BigDecimal("29.00").compareTo(invoice.getAmount()));
        assertFalse(invoice.isOverdue(due));
        assertTrue(invoice.isOverdue(due.plusDays(1)));
        invoice.markPaid();
        assertFalse(invoice.isOverdue(due.plusDays(1)));
        assertThrows(IllegalStateException.class, invoice::markPaid);
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> new Invoice("negative", "subscription", START, new BigDecimal("-0.001"), START, due));
    }

    private Subscription subscription(SubscriptionPlan plan, int seats) {
        return new Subscription("subscription", "customer", plan, seats, START);
    }
}
