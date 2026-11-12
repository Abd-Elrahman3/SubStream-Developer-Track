/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

/** No coupon has this code. API: 404. */
public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String message) {
        super(message);
    }
}
