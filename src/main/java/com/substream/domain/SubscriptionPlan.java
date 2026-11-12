package com.substream.domain;

import java.math.BigDecimal;

/**
 * M1. The three plans SubStream sells (README, section 4.2).
 *
 * TODO: give every constant its tier, monthly price and user limit,
 *       add a constructor and the private fields, and complete the two getters.
 *       {@code tier} has no getter: it is used only inside {@link #isUpgradeFrom}.
 */
public enum SubscriptionPlan {

    FREE(1, new BigDecimal("0.00"), 1),
    PRO(2, new BigDecimal("29.00"), 10),
    ENTERPRISE(3, new BigDecimal("199.00"), Integer.MAX_VALUE);

    private final int tier;
    private final BigDecimal monthlyPrice;
    private final int userLimit;

    SubscriptionPlan(int tier, BigDecimal monthlyPrice, int userLimit) {
        this.tier = tier;
        this.monthlyPrice = monthlyPrice;
        this.userLimit = userLimit;
    }

    public BigDecimal getMonthlyPrice() {
        // TODO: M1
        return monthlyPrice;
    }

    public int getUserLimit() {
        // TODO: M1
        return userLimit;
    }

    /** {@code true} if the user limit is {@code Integer.MAX_VALUE}. */
    public boolean isUnlimited() {
        // TODO: M1
        return userLimit == Integer.MAX_VALUE;
    }

    /** {@code true} if {@code seats} is at least 1 and at most the user limit. */
    public boolean canAccommodate(int seats) {
        // TODO: M1
        return seats >= 1 && seats <= userLimit;
    }

    /** {@code true} if this plan has a higher tier than {@code other}. Never use {@code ordinal()}. */
    public boolean isUpgradeFrom(SubscriptionPlan other) {
        // TODO: M1
        return tier > other.tier;
    }
}
