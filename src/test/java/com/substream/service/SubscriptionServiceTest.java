package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.domain.SubscriptionStatus;
import com.substream.exception.CustomerNotFoundException;
import com.substream.exception.InvalidCustomerDataException;
import com.substream.exception.InvalidPlanTransitionException;
import com.substream.exception.InvalidStatusTransitionException;
import com.substream.exception.SubscriptionNotFoundException;
import com.substream.repository.InMemoryCustomerRepository;
import com.substream.repository.InMemorySubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** D4: use cases use the supplied clock, persist successful changes, and propagate errors. */
class SubscriptionServiceTest {

    // It is April 1 in this clock's zone, but still March 31 in UTC.
    private final Clock clock = Clock.fixed(
            Instant.parse("2025-03-31T23:30:00Z"), ZoneId.of("Europe/Helsinki"));
    private final LocalDate today = LocalDate.of(2025, 4, 1);
    private final CountingCustomerRepository customers = new CountingCustomerRepository();
    private final CountingSubscriptionRepository subscriptions = new CountingSubscriptionRepository();
    private final SubscriptionService service = new SubscriptionService(customers, subscriptions, clock);

    @Test
    void registrationUsesFixedClockAndSavesCustomer() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");

        assertEquals(today, customer.getCreatedAt());
        assertEquals(customer, customers.findById(customer.getId()).orElseThrow());
        assertEquals(1, customers.saveCalls);
    }

    @Test
    void registrationCreatesDistinctRandomUuidIds() {
        Customer first = service.registerCustomer("Acme", "hello@acme.com");
        Customer second = service.registerCustomer("Acme", "hello@acme.com");

        assertNotEquals(first.getId(), second.getId());
        assertRandomUuid(first.getId());
        assertRandomUuid(second.getId());
    }

    @Test
    void unknownCustomerThrowsWithoutSavingAnything() {
        assertThrows(CustomerNotFoundException.class,
                () -> service.subscribe("missing", SubscriptionPlan.PRO, 2));

        assertEquals(0, subscriptions.count());
        assertEquals(0, subscriptions.saveCalls);
        assertEquals(0, customers.saveCalls);
    }

    @Test
    void invalidCustomerDataIsPropagatedWithoutSaving() {
        assertThrows(InvalidCustomerDataException.class,
                () -> service.registerCustomer(" ", "hello@acme.com"));
        assertThrows(InvalidCustomerDataException.class,
                () -> service.registerCustomer("Acme", "invalid-email"));

        assertEquals(0, customers.count());
        assertEquals(0, customers.saveCalls);
    }

    @Test
    void subscribeUsesFixedClockAndAllowsMultipleSubscriptionsForOneCustomer() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");

        Subscription first = service.subscribe(customer.getId(), SubscriptionPlan.PRO, 2);
        Subscription second = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);

        assertEquals(today, first.getStartDate());
        assertEquals(SubscriptionStatus.ACTIVE, first.getStatus());
        assertEquals(customer.getId(), first.getCustomerId());
        assertEquals(SubscriptionPlan.PRO, first.getPlan());
        assertEquals(2, first.getSeatCount());
        assertEquals(first, subscriptions.findById(first.getId()).orElseThrow());
        assertEquals(second, subscriptions.findById(second.getId()).orElseThrow());
        assertEquals(2, subscriptions.count());
        assertNotEquals(first.getId(), second.getId());
        assertRandomUuid(first.getId());
        assertRandomUuid(second.getId());
        assertEquals(1, customers.saveCalls, "Subscribing must not save the customer again");
    }

    @Test
    void invalidSeatCountIsPropagatedWithoutSavingSubscription() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");

        assertThrows(IllegalArgumentException.class,
                () -> service.subscribe(customer.getId(), SubscriptionPlan.FREE, 2));

        assertEquals(0, subscriptions.count());
        assertEquals(0, subscriptions.saveCalls);
    }

    @Test
    void unknownSubscriptionCannotBeChangedOrCancelled() {
        assertThrows(SubscriptionNotFoundException.class,
                () -> service.changePlan("missing", SubscriptionPlan.PRO));
        assertThrows(SubscriptionNotFoundException.class, () -> service.cancel("missing"));

        assertEquals(0, subscriptions.count());
        assertEquals(0, subscriptions.saveCalls);
    }

    @Test
    void changePlanSavesOnlyTheSelectedSubscription() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");
        Subscription first = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);
        Subscription other = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);

        Subscription changed = service.changePlan(first.getId(), SubscriptionPlan.PRO);

        assertEquals(first.getId(), changed.getId());
        assertEquals(SubscriptionPlan.PRO, subscriptions.findById(first.getId()).orElseThrow().getPlan());
        assertEquals(SubscriptionPlan.FREE, other.getPlan());
        assertEquals(3, subscriptions.saveCalls, "Two creations plus the one plan change");
    }

    @Test
    void forbiddenPlanChangeIsPropagatedWithoutSaving() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");
        Subscription subscription = service.subscribe(customer.getId(), SubscriptionPlan.PRO, 2);

        assertThrows(InvalidPlanTransitionException.class,
                () -> service.changePlan(subscription.getId(), SubscriptionPlan.FREE));

        assertEquals(SubscriptionPlan.PRO, subscription.getPlan());
        assertEquals(1, subscriptions.saveCalls, "Only the original creation was saved");
    }

    @Test
    void cancelSavesOnlyTheSelectedSubscription() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");
        Subscription first = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);
        Subscription other = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);

        Subscription cancelled = service.cancel(first.getId());

        assertEquals(first.getId(), cancelled.getId());
        assertEquals(SubscriptionStatus.CANCELLED,
                subscriptions.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, other.getStatus());
        assertEquals(3, subscriptions.saveCalls, "Two creations plus the one cancellation");
    }

    @Test
    void repeatedCancellationIsPropagatedWithoutAnotherSave() {
        Customer customer = service.registerCustomer("Acme", "hello@acme.com");
        Subscription subscription = service.subscribe(customer.getId(), SubscriptionPlan.FREE, 1);
        service.cancel(subscription.getId());

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.cancel(subscription.getId()));

        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
        assertEquals(2, subscriptions.saveCalls, "The rejected second cancellation must not save");
    }

    private static void assertRandomUuid(String id) {
        UUID uuid = UUID.fromString(id);
        assertEquals(id, uuid.toString(), "ID uses the standard 36-character UUID form");
        assertEquals(4, uuid.version(), "ID is a random UUID");
    }

    // Counting saves detects accidental writes even when repository size stays unchanged.
    private static class CountingCustomerRepository extends InMemoryCustomerRepository {
        private int saveCalls;

        @Override
        public Customer save(Customer customer) {
            saveCalls++;
            return super.save(customer);
        }
    }

    private static class CountingSubscriptionRepository extends InMemorySubscriptionRepository {
        private int saveCalls;

        @Override
        public Subscription save(Subscription subscription) {
            saveCalls++;
            return super.save(subscription);
        }
    }
}
