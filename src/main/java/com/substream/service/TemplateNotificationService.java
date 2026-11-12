/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;

import java.util.List;

/**
 * D7 (BR-603). Template Method: the message is always built the same way;
 * subclasses decide only who receives it and how the delivered line looks.
 *
 * TODO: keep the delivered lines in a private field.
 */
public abstract class TemplateNotificationService implements NotificationService {

    /** Builds the message of BR-603 and stores {@code format(resolveRecipient(customer), message)}. */
    @Override
    public final void notifySuspension(Customer customer, Subscription subscription) {
        // TODO: D7, BR-603
        throw new UnsupportedOperationException("TODO: D7, BR-603");
    }

    /** Every delivered line, oldest first. The returned list cannot be modified. */
    public List<String> getSentMessages() {
        // TODO: D7, BR-603
        throw new UnsupportedOperationException("TODO: D7, BR-603");
    }

    protected abstract String resolveRecipient(Customer customer);

    protected abstract String format(String recipient, String message);
}
