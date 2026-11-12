/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

/**
 * PROVIDED. Billing settings. Business code reads limits from here, never from hard-coded numbers.
 */
public record BillingConfig(int maxFailedAttempts, int invoiceDueDays) {

    public BillingConfig {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts must be at least 1");
        }
        if (invoiceDueDays < 0) {
            throw new IllegalArgumentException("invoiceDueDays must not be negative");
        }
    }

    /** The production settings: suspend after 3 failed payments, invoices due in 14 days. */
    public static BillingConfig defaults() {
        return new BillingConfig(3, 14);
    }
}
