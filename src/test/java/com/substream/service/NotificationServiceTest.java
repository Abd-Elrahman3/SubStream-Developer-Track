package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;
import com.substream.domain.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private final Customer customer = new Customer("customer", "Acme Corp", "billing@acme.com", TODAY);

    @Test
    void emailUsesTheExactSuspensionMessage() {
        EmailNotificationService service = new EmailNotificationService();

        service.notifySuspension(customer, suspendedSubscription("sub-pro", SubscriptionPlan.PRO, 3));

        assertEquals(List.of("EMAIL to billing@acme.com: Dear Acme Corp, your PRO subscription sub-pro"
                + " has been suspended after 3 failed payment attempt(s). Please update your payment method."),
                service.getSentMessages());
    }

    @Test
    void slackUsesTheConfiguredChannelAndActualFailureCount() {
        SlackNotificationService service = new SlackNotificationService("#billing");

        service.notifySuspension(customer, suspendedSubscription("sub-enterprise", SubscriptionPlan.ENTERPRISE, 1));

        assertEquals(List.of("SLACK #billing: Dear Acme Corp, your ENTERPRISE subscription sub-enterprise"
                + " has been suspended after 1 failed payment attempt(s). Please update your payment method."),
                service.getSentMessages());
    }

    @Test
    void deliveredMessagesStayInDeliveryOrderAndCannotBeChanged() {
        EmailNotificationService service = new EmailNotificationService();
        service.notifySuspension(customer, suspendedSubscription("first", SubscriptionPlan.PRO, 1));
        service.notifySuspension(customer, suspendedSubscription("second", SubscriptionPlan.ENTERPRISE, 2));

        List<String> messages = service.getSentMessages();

        assertEquals(List.of(
                "EMAIL to billing@acme.com: Dear Acme Corp, your PRO subscription first"
                        + " has been suspended after 1 failed payment attempt(s). Please update your payment method.",
                "EMAIL to billing@acme.com: Dear Acme Corp, your ENTERPRISE subscription second"
                        + " has been suspended after 2 failed payment attempt(s). Please update your payment method."),
                messages);
        assertThrows(UnsupportedOperationException.class, () -> messages.add("fake"));
        assertThrows(UnsupportedOperationException.class, messages::clear);
        assertEquals(2, service.getSentMessages().size());
    }

    @Test
    void eachServiceHasItsOwnInitiallyEmptyMessageHistory() {
        EmailNotificationService first = new EmailNotificationService();
        EmailNotificationService second = new EmailNotificationService();
        SlackNotificationService slack = new SlackNotificationService("#billing");
        assertTrue(first.getSentMessages().isEmpty());

        first.notifySuspension(customer, suspendedSubscription("sub-pro", SubscriptionPlan.PRO, 1));

        assertEquals(1, first.getSentMessages().size());
        assertTrue(second.getSentMessages().isEmpty());
        assertTrue(slack.getSentMessages().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "#", "billing", " #billing"})
    void invalidSlackChannelsAreRejected(String channel) {
        assertThrows(IllegalArgumentException.class, () -> new SlackNotificationService(channel));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#billing ", "# ", "#Billing"})
    void validSlackChannelsAreUsedExactlyAsGiven(String channel) {
        SlackNotificationService service = new SlackNotificationService(channel);

        service.notifySuspension(customer, suspendedSubscription("sub-pro", SubscriptionPlan.PRO, 1));

        assertEquals(List.of("SLACK " + channel + ": Dear Acme Corp, your PRO subscription sub-pro"
                + " has been suspended after 1 failed payment attempt(s). Please update your payment method."),
                service.getSentMessages());
    }

    private Subscription suspendedSubscription(String id, SubscriptionPlan plan, int attempts) {
        Subscription subscription = new Subscription(id, customer.getId(), plan, 1, TODAY.minusMonths(1));
        for (int attempt = 0; attempt < attempts; attempt++) {
            subscription.recordPaymentFailure(attempts);
        }
        return subscription;
    }
}
