package com.substream.exception;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

/**
 * Base class for all business-rule violations in SubStream (unchecked).
 */
public class SubStreamException extends RuntimeException {

    public SubStreamException(String message) {
        super(message);
    }
}
