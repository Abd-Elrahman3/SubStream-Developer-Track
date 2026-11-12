/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

/** The request is valid but cannot be applied: expired coupon, subtotal below the coupon minimum, duplicate coupon code, or not an upgrade. API: 409. */
public class PricingConflictException extends RuntimeException {

    public PricingConflictException(String message) {
        super(message);
    }
}
