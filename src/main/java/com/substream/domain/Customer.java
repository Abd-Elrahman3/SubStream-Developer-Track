package com.substream.domain;

import com.substream.exception.InvalidCustomerDataException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * M1. A corporate customer (README, section 4.1).
 *
 * TODO: add the private fields and one getter per field (no setters).
 */
public final class Customer implements Identifiable<String> {

    public static final int MAX_NAME_LENGTH = 100;

    private final String id;
    private final String name;
    private final String email;
    private final LocalDate createdAt;

    /**
     * BR-101 / BR-102: throws {@code InvalidCustomerDataException} for an invalid name or email.
     * Stores the name trimmed and the email trimmed and lower-cased.
     */
    public Customer(String id, String name, String email, LocalDate createdAt) {
        // TODO: BR-101, BR-102
        if (name == null || name.trim().isBlank() || name.trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidCustomerDataException("Name must contain between 1 and 100 characters");
        }
        if (!EmailValidator.isValid(email)) {
            throw new InvalidCustomerDataException("Invalid email address");
        }
        this.id = id;
        this.name = name.trim();
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        // TODO: M1
        return id;
    }

    /** Used by the provided {@code EmailNotificationService}. */
    public String getEmail() {
        // TODO: M1
        return email;
    }

    public String getName() {
        return name;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    /** Section 4.7: equality is based on {@code id} only. */
    @Override
    public boolean equals(Object o) {
        // TODO: section 4.7
        if (this == o) {
            return true;
        }
        return o instanceof Customer other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        // TODO: section 4.7
        return Objects.hashCode(id);
    }
}
