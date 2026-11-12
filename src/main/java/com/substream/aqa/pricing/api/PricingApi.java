/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.substream.aqa.pricing.BillingPeriod;
import com.substream.aqa.pricing.Coupon;
import com.substream.aqa.pricing.CouponNotFoundException;
import com.substream.aqa.pricing.CouponType;
import com.substream.aqa.pricing.InvalidPricingRequestException;
import com.substream.aqa.pricing.PricingConflictException;
import com.substream.aqa.pricing.PricingService;
import com.substream.aqa.pricing.Quote;
import com.substream.aqa.pricing.UpgradeCharge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The SubStream Pricing REST API. A small HTTP server around {@link PricingService},
 * with in-memory storage: every started server begins with no coupons and no quotes.
 *
 * <pre>
 * PricingApi api = PricingApi.start();   // free port, date 2024-01-15
 * RestAssured.port = api.getPort();
 * ...
 * api.stop();
 * </pre>
 *
 * The endpoints are documented in {@code docs/aqa/PRICING_SPEC.md}, section 3.
 */
public final class PricingApi {

    /** The date every new server starts on. */
    public static final LocalDate START_DATE = LocalDate.of(2024, 1, 15);

    private static final String JSON = "application/json; charset=utf-8";

    private final HttpServer server;
    private final MutableClock clock = new MutableClock(START_DATE);
    private final PricingService pricing = new PricingService(clock);

    private PricingApi(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", this::handle);
    }

    /** Starts a server on a free port. */
    public static PricingApi start() {
        return start(0);
    }

    /** Starts a server on the given port ({@code 0} = any free port). */
    public static PricingApi start(int port) {
        try {
            PricingApi api = new PricingApi(port);
            api.server.start();
            return api;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start the Pricing API on port " + port, e);
        }
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    public void stop() {
        server.stop(0);
    }

    /** Runs the API on port 8080 so you can explore it with Postman or a browser. */
    public static void main(String[] args) {
        PricingApi api = start(8080);
        System.out.println("Pricing API running at " + api.getBaseUrl() + " (Ctrl+C to stop)");
    }

    // ------------------------------------------------------------------ routing

    private void handle(HttpExchange exchange) throws IOException {
        Response response;
        try {
            response = route(exchange.getRequestMethod(), segments(exchange), exchange);
        } catch (IllegalArgumentException | InvalidPricingRequestException e) {
            response = error(400, e.getMessage());
        } catch (CouponNotFoundException | NotFound e) {
            response = error(404, e.getMessage());
        } catch (PricingConflictException e) {
            response = error(409, e.getMessage());
        } catch (MethodNotAllowed e) {
            response = error(405, "Method " + exchange.getRequestMethod() + " is not allowed here");
        } catch (RuntimeException e) {
            response = error(500, "Internal server error");
        }
        send(exchange, response);
    }

    private Response route(String method, List<String> path, HttpExchange exchange) throws IOException {
        int size = path.size();
        String root = size > 0 ? path.get(0) : "";
        switch (root) {
            case "coupons" -> {
                if (size == 1) {
                    require(method, "POST");
                    return createCoupon(body(exchange));
                }
                if (size == 2) {
                    require(method, "GET");
                    return getCoupon(path.get(1));
                }
            }
            case "quotes" -> {
                if (size == 1) {
                    require(method, "POST");
                    return createQuote(body(exchange));
                }
                if (size == 2) {
                    require(method, "GET");
                    Quote quote = pricing.findQuote(path.get(1))
                            .orElseThrow(() -> new NotFound("Unknown quote: " + path.get(1)));
                    return ok(quoteJson(quote));
                }
            }
            case "upgrades" -> {
                if (size == 1) {
                    require(method, "POST");
                    return createUpgrade(body(exchange));
                }
            }
            case "test" -> {
                if (size == 2 && path.get(1).equals("clock")) {
                    if (method.equals("PUT")) {
                        clock.setDate(date(requireString(body(exchange), "date")));
                    } else {
                        require(method, "GET");
                    }
                    return ok(Map.of("date", clock.getDate().toString()));
                }
            }
            default -> {
                // falls through to 404
            }
        }
        throw new NotFound("No endpoint " + method + " /" + String.join("/", path));
    }

    // ------------------------------------------------------------------ handlers

    private Response createCoupon(Map<String, Object> body) {
        Coupon coupon = pricing.addCoupon(
                requireString(body, "code"),
                couponType(requireString(body, "type")),
                requireDecimal(body, "value"),
                date(requireString(body, "validUntil")),
                optionalDecimal(body, "minimumSubtotal"));
        return created("/coupons/" + coupon.code(), couponJson(coupon));
    }

    private Response getCoupon(String code) {
        Coupon coupon = pricing.findCoupon(code).get();
        return ok(couponJson(coupon));
    }

    private Response createQuote(Map<String, Object> body) {
        Quote quote = pricing.quote(
                requireString(body, "plan"),
                period(requireString(body, "period")),
                requireString(body, "country"),
                optionalString(body, "coupon"));
        return ok(quoteJson(quote));
    }

    private Response createUpgrade(Map<String, Object> body) {
        UpgradeCharge charge = pricing.upgrade(
                requireString(body, "fromPlan"),
                requireString(body, "toPlan"),
                date(requireString(body, "cycleStart")),
                requireString(body, "country"));
        return ok(upgradeJson(charge));
    }

    // ------------------------------------------------------------------ JSON views

    private static Map<String, Object> couponJson(Coupon coupon) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("code", coupon.code());
        json.put("type", coupon.type().name());
        json.put("value", coupon.value().toPlainString());
        json.put("validUntil", coupon.validUntil().toString());
        json.put("minimumSubtotal", coupon.minimumSubtotal().toPlainString());
        return json;
    }

