/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Invoice;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * D7. Pure calculations, written with the Stream API (no loops).
 */
public final class BillingMetrics {

    private BillingMetrics() {
    }

    /** BR-601 */
    public static BigDecimal calculateMrr(Collection<Subscription> subscriptions) {
        // TODO: D7, BR-601
        return subscriptions.stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .map(subscription -> subscription.getPlan().getMonthlyPrice())
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .setScale(2);
    }

    /** BR-602 */
    public static List<Invoice> findOverdue(Collection<Invoice> invoices, LocalDate today) {
        // TODO: D7, BR-602
        return invoices.stream()
                .filter(invoice -> invoice.isOverdue(today))
                .sorted(Comparator.comparing(Invoice::getDueDate))
                .toList();
    }
}
