/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SubStream Pricing: price quotes, discount coupons and mid-cycle upgrades.
 * The rules PR-1 to PR-6 are in {@code docs/aqa/PRICING_SPEC.md}.
 *
 * <pre>
 * Clock clock = Clock.fixed(Instant.parse("2024-01-15T00:00:00Z"), ZoneOffset.UTC);
 * PricingService pricing = new PricingService(clock);
 * Quote quote = pricing.quote("PRO", BillingPeriod.MONTHLY, "EG", null);
 * </pre>
 *
 * Storage is in memory: every new service starts with no coupons and no quotes.
 */
public class PricingService {

    /** PR-1: monthly price of each plan. */
    private static final Map<String, BigDecimal> MONTHLY_PRICES = Map.of(
            "FREE", new BigDecimal("0.00"),
            "PRO", new BigDecimal("29.00"),
            "ENTERPRISE", new BigDecimal("199.00"));

    /** PR-1: VAT rate of each supported country. */
    private static final Map<String, BigDecimal> VAT_RATES = Map.of(
            "EG", new BigDecimal("0.14"),
            "DE", new BigDecimal("0.19"),
            "HU", new BigDecimal("0.27"),
            "US", new BigDecimal("0.00"));

    /** PR-2: months charged for an annual subscription. */
    private static final int ANNUAL_MONTHS = 11;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final Clock clock;
    private final Map<String, Coupon> coupons = new LinkedHashMap<>();
    private final Map<String, Quote> quotes = new LinkedHashMap<>();

    public PricingService(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.clock = clock;
    }

    // ------------------------------------------------------------------ coupons (PR-3)

    /**
     * Creates a coupon. The code is stored trimmed and in upper case.
     * {@code minimumSubtotal} may be {@code null}: no minimum.
     */
    public Coupon addCoupon(String code, CouponType type, BigDecimal value, LocalDate validUntil,
                            BigDecimal minimumSubtotal) {
        if (code == null || code.isBlank()) {
            throw new InvalidPricingRequestException("Coupon code is required");
        }
        if (type == null) {
            throw new InvalidPricingRequestException("Coupon type is required");
        }
        if (value == null || value.signum() <= 0) {
            throw new InvalidPricingRequestException("Coupon value must be greater than 0");
        }
        if (type == CouponType.PERCENT && value.compareTo(HUNDRED) > 0) {
            throw new InvalidPricingRequestException("A percent coupon cannot exceed 100");
        }
        if (validUntil == null) {
            throw new InvalidPricingRequestException("validUntil is required");
        }
        if (minimumSubtotal != null && minimumSubtotal.signum() < 0) {
            throw new InvalidPricingRequestException("minimumSubtotal cannot be negative");
        }
        String normalized = normalizeCode(code);
        if (coupons.containsKey(normalized)) {
            throw new PricingConflictException("Coupon already exists: " + normalized);
        }
        Coupon coupon = new Coupon(normalized, type, money(value), validUntil,
                money(minimumSubtotal == null ? BigDecimal.ZERO : minimumSubtotal));
        coupons.put(normalized, coupon);
        return coupon;
    }

    /** Finds a coupon by code, ignoring case and surrounding spaces. */
    public Optional<Coupon> findCoupon(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(coupons.get(normalizeCode(code)));
    }

    // ------------------------------------------------------------------ quotes (PR-1 to PR-5)

    /**
     * Prices a plan for one billing period in one country, with an optional coupon
     * ({@code couponCode} may be {@code null}). The quote is stored and can be read again.
     */
    public Quote quote(String plan, BillingPeriod period, String country, String couponCode) {
        String planName = normalizePlan(plan);
        String countryCode = normalizeCountry(country);
        if (period == null) {
            throw new InvalidPricingRequestException("Billing period is required");
        }
        LocalDate today = LocalDate.now(clock);

        BigDecimal subtotal = subtotal(planName, period);

        Coupon coupon = null;
        BigDecimal discount = money(BigDecimal.ZERO);
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = usableCoupon(couponCode, subtotal, today);
            discount = discount(coupon, subtotal);
        }

