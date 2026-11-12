package com.substream.structure;

import com.substream.domain.Customer;
import com.substream.domain.EmailValidator;
import com.substream.domain.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PROVIDED — DO NOT MODIFY.
 *
 * Checks that the classes of M1 match README section 4: SubscriptionPlan, EmailValidator, Customer.
 * Run it when you finish M1: right-click the file → Run.
 *
 * It checks the structure only: names, parameters, return types, private fields, no setters.
 * It does not check the logic. The rules of the milestone are checked by the NIX bot on every
 * Merge Request, and the final evaluation checks them with more cases.
 */
@Tag("structure")
@DisplayName("M1 structure")
class M1StructureTest {

    @Test
    @DisplayName("SubscriptionPlan")
    void subscriptionPlan() {
        StructureCheck check = new StructureCheck(SubscriptionPlan.class);
        check.instanceMethod(BigDecimal.class, "getMonthlyPrice");
        check.instanceMethod(int.class, "getUserLimit");
        check.instanceMethod(boolean.class, "canAccommodate", int.class);
        check.instanceMethod(boolean.class, "isUnlimited");
        check.instanceMethod(boolean.class, "isUpgradeFrom", SubscriptionPlan.class);
        check.noPublicMethod("getTier");
        check.fields("tier", "monthlyPrice", "userLimit");
        check.constants("FREE", "PRO", "ENTERPRISE");
        check.verify();
    }

    @Test
    @DisplayName("EmailValidator")
    void emailValidator() {
        StructureCheck check = new StructureCheck(EmailValidator.class);
        check.staticMethod(boolean.class, "isValid", String.class);
        check.verify();
    }

    @Test
    @DisplayName("Customer")
    void customer() {
        StructureCheck check = new StructureCheck(Customer.class);
        check.constructor(String.class, String.class, String.class, LocalDate.class);
        check.getter(String.class, "getId");
        check.getter(String.class, "getName");
        check.getter(String.class, "getEmail");
        check.getter(LocalDate.class, "getCreatedAt");
        check.fields("id", "name", "email", "createdAt");
        check.publicConstant("MAX_NAME_LENGTH", int.class);
        check.verify();
    }
}
