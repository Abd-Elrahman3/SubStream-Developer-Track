/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

/**
 * PROVIDED. Result of one billing run (BR-309).
 * {@code eligible == succeeded + failed + suspended}.
 */
public record BillingRunSummary(int eligible, int succeeded, int failed, int suspended) {
}
