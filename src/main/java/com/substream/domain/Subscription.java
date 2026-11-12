package com.substream.domain;

import com.substream.exception.InvalidPlanTransitionException;
import com.substream.exception.InvalidStatusTransitionException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * M3. A customer's subscription to one plan (README, section 4.4).
 *
 * TODO (M3): add the private fields and one getter per field (no setters).
 *            All state changes go through the methods below.
 */
public class Subscription implements Identifiable<String> {

    private final String id;
    private final String customerId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private final int seatCount;
    private final LocalDate startDate;
    private int billingCycle;
    private int failedPaymentAttempts;

    /**
     * BR-103: throws {@code IllegalArgumentException} if the plan cannot accommodate {@code seatCount}.
     * {@code null} for id, customerId, plan or startDate throws {@code NullPointerException}.
     * A new subscription is {@code ACTIVE}, with {@code billingCycle = 0} and no failed payment attempts.
     */
    public Subscription(String id, String customerId, SubscriptionPlan plan, int seatCount, LocalDate startDate) {
        // TODO: M3, BR-103
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        if (!plan.canAccommodate(seatCount)) {
            throw new IllegalArgumentException("Plan cannot accommodate the requested seats");
        }
        this.seatCount = seatCount;
        this.status = SubscriptionStatus.ACTIVE;
        this.billingCycle = 0;
        this.failedPaymentAttempts = 0;
    }

    @Override
    public String getId() {
        // TODO: M3
        return id;
    }

    /** BR-201: calculated from {@code startDate} and {@code billingCycle}; never stored in a field. */
    public LocalDate getRenewalDate() {
        // TODO: M3, BR-201
        return startDate.plusMonths((long) billingCycle + 1);
    }

    /**
     * BR-301: {@code true} when the subscription is ACTIVE and {@code getRenewalDate() <= today}:
     * the renewal date is today, or an earlier date that is not paid yet.
     */
    public boolean isDue(LocalDate today) {
        // TODO: M3, BR-301
        return status == SubscriptionStatus.ACTIVE && !getRenewalDate().isAfter(today);
    }

    /** BR-104: throws {@code InvalidPlanTransitionException} when the change is not allowed. */
    public void changePlan(SubscriptionPlan target) {
        // TODO: M3, BR-104
        if (status != SubscriptionStatus.ACTIVE) {
            throw new InvalidPlanTransitionException("Only an ACTIVE subscription can change plans");
        }
        Objects.requireNonNull(target, "target");
        if (target == plan) {
            throw new InvalidPlanTransitionException("Cannot change to the same plan");
        }
        if (!target.canAccommodate(seatCount)) {
            throw new InvalidPlanTransitionException("Target plan cannot accommodate the current seats");
        }
        plan = target;
    }

    /**
     * BR-305: {@code billingCycle + 1} and {@code failedPaymentAttempts = 0}; the status stays ACTIVE.
     * BR-401: on a SUSPENDED or CANCELLED subscription, throws
     * {@code new InvalidStatusTransitionException(currentStatus, SubscriptionStatus.ACTIVE)}.
     * Only this subscription's fields change here: marking the invoice PAID is the billing engine's job (D5).
     */
    public void recordPaymentSuccess() {
        // TODO: M3, BR-305, BR-401
        if (status != SubscriptionStatus.ACTIVE) {
            throw new InvalidStatusTransitionException(status, SubscriptionStatus.ACTIVE);
        }
        billingCycle++;
        failedPaymentAttempts = 0;
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
        if (status != SubscriptionStatus.ACTIVE) {
            throw new InvalidStatusTransitionException(status, SubscriptionStatus.SUSPENDED);
        }
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("Maximum failed attempts must be positive");
        }
        failedPaymentAttempts++;
        if (failedPaymentAttempts >= maxFailedAttempts) {
            status = SubscriptionStatus.SUSPENDED;
            return true;
        }
        return false;
    }

    /** BR-401 */
    public void cancel() {
        // TODO: M3, BR-401
        if (status == SubscriptionStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(status, SubscriptionStatus.CANCELLED);
        }
        status = SubscriptionStatus.CANCELLED;
    }

    public String getCustomerId() {
        return customerId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getBillingCycle() {
        return billingCycle;
    }

    public int getFailedPaymentAttempts() {
        return failedPaymentAttempts;
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
