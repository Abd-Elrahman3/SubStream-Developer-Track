package com.substream.external;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import java.util.Objects;

/**
 * Immutable result of a charge attempt. Created only through the static factories.
 */
public final class PaymentResult {

    private final boolean success;
    private final String transactionId;
    private final String failureReason;

    private PaymentResult(boolean success, String transactionId, String failureReason) {
        this.success = success;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }

    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, Objects.requireNonNull(transactionId, "transactionId"), null);
    }

    public static PaymentResult failed(String reason) {
        return new PaymentResult(false, null, Objects.requireNonNull(reason, "reason"));
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentResult other)) {
            return false;
        }
        return success == other.success
                && Objects.equals(transactionId, other.transactionId)
                && Objects.equals(failureReason, other.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, transactionId, failureReason);
    }

    @Override
    public String toString() {
        return success
                ? "PaymentResult{success, transactionId='" + transactionId + "'}"
                : "PaymentResult{failed, reason='" + failureReason + "'}";
    }
}
