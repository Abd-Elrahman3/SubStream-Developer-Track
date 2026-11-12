/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Invoice;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.exception.CustomerNotFoundException;
import com.substream.exception.PaymentGatewayTimeoutException;
import com.substream.external.PaymentGateway;
import com.substream.repository.CustomerRepository;
import com.substream.repository.InvoiceRepository;
import com.substream.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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
        LocalDate today = LocalDate.now(clock);
        int eligible = 0;
        int succeeded = 0;
        int failed = 0;

        for (Subscription subscription : subscriptionRepository.findAll()) {
            if (!subscription.isDue(today)) {
                continue;
            }
            eligible++;
            if (processRenewal(subscription, today)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        return new BillingRunSummary(eligible, succeeded, failed, 0);
    }

    private boolean processRenewal(Subscription subscription, LocalDate today) {
        Customer customer = customerRepository.findById(subscription.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(subscription.getCustomerId()));
        Invoice invoice = createInvoice(subscription, today);
        boolean paid = subscription.getPlan() == SubscriptionPlan.FREE
                || charge(customer, invoice);

        if (paid) {
            invoice.markPaid();
            subscription.recordPaymentSuccess();
        }

        invoiceRepository.save(invoice);
        subscriptionRepository.save(subscription);
        return paid;
    }

    private Invoice createInvoice(Subscription subscription, LocalDate today) {
        return new Invoice(UUID.randomUUID().toString(), subscription.getId(),
                subscription.getRenewalDate(), subscription.getPlan().getMonthlyPrice(),
                today, today.plusDays(config.invoiceDueDays()));
    }

    private boolean charge(Customer customer, Invoice invoice) {
        try {
            return paymentGateway.charge(customer.getId(), invoice.getAmount()).isSuccess();
        } catch (PaymentGatewayTimeoutException exception) {
            // D6 will convert gateway exceptions into recorded payment failures.
            throw new IllegalStateException("The payment gateway timed out", exception);
        }
    }
}
