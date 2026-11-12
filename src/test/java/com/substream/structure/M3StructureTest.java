package com.substream.structure;

import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.domain.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * PROVIDED — DO NOT MODIFY.
 *
 * Checks that the class Subscription of M3 matches README section 4.
 * Run it when you finish M3: right-click the file → Run.
 *
 * It checks the structure only: names, parameters, return types, private fields, no setters.
 * It does not check the logic. The rules of the milestone are checked by the NIX bot on every
 * Merge Request, and the final evaluation checks them with more cases.
 */
@Tag("structure")
@DisplayName("M3 structure")
class M3StructureTest {

    @Test
    @DisplayName("Subscription")
    void subscription() {
        StructureCheck check = new StructureCheck(Subscription.class);
        check.constructor(String.class, String.class, SubscriptionPlan.class, int.class, LocalDate.class);
        check.getter(String.class, "getId");
        check.getter(String.class, "getCustomerId");
        check.getter(SubscriptionPlan.class, "getPlan");
        check.getter(SubscriptionStatus.class, "getStatus");
        check.getter(int.class, "getSeatCount");
        check.getter(LocalDate.class, "getStartDate");
        check.getter(int.class, "getBillingCycle");
        check.getter(int.class, "getFailedPaymentAttempts");
        check.instanceMethod(LocalDate.class, "getRenewalDate");
        check.instanceMethod(boolean.class, "isDue", LocalDate.class);
        check.instanceMethod(void.class, "changePlan", SubscriptionPlan.class);
        check.instanceMethod(void.class, "recordPaymentSuccess");
        check.instanceMethod(boolean.class, "recordPaymentFailure", int.class);
        check.instanceMethod(void.class, "cancel");
        check.fields("id", "customerId", "plan", "status", "seatCount", "startDate",
                "billingCycle", "failedPaymentAttempts");
        check.noField("renewalDate");
        check.verify();
    }
}
