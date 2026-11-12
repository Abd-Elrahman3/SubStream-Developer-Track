package com.substream.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * M2. One bill for one subscription period (README, section 4.6).
 *
 * TODO: add the private fields and one getter per field (no setters).
 */
public class Invoice implements Identifiable<String> {

    /**
     * A new invoice is {@code UNPAID}; the amount is stored with scale 2 ({@code 29} becomes {@code 29.00}).
     * Throws {@code IllegalArgumentException} for a negative amount.
     * Store the values as given: which values a renewal uses (BR-302) is the billing engine's job in D5.
     */
    public Invoice(String id, String subscriptionId, LocalDate periodStart,
                   BigDecimal amount, LocalDate issueDate, LocalDate dueDate) {
        // TODO: M2, BR-302
        throw new UnsupportedOperationException("TODO: M2, BR-302");
    }

    @Override
    public String getId() {
        // TODO: M2
        throw new UnsupportedOperationException("TODO: M2");
    }

    /** Used by the provided {@code InMemoryInvoiceRepository.findByKey}. */
    public String getSubscriptionId() {
        // TODO: M2
        throw new UnsupportedOperationException("TODO: M2");
    }

    /** Used by the provided {@code InMemoryInvoiceRepository.findByKey}. */
    public LocalDate getPeriodStart() {
        // TODO: M2
        throw new UnsupportedOperationException("TODO: M2");
    }

    /** BR-305: UNPAID -> PAID. Throws {@code IllegalStateException} if already paid. */
    public void markPaid() {
        // TODO: M2, BR-305
        throw new UnsupportedOperationException("TODO: M2, BR-305");
    }

    /** BR-602 */
    public boolean isOverdue(LocalDate today) {
        // TODO: M2, BR-602
        throw new UnsupportedOperationException("TODO: M2, BR-602");
    }

    /** PROVIDED (section 4.7): equality is based on {@code id} only, as you wrote it for Customer in M1. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Invoice other && Objects.equals(getId(), other.getId());
    }

    /** PROVIDED (section 4.7). */
    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
