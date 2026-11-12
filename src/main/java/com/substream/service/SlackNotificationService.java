/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;

/** D7 (BR-603). Delivers to a fixed Slack channel. */
public class SlackNotificationService extends TemplateNotificationService {

    /** The channel is used exactly as given: {@code IllegalArgumentException} unless it starts with '#' followed by at least one character. */
    public SlackNotificationService(String channel) {
        // TODO: D7, BR-603
        throw new UnsupportedOperationException("TODO: D7, BR-603");
    }

    @Override
    protected String resolveRecipient(Customer customer) {
        // TODO: D7, BR-603
        throw new UnsupportedOperationException("TODO: D7, BR-603");
    }

    @Override
    protected String format(String recipient, String message) {
        // TODO: D7, BR-603
        throw new UnsupportedOperationException("TODO: D7, BR-603");
    }
}
