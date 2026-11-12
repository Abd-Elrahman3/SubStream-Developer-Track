package com.substream.exception;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

public class CustomerNotFoundException extends SubStreamException {

    public CustomerNotFoundException(String customerId) {
        super("Customer not found: " + customerId);
    }
}
