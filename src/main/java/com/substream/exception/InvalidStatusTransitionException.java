package com.substream.exception;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import com.substream.domain.SubscriptionStatus;

public class InvalidStatusTransitionException extends SubStreamException {

    public InvalidStatusTransitionException(SubscriptionStatus from, SubscriptionStatus to) {
        super("Cannot change subscription status from " + from + " to " + to);
    }
}
