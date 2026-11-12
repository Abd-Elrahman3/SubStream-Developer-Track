# RestAssured in One Page

RestAssured sends HTTP requests from a JUnit test and checks the responses. The working pattern is in `src/test/java/com/substream/aqa/pricing/api/CouponApiExampleTest.java`; open it next to this page.

## The shape of every test

```java
given()                                    // the request: headers, body
        .contentType(ContentType.JSON)
        .body("""
              {"plan": "PRO", "period": "MONTHLY", "country": "EG"}
              """)
.when()                                    // the call
        .post("/quotes")
.then()                                    // the checks
        .statusCode(201)
        .body("subtotal", equalTo("29.00"))
        .body("total", equalTo("33.06"));
```

## The things you need

| You want to… | Write |
| :--- | :--- |
| Send a body | `.contentType(ContentType.JSON).body("""{...}""")` — a Java text block |
| Put a value in the path | `.get("/quotes/{id}", id)` |
| Check the status | `.statusCode(404)` |
| Check a field | `.body("plan", equalTo("PRO"))` |
| Check a number field | `.body("remainingDays", equalTo(15))` |
| Check a field is `null` | `.body("coupon", nullValue())` |
| Check a header | `.header("Location", startsWith("/quotes/"))` |
| Keep a value for the next request | `String id = ... .then().extract().path("id");` |
| See what happened | `.log().ifValidationFails()` after `then()` |

Matchers come from `org.hamcrest.Matchers`: `equalTo`, `notNullValue`, `nullValue`, `containsString`, `startsWith`.

## Good API tests

- Start a **new server per test** (`@BeforeEach`), so tests never depend on each other.
- Create the data each test needs through the API itself.
- Check the **status code and the body**: a correct body with the wrong status is still a defect.
- Cover the error paths of `PRICING_SPEC.md`, section 3.3, not only the happy path.

## Scenario tests (A7)

A scenario is several requests in a row, each one checked. Small private helpers keep it readable:

```java
private void createCoupon(String code, String validUntil) {
    given().contentType(ContentType.JSON)
            .body("""
                  {"code": "%s", "type": "FIXED", "value": "5", "validUntil": "%s"}
                  """.formatted(code, validUntil))
            .when().post("/coupons")
            .then().statusCode(201);
}

private void moveClockTo(String date) {
    given().contentType(ContentType.JSON)
            .body("""
                  {"date": "%s"}
                  """.formatted(date))
            .when().put("/test/clock")
            .then().statusCode(200);
}
```

A test then reads like the business story: `createCoupon("SPRING", "2024-03-31")`, `moveClockTo("2024-04-01")`, request a quote with the coupon, then check the status and the error.

Official guide: https://github.com/rest-assured/rest-assured/wiki/Usage
