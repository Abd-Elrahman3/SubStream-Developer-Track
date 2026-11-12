package com.substream.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * M3. A customer's subscription to one plan (README, section 4.4).
 *
 * TODO (M3): add the private fields and one getter per field (no setters).
 *            All state changes go through the methods below.
 */
public class Subscription implements Identifiable<String> {

    /**
     * BR-103: throws {@code IllegalArgumentException} if the plan cannot accommodate {@code seatCount}.
     * {@code null} for id, customerId, plan or startDate throws {@code NullPointerException}.
     * A new subscription is {@code ACTIVE}, with {@code billingCycle = 0} and no failed payment attempts.
     */
    public Subscription(String id, String customerId, SubscriptionPlan plan, int seatCount, LocalDate startDate) {
        // TODO: M3, BR-103
        throw new UnsupportedOperationException("TODO: M3, BR-103");
    }

    @Override
    public String getId() {
        // TODO: M3
        throw new UnsupportedOperationException("TODO: M3");
    }

    /** BR-201: calculated from {@code startDate} and {@code billingCycle}; never stored in a field. */
    public LocalDate getRenewalDate() {
        // TODO: M3, BR-201
        throw new UnsupportedOperationException("TODO: M3, BR-201");
    }

    /**
     * BR-301: {@code true} when the subscription is ACTIVE and {@code getRenewalDate() <= today}:
     * the renewal date is today, or an earlier date that is not paid yet.
     */
    public boolean isDue(LocalDate today) {
        // TODO: M3, BR-301
        throw new UnsupportedOperationException("TODO: M3, BR-301");
    }

    /** BR-104: throws {@code InvalidPlanTransitionException} when the change is not allowed. */
    public void changePlan(SubscriptionPlan target) {
        // TODO: M3, BR-104
        throw new UnsupportedOperationException("TODO: M3, BR-104");
    }

    /**
     * BR-305: {@code billingCycle + 1} and {@code failedPaymentAttempts = 0}; the status stays ACTIVE.
     * BR-401: on a SUSPENDED or CANCELLED subscription, throws
     * {@code new InvalidStatusTransitionException(currentStatus, SubscriptionStatus.ACTIVE)}.
     * Only this subscription's fields change here: marking the invoice PAID is the billing engine's job (D5).
     */
    public void recordPaymentSuccess() {
        // TODO: M3, BR-305, BR-401
        throw new UnsupportedOperationException("TODO: M3, BR-305, BR-401");
    }

    /**
     * BR-306: {@code failedPaymentAttempts + 1}; the renewal date does not move.
     * BR-307: when the counter reaches {@code maxFailedAttempts}, the status becomes SUSPENDED and the
     * method returns {@code true}. Sending the notification is the billing engine's job (D6), not this method's.
     * BR-401: on a SUSPENDED or CANCELLED subscription, throws
     * {@code new InvalidStatusTransitionException(currentStatus, SubscriptionStatus.SUSPENDED)}.
     *
     * @return {@code true} if this failure caused the subscription to be suspended
     */
    public boolean recordPaymentFailure(int maxFailedAttempts) {
        // TODO: M3, BR-306, BR-307, BR-401
        throw new UnsupportedOperationException("TODO: M3, BR-306, BR-307, BR-401");
    }

    /** BR-401 */
    public void cancel() {
        // TODO: M3, BR-401
        throw new UnsupportedOperationException("TODO: M3, BR-401");
    }

    /** PROVIDED (section 4.7): equality is based on {@code id} only, as you wrote it for Customer in M1. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Subscription other && Objects.equals(getId(), other.getId());
    }

    /** PROVIDED (section 4.7). */
    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
