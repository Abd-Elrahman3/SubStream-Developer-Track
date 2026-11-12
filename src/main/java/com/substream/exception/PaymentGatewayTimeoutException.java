package com.substream.exception;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

/**
 * Checked exception: the payment gateway is an external boundary,
 * so callers must handle a timeout explicitly.
 */
public class PaymentGatewayTimeoutException extends Exception {

    public PaymentGatewayTimeoutException(String message) {
        super(message);
    }
}
