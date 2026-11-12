package com.substream.structure;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import com.substream.external.PaymentGateway;
import com.substream.repository.CustomerRepository;
import com.substream.repository.InvoiceRepository;
import com.substream.repository.SubscriptionRepository;
import com.substream.service.BillingConfig;
import com.substream.service.BillingEngine;
import com.substream.service.BillingMetrics;
import com.substream.service.BillingRunSummary;
import com.substream.service.NotificationService;
import com.substream.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * PROVIDED — DO NOT MODIFY.
 *
 * DEVELOPER TRACK ONLY. Checks that the services keep the signatures they are given in the starter.
 * It is green from the start: it turns red only if a provided signature is changed.
 * Run it after each Developer milestone (D4 – D7): right-click the file → Run.
 *
 * It checks the structure only: names, parameters, return types, private fields, no setters.
 * It does not check the logic. The rules of the milestone are checked by the NIX bot on every
 * Merge Request, and the final evaluation checks them with more cases.
 */
@Tag("structure")
@DisplayName("Services structure (Developer track)")
class ServicesStructureTest {

    @Test
    @DisplayName("D4 · SubscriptionService")
    void subscriptionService() {
        StructureCheck check = new StructureCheck(SubscriptionService.class);
        check.constructor(CustomerRepository.class, SubscriptionRepository.class, Clock.class);
        check.instanceMethod(Customer.class, "registerCustomer", String.class, String.class);
        check.instanceMethod(Subscription.class, "subscribe", String.class, SubscriptionPlan.class, int.class);
        check.instanceMethod(Subscription.class, "changePlan", String.class, SubscriptionPlan.class);
        check.instanceMethod(Subscription.class, "cancel", String.class);
        check.verify();
    }

    @Test
    @DisplayName("D5 – D6 · BillingEngine")
    void billingEngine() {
        StructureCheck check = new StructureCheck(BillingEngine.class);
        check.constructor(SubscriptionRepository.class, CustomerRepository.class, InvoiceRepository.class,
                PaymentGateway.class, NotificationService.class, BillingConfig.class, Clock.class);
        check.instanceMethod(BillingRunSummary.class, "runRenewals");
        check.verify();
    }

    @Test
    @DisplayName("D7 · BillingMetrics")
    void billingMetrics() {
        StructureCheck check = new StructureCheck(BillingMetrics.class);
        check.staticMethod(BigDecimal.class, "calculateMrr", Collection.class);
        check.staticMethod(List.class, "findOverdue", Collection.class, LocalDate.class);
        check.verify();
    }
}
