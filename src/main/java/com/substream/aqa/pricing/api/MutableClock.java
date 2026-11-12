/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing.api;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * A clock whose date can be moved, so that dates can be tested through the API
 * ("time travel"). Always midnight UTC of the current date.
 */
public final class MutableClock extends Clock {

    private volatile LocalDate date;

    public MutableClock(LocalDate date) {
        this.date = Objects.requireNonNull(date, "date");
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date, "date");
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
