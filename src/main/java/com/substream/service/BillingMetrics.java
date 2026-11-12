/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Invoice;
import com.substream.domain.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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
        throw new UnsupportedOperationException("TODO: D7, BR-601");
    }

    /** BR-602 */
    public static List<Invoice> findOverdue(Collection<Invoice> invoices, LocalDate today) {
        // TODO: D7, BR-602
        throw new UnsupportedOperationException("TODO: D7, BR-602");
    }
}
