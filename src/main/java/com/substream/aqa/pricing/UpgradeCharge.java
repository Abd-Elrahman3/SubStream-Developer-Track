/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PR-6: what a customer pays to move to a more expensive plan in the middle of a monthly cycle. */
public record UpgradeCharge(String fromPlan, String toPlan, LocalDate cycleStart, LocalDate cycleEnd,
                            int cycleDays, int remainingDays, BigDecimal amount, BigDecimal vat, BigDecimal total) {
}
