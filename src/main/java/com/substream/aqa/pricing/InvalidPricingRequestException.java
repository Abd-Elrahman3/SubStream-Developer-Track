/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

/** The request is invalid: unknown plan or country, missing or wrong value. API: 400. */
public class InvalidPricingRequestException extends RuntimeException {

    public InvalidPricingRequestException(String message) {
        super(message);
    }
}
