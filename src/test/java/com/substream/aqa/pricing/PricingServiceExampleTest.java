/*
 * AQA TRACK ONLY. PROVIDED EXAMPLE (A5) — DO NOT MODIFY.
 */
package com.substream.aqa.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests on {@link PricingService}, with a fixed clock so every run sees the same date.
 * Use this class as the pattern for your own A5 tests; the rules are in docs/aqa/PRICING_SPEC.md.
 */
@DisplayName("PricingService (example)")
class PricingServiceExampleTest {

    private PricingService pricing;

    @BeforeEach
    void createService() {
        Clock clock = Clock.fixed(Instant.parse("2024-01-15T00:00:00Z"), ZoneOffset.UTC);
        pricing = new PricingService(clock);
    }

    @Test
    @DisplayName("PRO monthly in Egypt without a coupon: 29.00 + 14% VAT")
    void proMonthlyInEgypt() {
        Quote quote = pricing.quote("PRO", BillingPeriod.MONTHLY, "EG", null);

        // Money is compared with compareTo, so 29.0 and 29.00 count as equal.
        assertAll(
                () -> assertEquals(0, new BigDecimal("29.00").compareTo(quote.subtotal()), "subtotal"),
                () -> assertEquals(0, new BigDecimal("0.00").compareTo(quote.discount()), "discount"),
                () -> assertEquals(0, new BigDecimal("4.06").compareTo(quote.vat()), "vat"),
                () -> assertEquals(0, new BigDecimal("33.06").compareTo(quote.total()), "total"),
                () -> assertEquals(LocalDate.of(2024, 1, 15), quote.quotedOn(), "quotedOn"));
    }

    @Test
    @DisplayName("an unsupported country is rejected")
    void unsupportedCountryIsRejected() {
        assertThrows(InvalidPricingRequestException.class,
                () -> pricing.quote("PRO", BillingPeriod.MONTHLY, "FR", null));
    }
}
