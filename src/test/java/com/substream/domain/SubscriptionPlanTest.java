package com.substream.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROVIDED EXAMPLE — DO NOT MODIFY.
 *
 * A complete JUnit 5 test class for SubscriptionPlan (README, section 4.2).
 * It fails until your M1 is done; when it passes, that part of M1 is right.
 *
 * AQA track: use it as the pattern for the test classes you write in A4.
 * Each technique used here is explained in docs/primers/JUNIT5.md.
 */
@DisplayName("SubscriptionPlan")
class SubscriptionPlanTest {

    // ---- 1. A plain test: arrange, act, assert -----------------------------------

    @Test
    @DisplayName("PRO costs 29.00 per month")
    void proMonthlyPrice() {
        BigDecimal price = SubscriptionPlan.PRO.getMonthlyPrice();

        // Money: compare BigDecimal with compareTo, so 29.0 and 29.00 count as equal
        assertEquals(0, new BigDecimal("29.00").compareTo(price), "monthly price of PRO");
    }

    // ---- 2. Several assertions about one thing: all are reported, not only the first

    @Test
    @DisplayName("each plan has the user limit of section 4.2")
    void userLimits() {
        assertAll(
                () -> assertEquals(1, SubscriptionPlan.FREE.getUserLimit()),
                () -> assertEquals(10, SubscriptionPlan.PRO.getUserLimit()),
                () -> assertEquals(Integer.MAX_VALUE, SubscriptionPlan.ENTERPRISE.getUserLimit()));
    }

    // ---- 3. Parameterized test: one method, one row per case ---------------------

    @ParameterizedTest(name = "{0} accommodates {1} seat(s): {2}")
    @CsvSource({
            // plan,     seats,  expected
            "FREE,       1,      true",    // the limit itself
            "FREE,       2,      false",   // just above the limit
            "PRO,        10,     true",
            "PRO,        11,     false",
            "PRO,        0,      false",   // below the minimum of 1
            "ENTERPRISE, 100000, true"
    })
    void canAccommodate(SubscriptionPlan plan, int seats, boolean expected) {
        assertEquals(expected, plan.canAccommodate(seats));
    }

    @ParameterizedTest(name = "every plan rejects {0} seats")
    @ValueSource(ints = {0, -1})
    void noPlanAcceptsZeroOrNegativeSeats(int seats) {
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            assertFalse(plan.canAccommodate(seats), plan + " must reject " + seats);
        }
    }

    // ---- 4. Grouping related tests ------------------------------------------------

    @Nested
    @DisplayName("isUpgradeFrom")
    class IsUpgradeFrom {

        @Test
        @DisplayName("a higher tier is an upgrade")
        void higherTier() {
            assertTrue(SubscriptionPlan.ENTERPRISE.isUpgradeFrom(SubscriptionPlan.PRO));
            assertTrue(SubscriptionPlan.PRO.isUpgradeFrom(SubscriptionPlan.FREE));
        }

        @Test
        @DisplayName("a lower tier is not an upgrade")
        void lowerTier() {
            assertFalse(SubscriptionPlan.FREE.isUpgradeFrom(SubscriptionPlan.ENTERPRISE));
        }

        @ParameterizedTest(name = "{0} is not an upgrade from itself")
        @EnumSource(SubscriptionPlan.class)
        void samePlan(SubscriptionPlan plan) {
            assertFalse(plan.isUpgradeFrom(plan));
        }
    }

    @Test
    @DisplayName("only ENTERPRISE is unlimited")
    void unlimited() {
        assertTrue(SubscriptionPlan.ENTERPRISE.isUnlimited());
        assertFalse(SubscriptionPlan.PRO.isUnlimited());
        assertFalse(SubscriptionPlan.FREE.isUnlimited());
    }
}
