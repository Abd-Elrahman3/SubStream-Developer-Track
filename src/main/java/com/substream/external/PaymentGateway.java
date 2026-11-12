package com.substream.external;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import com.substream.exception.PaymentGatewayTimeoutException;

import java.math.BigDecimal;

/**
 * Third-party payment provider. In unit tests, replace it with a test double (a lambda is enough).
 */
public interface PaymentGateway {

    PaymentResult charge(String customerId, BigDecimal amount) throws PaymentGatewayTimeoutException;
}
