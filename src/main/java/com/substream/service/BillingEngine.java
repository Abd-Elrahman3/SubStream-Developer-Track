/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.external.PaymentGateway;
import com.substream.repository.CustomerRepository;
import com.substream.repository.InvoiceRepository;
import com.substream.repository.SubscriptionRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * D5 (renewal happy path) and D6 (failures and suspension).
 * Processes due subscriptions one by one (README, section 5, Module C).
 * "Today" is always {@code LocalDate.now(clock)} (BR-202).
 */
public class BillingEngine {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final BillingConfig config;
    private final Clock clock;

    public BillingEngine(SubscriptionRepository subscriptionRepository,
                         CustomerRepository customerRepository,
                         InvoiceRepository invoiceRepository,
                         PaymentGateway paymentGateway,
                         NotificationService notificationService,
                         BillingConfig config,
                         Clock clock) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.config = Objects.requireNonNull(config);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * BR-301 to BR-309. Bills every due subscription at most once and returns the run summary.
     */
    public BillingRunSummary runRenewals() {
        // TODO: D5, D6
        throw new UnsupportedOperationException("TODO: D5, D6");
    }
}