        BigDecimal net = subtotal.subtract(discount);
        BigDecimal vat = vat(subtotal, countryCode);
        BigDecimal total = net.add(vat);

        Quote quote = new Quote(UUID.randomUUID().toString(), planName, period, countryCode,
                coupon == null ? null : coupon.code(), subtotal, discount, vat, total, today);
        quotes.put(quote.id(), quote);
        return quote;
    }

    public Optional<Quote> findQuote(String id) {
        return Optional.ofNullable(id == null ? null : quotes.get(id));
    }

    /** PR-2 */
    private BigDecimal subtotal(String plan, BillingPeriod period) {
        BigDecimal monthly = MONTHLY_PRICES.get(plan);
        int months = period == BillingPeriod.ANNUAL ? ANNUAL_MONTHS : 1;
        return money(monthly.multiply(BigDecimal.valueOf(months)));
    }

    /** PR-3 */
    private Coupon usableCoupon(String code, BigDecimal subtotal, LocalDate today) {
        Coupon coupon = findCoupon(code)
                .orElseThrow(() -> new CouponNotFoundException("Unknown coupon: " + code.trim()));
        if (!today.isBefore(coupon.validUntil())) {
            throw new PricingConflictException("Coupon " + coupon.code() + " expired on " + coupon.validUntil());
        }
        if (subtotal.compareTo(coupon.minimumSubtotal()) <= 0) {
            throw new PricingConflictException("Coupon " + coupon.code() + " needs a subtotal of at least "
                    + coupon.minimumSubtotal());
        }
        return coupon;
    }

    /** PR-4 */
    private BigDecimal discount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.type() == CouponType.PERCENT) {
            return subtotal.multiply(coupon.value()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return money(coupon.value());
    }

    /** PR-5 */
    private BigDecimal vat(BigDecimal net, String country) {
        return net.multiply(VAT_RATES.get(country)).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ upgrades (PR-6)

    /**
     * Charges the difference between two monthly plans for the rest of the current cycle.
     * The cycle starts on {@code cycleStart} and ends one calendar month later.
     */
    public UpgradeCharge upgrade(String fromPlan, String toPlan, LocalDate cycleStart, String country) {
        String from = normalizePlan(fromPlan);
        String to = normalizePlan(toPlan);
        String countryCode = normalizeCountry(country);
        if (cycleStart == null) {
            throw new InvalidPricingRequestException("cycleStart is required");
        }
        BigDecimal difference = MONTHLY_PRICES.get(to).subtract(MONTHLY_PRICES.get(from));
        if (difference.signum() <= 0) {
            throw new PricingConflictException("Not an upgrade: " + from + " -> " + to);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate cycleEnd = cycleStart.plusMonths(1);
        if (today.isBefore(cycleStart) || !today.isBefore(cycleEnd)) {
            throw new InvalidPricingRequestException("Today (" + today + ") is outside the cycle "
                    + cycleStart + " - " + cycleEnd);
        }
        int cycleDays = (int) ChronoUnit.DAYS.between(cycleStart, cycleEnd);
        int remainingDays = (int) ChronoUnit.DAYS.between(today.plusDays(1), cycleEnd);

        BigDecimal amount = difference.multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(cycleDays), 2, RoundingMode.HALF_UP);
        BigDecimal vat = vat(amount, countryCode);
        return new UpgradeCharge(from, to, cycleStart, cycleEnd, cycleDays, remainingDays,
                amount, vat, amount.add(vat));
    }

    // ------------------------------------------------------------------ helpers

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePlan(String plan) {
        String name = plan == null ? "" : plan.trim().toUpperCase(Locale.ROOT);
        if (!MONTHLY_PRICES.containsKey(name)) {
            throw new InvalidPricingRequestException("Unknown plan: " + plan);
        }
        return name;
    }

    private static String normalizeCountry(String country) {
        String code = country == null ? "" : country.trim().toUpperCase(Locale.ROOT);
        if (!VAT_RATES.containsKey(code)) {
            throw new InvalidPricingRequestException("Unsupported country: " + country);
        }
        return code;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