    private static Map<String, Object> quoteJson(Quote quote) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", quote.id());
        json.put("plan", quote.plan());
        json.put("period", quote.period().name());
        json.put("country", quote.country());
        json.put("coupon", quote.couponCode());
        json.put("subtotal", quote.subtotal().toPlainString());
        json.put("discount", quote.discount().toPlainString());
        json.put("vat", quote.vat().toPlainString());
        json.put("total", quote.total().toPlainString());
        json.put("quotedOn", quote.quotedOn().toString());
        return json;
    }

    private static Map<String, Object> upgradeJson(UpgradeCharge charge) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("fromPlan", charge.fromPlan());
        json.put("toPlan", charge.toPlan());
        json.put("cycleStart", charge.cycleStart().toString());
        json.put("cycleEnd", charge.cycleEnd().toString());
        json.put("cycleDays", charge.cycleDays());
        json.put("remainingDays", charge.remainingDays());
        json.put("amount", charge.amount().toPlainString());
        json.put("vat", charge.vat().toPlainString());
        json.put("total", charge.total().toPlainString());
        return json;
    }

    // ------------------------------------------------------------------ request helpers

    private static Map<String, Object> body(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return Json.readObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String optionalString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Field '" + field + "' must be a string");
        }
        return (String) value;
    }

    private static String requireString(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return value;
    }

    /** Money and percentages may be sent as a number (10.5) or a string ("10.50"). */
    private static BigDecimal optionalDecimal(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field '" + field + "' must be a number");
        }
    }

    private static BigDecimal requireDecimal(Map<String, Object> body, String field) {
        BigDecimal value = optionalDecimal(body, field);
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return value;
    }

    private static CouponType couponType(String name) {
        try {
            return CouponType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown coupon type: " + name);
        }
    }

    private static BillingPeriod period(String name) {
        try {
            return BillingPeriod.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown billing period: " + name);
        }
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must look like 2024-01-31: " + text);
        }
    }

    private static List<String> segments(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return java.util.Arrays.stream(path.split("/")).filter(part -> !part.isEmpty()).toList();
    }

    private static void require(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new MethodNotAllowed();
        }
    }

    // ------------------------------------------------------------------ responses

    private record Response(int status, String location, Object body) {
    }

    private static Response ok(Object body) {
        return new Response(200, null, body);
    }

    private static Response created(String location, Object body) {
        return new Response(201, location, body);
    }

    private static Response error(int status, String message) {
        return new Response(status, null, Map.of("error", message == null ? "Error" : message));
    }

    private static void send(HttpExchange exchange, Response response) throws IOException {
        byte[] bytes = Json.write(response.body()).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        if (response.location() != null) {
            exchange.getResponseHeaders().set("Location", response.location());
        }
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final class NotFound extends RuntimeException {
        NotFound(String message) {
            super(message);
        }
    }

    private static final class MethodNotAllowed extends RuntimeException {
    }
}
