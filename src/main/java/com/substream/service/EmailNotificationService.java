/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;

/**
 * PROVIDED (BR-603). A complete example of a Template Method subclass.
 *
 * The parent class {@link TemplateNotificationService} owns the algorithm: it builds the
 * message and stores the delivered line. This subclass only fills in the two steps that
 * differ per channel: who receives the message, and how the delivered line looks.
 * {@code SlackNotificationService} (D7) follows the same pattern.
 */
public class EmailNotificationService extends TemplateNotificationService {

    @Override
    protected String resolveRecipient(Customer customer) {
        return customer.getEmail();
    }

    @Override
    protected String format(String recipient, String message) {
        return "EMAIL to " + recipient + ": " + message;
    }
}
