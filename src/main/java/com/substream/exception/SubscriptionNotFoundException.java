package com.substream.exception;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

public class SubscriptionNotFoundException extends SubStreamException {

    public SubscriptionNotFoundException(String subscriptionId) {
        super("Subscription not found: " + subscriptionId);
    }
}
