/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PR-1 to PR-5: the price of one plan for one billing period, with discount and VAT. */
public record Quote(String id, String plan, BillingPeriod period, String country, String couponCode,
                    BigDecimal subtotal, BigDecimal discount, BigDecimal vat, BigDecimal total, LocalDate quotedOn) {
}
