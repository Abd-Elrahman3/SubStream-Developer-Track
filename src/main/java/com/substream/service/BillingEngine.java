/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Invoice;
import com.substream.domain.InvoiceStatus;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.domain.SubscriptionStatus;
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
        int suspended = 0;

        for (Subscription subscription : subscriptionRepository.findAll()) {
            if (!subscription.isDue(today)) {
                continue;
            }
            eligible++;
            try {
                Invoice invoice = processRenewal(subscription, today);
                if (invoice.getStatus() == InvoiceStatus.PAID) {
                    succeeded++;
                } else if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                    suspended++;
                } else {
                    failed++;
                }
            } catch (RuntimeException exception) {
                // BR-308: count an unexpected error as failed and continue with the next subscription.
                failed++;
            }
        }
        return new BillingRunSummary(eligible, succeeded, failed, suspended);
    }

    private Invoice processRenewal(Subscription subscription, LocalDate today) {
        Customer customer = customerRepository.findById(subscription.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(subscription.getCustomerId()));
        Invoice invoice = findOrCreateInvoice(subscription, today);
        boolean paid = subscription.getPlan() == SubscriptionPlan.FREE
                || charge(customer, invoice);
        boolean suspended = false;

        if (paid) {
            invoice.markPaid();
            subscription.recordPaymentSuccess();
        } else {
            suspended = subscription.recordPaymentFailure(config.maxFailedAttempts());
        }

        // BR-303: save the invoice exactly once, after the attempt's outcome is known.
        invoiceRepository.save(invoice);
        subscriptionRepository.save(subscription);

        if (suspended) {
            // Persist the suspension before notifying; a delivery error must not trigger another charge.
            notificationService.notifySuspension(customer, subscription);
        }
        return invoice;
    }

    private Invoice findOrCreateInvoice(Subscription subscription, LocalDate today) {
        LocalDate periodStart = subscription.getRenewalDate();
        Invoice invoice = invoiceRepository.findByKey(subscription.getId(), periodStart)
                .orElseGet(() -> new Invoice(UUID.randomUUID().toString(), subscription.getId(),
                        periodStart, subscription.getPlan().getMonthlyPrice(), today,
                        today.plusDays(config.invoiceDueDays())));
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("The renewal period already has a paid invoice");
        }
        return invoice;
    }

    private boolean charge(Customer customer, Invoice invoice) {
        try {
            return paymentGateway.charge(customer.getId(), invoice.getAmount()).isSuccess();
        } catch (PaymentGatewayTimeoutException | RuntimeException exception) {
            // BR-306: only gateway exceptions are converted into payment failures.
            return false;
        }
    }

}
