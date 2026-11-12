/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.repository.CustomerRepository;
import com.substream.repository.SubscriptionRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * D4. Use cases for customers and subscriptions (README, section 5, Module E).
 * "Today" is always {@code LocalDate.now(clock)} (BR-202).
 */
public class SubscriptionService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public SubscriptionService(CustomerRepository customerRepository,
                               SubscriptionRepository subscriptionRepository,
                               Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /** BR-501 */
    public Customer registerCustomer(String name, String email) {
        // TODO: D4, BR-501
        throw new UnsupportedOperationException("TODO: D4, BR-501");
    }

    /** BR-502 */
    public Subscription subscribe(String customerId, SubscriptionPlan plan, int seatCount) {
        // TODO: D4, BR-502
        throw new UnsupportedOperationException("TODO: D4, BR-502");
    }

    /** BR-503 */
    public Subscription changePlan(String subscriptionId, SubscriptionPlan targetPlan) {
        // TODO: D4, BR-503
        throw new UnsupportedOperationException("TODO: D4, BR-503");
    }

    /** BR-503 */
    public Subscription cancel(String subscriptionId) {
        // TODO: D4, BR-503
        throw new UnsupportedOperationException("TODO: D4, BR-503");
    }
}
