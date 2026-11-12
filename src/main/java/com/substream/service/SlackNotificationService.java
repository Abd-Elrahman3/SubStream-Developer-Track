/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;

/** D7 (BR-603). Delivers to a fixed Slack channel. */
public class SlackNotificationService extends TemplateNotificationService {

    private final String channel;

    /** The channel is used exactly as given: {@code IllegalArgumentException} unless it starts with '#' followed by at least one character. */
    public SlackNotificationService(String channel) {
        // TODO: D7, BR-603
        if (channel == null || !channel.startsWith("#") || channel.length() < 2) {
            throw new IllegalArgumentException("Channel must start with # followed by at least one character");
        }
        this.channel = channel;
    }

    @Override
    protected String resolveRecipient(Customer customer) {
        // TODO: D7, BR-603
        return channel;
    }

    @Override
    protected String format(String recipient, String message) {
        // TODO: D7, BR-603
        return "SLACK " + recipient + ": " + message;
    }
}
