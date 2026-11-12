package com.substream.domain;

import java.time.LocalDate;

/**
 * M1. A corporate customer (README, section 4.1).
 *
 * TODO: add the private fields and one getter per field (no setters).
 */
public final class Customer implements Identifiable<String> {

    public static final int MAX_NAME_LENGTH = 100;

    /**
     * BR-101 / BR-102: throws {@code InvalidCustomerDataException} for an invalid name or email.
     * Stores the name trimmed and the email trimmed and lower-cased.
     */
    public Customer(String id, String name, String email, LocalDate createdAt) {
        // TODO: BR-101, BR-102
        throw new UnsupportedOperationException("TODO: BR-101, BR-102");
    }

    @Override
    public String getId() {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1");
    }

    /** Used by the provided {@code EmailNotificationService}. */
    public String getEmail() {
        // TODO: M1
        throw new UnsupportedOperationException("TODO: M1");
    }

    /** Section 4.7: equality is based on {@code id} only. */
    @Override
    public boolean equals(Object o) {
        // TODO: section 4.7
        throw new UnsupportedOperationException("TODO: section 4.7");
    }

    @Override
    public int hashCode() {
        // TODO: section 4.7
        throw new UnsupportedOperationException("TODO: section 4.7");
    }
}
