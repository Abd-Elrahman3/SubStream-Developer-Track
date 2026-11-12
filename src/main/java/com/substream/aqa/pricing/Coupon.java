/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PR-3: a discount code. {@code value} is a percentage for {@link CouponType#PERCENT}
 * and an amount for {@link CouponType#FIXED}.
 */
public record Coupon(String code, CouponType type, BigDecimal value, LocalDate validUntil, BigDecimal minimumSubtotal) {
}
