/*
 * AQA TRACK ONLY. PROVIDED EXAMPLE (A6) — DO NOT MODIFY.
 */
package com.substream.aqa.pricing.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * API tests with RestAssured. Each test starts its own Pricing server, so every
 * test begins with no data and on the date 2024-01-15. Use this class as the pattern
 * for your own API tests; the techniques are explained in docs/aqa/RESTASSURED.md.
 */
@DisplayName("Coupon API (example)")
class CouponApiExampleTest {

    private PricingApi api;

    @BeforeEach
    void startServer() {
        api = PricingApi.start();
        RestAssured.port = api.getPort();
    }

    @AfterEach
    void stopServer() {
        api.stop();
    }

    @Test
    @DisplayName("a created coupon can be read back, whatever the case of its code")
    void createdCouponCanBeReadBack() {
        // When: create a coupon
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"code": " welcome10 ", "type": "PERCENT", "value": "10", "validUntil": "2024-12-31"}
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(201)
                .header("Location", equalTo("/coupons/WELCOME10"))
                .body("code", equalTo("WELCOME10"));

        // Then: reading it back returns the stored values (PR-3: code in upper case, minimum 0.00 when not sent)
        given()
        .when()
                .get("/coupons/{code}", "Welcome10")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", equalTo("PERCENT"))
                .body("value", equalTo("10.00"))
                .body("validUntil", equalTo("2024-12-31"))
                .body("minimumSubtotal", equalTo("0.00"));
    }

    @Test
    @DisplayName("a percent coupon above 100 is rejected with 400 and an error message")
    void percentAbove100IsRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"code": "TOO-MUCH", "type": "PERCENT", "value": "101", "validUntil": "2024-12-31"}
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(400)
                .body("error", containsString("100"));
    }
}
