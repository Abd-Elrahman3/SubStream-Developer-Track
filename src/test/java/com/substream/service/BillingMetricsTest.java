package com.substream.service;

import com.substream.domain.Invoice;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingMetricsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    @Test
    void mrrMatchesTheBusinessRuleExample() {
        Subscription proOne = subscription("pro-one", SubscriptionPlan.PRO, 1);
        Subscription proTen = subscription("pro-ten", SubscriptionPlan.PRO, 10);
        Subscription enterprise = subscription("enterprise", SubscriptionPlan.ENTERPRISE, 30);
        Subscription free = subscription("free", SubscriptionPlan.FREE, 1);
        Subscription suspended = subscription("suspended", SubscriptionPlan.PRO, 2);
        suspended.recordPaymentFailure(1);
        Subscription cancelled = subscription("cancelled", SubscriptionPlan.ENTERPRISE, 20);
        cancelled.cancel();

        BigDecimal result = BillingMetrics.calculateMrr(
                List.of(proOne, proTen, enterprise, free, suspended, cancelled));

        // BR-601: each active subscription contributes one price, regardless of seats.
        assertEquals(0, new BigDecimal("257.00").compareTo(result));
        assertEquals(2, result.scale());
    }

    @Test
    void emptySubscriptionsHaveZeroMrrWithTwoDecimalPlaces() {
        BigDecimal result = BillingMetrics.calculateMrr(List.of());

        assertEquals(0, new BigDecimal("0.00").compareTo(result));
        assertEquals(2, result.scale());
    }

    @Test
    void subscriptionsWithoutAnActiveOneHaveZeroMrr() {
        Subscription suspended = subscription("suspended", SubscriptionPlan.PRO, 1);
        suspended.recordPaymentFailure(1);
        Subscription cancelled = subscription("cancelled", SubscriptionPlan.ENTERPRISE, 1);
        cancelled.cancel();

        BigDecimal result = BillingMetrics.calculateMrr(List.of(suspended, cancelled));

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
        assertEquals(2, result.scale());
    }

    @Test
    void overdueInvoicesAreUnpaidStrictlyPastDueAndSortedOldestFirst() {
        Invoice newer = invoice("newer", TODAY.minusDays(1));
        Invoice oldest = invoice("oldest", TODAY.minusDays(10));
        Invoice dueToday = invoice("today", TODAY);
        Invoice future = invoice("future", TODAY.plusDays(1));
        Invoice paid = invoice("paid", TODAY.minusDays(20));
        paid.markPaid();

        List<Invoice> result = BillingMetrics.findOverdue(
                List.of(newer, dueToday, paid, oldest, future), TODAY);

        assertEquals(List.of(oldest, newer), result);
    }

    @Test
    void emptyInvoicesHaveNoOverdueInvoices() {
        assertTrue(BillingMetrics.findOverdue(List.of(), TODAY).isEmpty());
    }

    @Test
    void invoicesWithoutOverdueEntriesReturnAnEmptyList() {
        Invoice paid = invoice("paid", TODAY.minusDays(1));
        paid.markPaid();

        List<Invoice> result = BillingMetrics.findOverdue(
                List.of(paid, invoice("today", TODAY), invoice("future", TODAY.plusDays(1))),
                TODAY);

        assertTrue(result.isEmpty());
    }

    private Subscription subscription(String id, SubscriptionPlan plan, int seats) {
        return new Subscription(id, "customer", plan, seats, TODAY.minusMonths(1));
    }

    private Invoice invoice(String id, LocalDate dueDate) {
        return new Invoice(id, "subscription", TODAY.minusMonths(1),
                new BigDecimal("29.00"), TODAY.minusMonths(1), dueDate);
    }
}
