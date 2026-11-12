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

    FREE,
    PRO,
    ENTERPRISE;

    public BigDecimal getMonthlyPrice() {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1, section 4.2");
    }

    public int getUserLimit() {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1, section 4.2");
    }

    /** {@code true} if the user limit is {@code Integer.MAX_VALUE}. */
    public boolean isUnlimited() {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1, section 4.2");
    }

    /** {@code true} if {@code seats} is at least 1 and at most the user limit. */
    public boolean canAccommodate(int seats) {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1, section 4.2");
    }

    /** {@code true} if this plan has a higher tier than {@code other}. Never use {@code ordinal()}. */
    public boolean isUpgradeFrom(SubscriptionPlan other) {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1, section 4.2");
    }
}
