package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Invoice;
import com.substream.domain.InvoiceStatus;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.domain.SubscriptionStatus;
import com.substream.exception.PaymentGatewayTimeoutException;
import com.substream.external.PaymentGateway;
import com.substream.external.PaymentResult;
import com.substream.repository.InMemoryCustomerRepository;
import com.substream.repository.InMemoryInvoiceRepository;
import com.substream.repository.InMemorySubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** D5 and D6: fixed dates, real repositories, and controllable external collaborators. */
class BillingEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 3, 1);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2024-03-01T00:00:00Z"), ZoneOffset.UTC);

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final InMemorySubscriptionRepository subscriptions = new InMemorySubscriptionRepository();
    private final RecordingInvoiceRepository invoices = new RecordingInvoiceRepository();
    private final List<String> notified = new ArrayList<>();
    private final NotificationService notifications =
            (customer, subscription) -> notified.add(subscription.getId());

    @BeforeEach
    void createCustomer() {
        customers.save(new Customer("c-1", "Acme", "billing@acme.com", TODAY.minusYears(1)));
    }

    @Test
    void billsDueProAtMonthlyPriceAndRenews() {
        Subscription subscription = dueSubscription("s-1", SubscriptionPlan.PRO, 10);
        AtomicInteger charges = new AtomicInteger();
        PaymentGateway gateway = (customerId, amount) -> {
            charges.incrementAndGet();
            assertEquals("c-1", customerId);
            assertMoney("29.00", amount); // Ten seats still cost one plan price.
            assertEquals(0, invoices.savedStatuses.size()); // Save only after the outcome.
            return PaymentResult.success("tx-1");
        };

        BillingRunSummary summary = engine(gateway).runRenewals();

        Invoice invoice = invoices.findByKey("s-1", TODAY).orElseThrow();
        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 1, 0, 0), summary),
                () -> assertEquals(1, charges.get()),
                () -> assertEquals(1, invoices.count()),
                () -> assertEquals(List.of(InvoiceStatus.PAID), invoices.savedStatuses),
                () -> assertMoney("29.00", invoice.getAmount()),
                () -> assertEquals(2, invoice.getAmount().scale()),
                () -> assertEquals(TODAY, invoice.getIssueDate()),
                () -> assertEquals(TODAY.plusDays(14), invoice.getDueDate()),
                () -> assertEquals(1, subscription.getBillingCycle()),
                () -> assertEquals(TODAY.plusMonths(1), subscription.getRenewalDate()),
                () -> assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus()),
                () -> assertTrue(notified.isEmpty()));
    }

    @Test
    void freeRenewsWithoutCallingGateway() {
        Subscription subscription = dueSubscription("free", SubscriptionPlan.FREE, 1);
        PaymentGateway gateway = (customerId, amount) -> fail("FREE must never call the gateway");

        BillingRunSummary summary = engine(gateway).runRenewals();

        Invoice invoice = invoices.findByKey("free", TODAY).orElseThrow();
        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 1, 0, 0), summary),
                () -> assertMoney("0.00", invoice.getAmount()),
                () -> assertEquals(InvoiceStatus.PAID, invoice.getStatus()),
                () -> assertEquals(List.of(InvoiceStatus.PAID), invoices.savedStatuses),
                () -> assertEquals(1, subscription.getBillingCycle()),
                () -> assertEquals(TODAY.plusMonths(1), subscription.getRenewalDate()));
    }

    @Test
    void skipsFutureCancelledAndSuspendedSubscriptions() {
        subscriptions.save(new Subscription("future", "c-1", SubscriptionPlan.PRO,
                1, TODAY));
        Subscription cancelled = dueSubscription("cancelled", SubscriptionPlan.PRO, 1);
        cancelled.cancel();
        Subscription suspended = dueSubscription("suspended", SubscriptionPlan.PRO, 1);
        suspended.recordPaymentFailure(1);
        PaymentGateway gateway = (customerId, amount) -> fail("No subscription is eligible");

        BillingRunSummary summary = engine(gateway).runRenewals();

        assertEquals(new BillingRunSummary(0, 0, 0, 0), summary);
        assertEquals(0, invoices.count());
        assertTrue(notified.isEmpty());
    }

    @Test
    void billsAnOverdueSubscriptionOnlyOncePerRun() {
        Subscription subscription = new Subscription("late", "c-1", SubscriptionPlan.PRO,
                1, LocalDate.of(2024, 1, 1));
        subscriptions.save(subscription);
        AtomicInteger charges = new AtomicInteger();
        PaymentGateway gateway = (customerId, amount) -> {
            charges.incrementAndGet();
            return PaymentResult.success("tx-late");
        };

        BillingRunSummary summary = engine(gateway).runRenewals();

        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 1, 0, 0), summary),
                () -> assertEquals(1, charges.get()),
                () -> assertEquals(1, subscription.getBillingCycle()),
                () -> assertEquals(TODAY, subscription.getRenewalDate()),
                () -> assertEquals(LocalDate.of(2024, 2, 1),
                        invoices.findAll().get(0).getPeriodStart()));
    }

    @Test
    void usesConfiguredInvoiceDueDays() {
        dueSubscription("s-1", SubscriptionPlan.PRO, 1);
        BillingEngine engine = engine((id, amount) -> PaymentResult.success("tx"),
                notifications, new BillingConfig(3, 0), CLOCK);

        engine.runRenewals();

        assertEquals(TODAY, invoices.findAll().get(0).getDueDate());
    }

    @Test
    void usesTheInjectedClocksTimeZoneForToday() {
        // Same instant as CLOCK, but the local date in this zone is March 2.
        Clock localClock = Clock.fixed(Instant.parse("2024-03-01T23:30:00Z"),
                ZoneOffset.ofHours(2));
        Subscription subscription = new Subscription("local-date", "c-1", SubscriptionPlan.PRO,
                1, LocalDate.of(2024, 2, 2));
        subscriptions.save(subscription);

        BillingRunSummary summary = engine((id, amount) -> PaymentResult.success("tx"),
                notifications, BillingConfig.defaults(), localClock).runRenewals();

        assertEquals(new BillingRunSummary(1, 1, 0, 0), summary);
        assertEquals(LocalDate.of(2024, 3, 2), invoices.findAll().get(0).getIssueDate());
    }

    @Test
    void declineKeepsInvoiceUnpaidAndRenewalDateUnchanged() {
        assertPaymentFailure((id, amount) -> PaymentResult.failed("declined"));
    }

    @Test
    void timeoutCountsAsPaymentFailure() {
        assertPaymentFailure((id, amount) -> {
            throw new PaymentGatewayTimeoutException("provider timed out");
        });
    }

    @Test
    void gatewayRuntimeExceptionCountsAsPaymentFailure() {
        assertPaymentFailure((id, amount) -> {
            throw new IllegalStateException("provider unavailable");
        });
    }

    @Test
    void retryReusesInvoiceAndResetsFailureCounterAfterSuccess() {
        Subscription subscription = dueSubscription("s-1", SubscriptionPlan.PRO, 1);
        engine((id, amount) -> PaymentResult.failed("declined")).runRenewals();
        Invoice original = invoices.findByKey("s-1", TODAY).orElseThrow();
        String invoiceId = original.getId();
        // An outstanding invoice retains its original price even if the plan changes.
        subscription.changePlan(SubscriptionPlan.ENTERPRISE);
        Clock tomorrow = Clock.fixed(Instant.parse("2024-03-02T00:00:00Z"), ZoneOffset.UTC);
        PaymentGateway gateway = (id, amount) -> {
            assertMoney("29.00", amount);
            assertEquals(1, invoices.savedStatuses.size());
            return PaymentResult.success("tx-retry");
        };

        BillingRunSummary summary = engine(gateway, notifications,
                BillingConfig.defaults(), tomorrow).runRenewals();

        Invoice retried = invoices.findByKey("s-1", TODAY).orElseThrow();
        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 1, 0, 0), summary),
                () -> assertEquals(1, invoices.count()),
                () -> assertEquals(invoiceId, retried.getId()),
                () -> assertEquals(TODAY, retried.getIssueDate()),
                () -> assertEquals(TODAY.plusDays(14), retried.getDueDate()),
                () -> assertMoney("29.00", retried.getAmount()),
                () -> assertEquals(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PAID), invoices.savedStatuses),
                () -> assertEquals(0, subscription.getFailedPaymentAttempts()),
                () -> assertEquals(1, subscription.getBillingCycle()),
                () -> assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus()),
                () -> assertTrue(notified.isEmpty()));
    }

    @Test
    void threeDeclinesSuspendAndNotifyExactlyOnce() {
        Subscription subscription = dueSubscription("s-1", SubscriptionPlan.PRO, 1);
        AtomicInteger charges = new AtomicInteger();
        BillingEngine engine = engine((id, amount) -> {
            charges.incrementAndGet();
            return PaymentResult.failed("declined");
        });

        assertEquals(new BillingRunSummary(1, 0, 1, 0), engine.runRenewals());
        assertEquals(new BillingRunSummary(1, 0, 1, 0), engine.runRenewals());
        assertEquals(2, subscription.getFailedPaymentAttempts());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertTrue(notified.isEmpty());

        BillingRunSummary thirdRun = engine.runRenewals();

        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 0, 0, 1), thirdRun),
                () -> assertEquals(3, subscription.getFailedPaymentAttempts()),
                () -> assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus()),
                () -> assertEquals(List.of("s-1"), notified),
                () -> assertEquals(1, invoices.count()),
                () -> assertEquals(InvoiceStatus.UNPAID, invoices.findAll().get(0).getStatus()),
                () -> assertEquals(TODAY, subscription.getRenewalDate()));

        assertEquals(new BillingRunSummary(0, 0, 0, 0), engine.runRenewals());
        assertEquals(3, charges.get());
        assertEquals(3, invoices.savedStatuses.size());
        assertEquals(List.of("s-1"), notified);
    }

    @Test
    void usesConfiguredSuspensionLimitAndNotifiesWithCustomerAndSubscription() {
        Subscription subscription = dueSubscription("s-1", SubscriptionPlan.PRO, 1);
        List<String> notices = new ArrayList<>();
        NotificationService recorder = (customer, suspended) -> {
            assertEquals(SubscriptionStatus.SUSPENDED, suspended.getStatus());
            assertEquals(1, suspended.getFailedPaymentAttempts());
            notices.add(customer.getId() + ":" + suspended.getId());
        };

        BillingRunSummary summary = engine((id, amount) -> PaymentResult.failed("declined"),
                recorder, new BillingConfig(1, 14), CLOCK).runRenewals();

        assertEquals(new BillingRunSummary(1, 0, 0, 1), summary);
        assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
        assertEquals(List.of("c-1:s-1"), notices);
    }

    @Test
    void missingCustomerCountsAsFailedAndDoesNotStopLaterSubscriptions() {
        subscriptions.save(new Subscription("orphan", "missing", SubscriptionPlan.PRO,
                1, TODAY.minusMonths(1)));
        Subscription healthy = dueSubscription("healthy", SubscriptionPlan.PRO, 1);
        List<String> chargedCustomers = new ArrayList<>();

        BillingRunSummary summary = engine((id, amount) -> {
            chargedCustomers.add(id);
            return PaymentResult.success("tx");
        }).runRenewals();

        assertEquals(new BillingRunSummary(2, 1, 1, 0), summary);
        assertEquals(List.of("c-1"), chargedCustomers);
        assertEquals(1, healthy.getBillingCycle());
        assertEquals(InvoiceStatus.PAID,
                invoices.findByKey("healthy", TODAY).orElseThrow().getStatus());
    }

    @Test
    void notificationExceptionCountsAsFailedAndDoesNotStopLaterSubscriptions() {
        Subscription brokenNotice = dueSubscription("broken-notice", SubscriptionPlan.PRO, 1);
        brokenNotice.recordPaymentFailure(3);
        brokenNotice.recordPaymentFailure(3);
        Subscription healthy = dueSubscription("healthy", SubscriptionPlan.FREE, 1);
        AtomicInteger notices = new AtomicInteger();
        NotificationService throwingNotifier = (customer, subscription) -> {
            notices.incrementAndGet();
            throw new IllegalStateException("notification unavailable");
        };

        BillingRunSummary summary = engine((id, amount) -> PaymentResult.failed("declined"),
                throwingNotifier, BillingConfig.defaults(), CLOCK).runRenewals();

        assertEquals(new BillingRunSummary(2, 1, 1, 0), summary);
        assertEquals(SubscriptionStatus.SUSPENDED, brokenNotice.getStatus());
        assertEquals(1, notices.get());
        assertEquals(1, healthy.getBillingCycle());
    }

    @Test
    void mixedRunCountsEveryEligibleSubscriptionExactlyOnce() {
        dueSubscription("paid", SubscriptionPlan.FREE, 1);
        dueSubscription("declined", SubscriptionPlan.PRO, 1);
        Subscription atLimit = dueSubscription("at-limit", SubscriptionPlan.ENTERPRISE, 1);
        atLimit.recordPaymentFailure(3);
        atLimit.recordPaymentFailure(3);

        BillingRunSummary summary = engine((id, amount) -> PaymentResult.failed("declined"))
                .runRenewals();

        assertEquals(new BillingRunSummary(3, 1, 1, 1), summary);
        assertEquals(summary.eligible(), summary.succeeded() + summary.failed() + summary.suspended());
        assertEquals(List.of("at-limit"), notified);
    }

    @Test
    void emptyRepositoryProducesEmptySummary() {
        PaymentGateway gateway = (id, amount) -> fail("There is nothing to charge");

        assertEquals(new BillingRunSummary(0, 0, 0, 0), engine(gateway).runRenewals());
        assertEquals(0, invoices.count());
    }

    private Subscription dueSubscription(String id, SubscriptionPlan plan, int seats) {
        return subscriptions.save(new Subscription(id, "c-1", plan, seats, TODAY.minusMonths(1)));
    }

    private BillingEngine engine(PaymentGateway gateway) {
        return engine(gateway, notifications, BillingConfig.defaults(), CLOCK);
    }

    private BillingEngine engine(PaymentGateway gateway, NotificationService notifier,
                                 BillingConfig config, Clock clock) {
        return new BillingEngine(subscriptions, customers, invoices, gateway, notifier, config, clock);
    }

    private void assertPaymentFailure(PaymentGateway gateway) {
        Subscription subscription = dueSubscription("s-1", SubscriptionPlan.PRO, 1);

        BillingRunSummary summary = engine(gateway).runRenewals();

        assertAll(
                () -> assertEquals(new BillingRunSummary(1, 0, 1, 0), summary),
                () -> assertEquals(List.of(InvoiceStatus.UNPAID), invoices.savedStatuses),
                () -> assertEquals(1, subscription.getFailedPaymentAttempts()),
                () -> assertEquals(0, subscription.getBillingCycle()),
                () -> assertEquals(TODAY, subscription.getRenewalDate()),
                () -> assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus()),
                () -> assertTrue(notified.isEmpty()));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    /** Captures status at save time, so mutating a saved object cannot hide an early save. */
    private static class RecordingInvoiceRepository extends InMemoryInvoiceRepository {
        private final List<InvoiceStatus> savedStatuses = new ArrayList<>();

        @Override
        public Invoice save(Invoice invoice) {
            savedStatuses.add(invoice.getStatus());
            return super.save(invoice);
        }
    }
}
