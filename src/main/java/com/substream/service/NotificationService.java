/* DEVELOPER TRACK ONLY. AQA-track students can ignore this package. */
package com.substream.service;

import com.substream.domain.Customer;
import com.substream.domain.Subscription;

/**
 * PROVIDED. Sends a notice to the customer when a subscription is suspended (BR-307).
 */
public interface NotificationService {

    void notifySuspension(Customer customer, Subscription subscription);
}
