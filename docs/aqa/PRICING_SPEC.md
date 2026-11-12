# SubStream Pricing — Specification (AQA track)

SubStream Pricing tells a customer what a plan costs before they buy it: the price for a month or a year, a discount coupon, VAT for their country, and the extra charge for an upgrade in the middle of a month.

It is the system you test on the AQA track. It is **finished and provided** in `src/main/java/com/substream/aqa/pricing/`, and it **may contain defects**. You never change its code: you test it against this specification.

| Milestone | What you test | How |
| :--- | :--- | :--- |
| A5 | PR-1 to PR-5 | Unit tests on `PricingService` |
| A6 | Section 3, the API | One request per test, with RestAssured |
| A7 | PR-3 and PR-6 over time | Scenarios: several requests, moving the server's date |

---

## 1. The Classes

| Class | What it is |
| :--- | :--- |
| `PricingService` | Everything below: coupons, quotes, upgrades. Takes a `Clock` in its constructor |
| `Quote`, `Coupon`, `UpgradeCharge` | Results (Java records: read them with `quote.total()`, `coupon.code()`…) |
| `BillingPeriod` | `MONTHLY`, `ANNUAL` |
| `CouponType` | `PERCENT`, `FIXED` |
| `InvalidPricingRequestException` | Invalid input. API: `400` |
| `CouponNotFoundException` | Unknown coupon code. API: `404` |
| `PricingConflictException` | Valid input that cannot be applied. API: `409` |
| `api/PricingApi` | The REST API around the service (section 3) |

"Today" is always the date of the service's `Clock`.

---

## 2. Business Rules

### PR-1 — Plans and countries

| Plan | Monthly price |
| :--- | :--- |
| `FREE` | 0.00 |
| `PRO` | 29.00 |
| `ENTERPRISE` | 199.00 |

| Country | VAT rate |
| :--- | :--- |
| `EG` Egypt | 14% |
| `DE` Germany | 19% |
| `HU` Hungary | 27% |
| `US` United States | 0% |

- Plan names and country codes are accepted in any case and with surrounding spaces (`" pro "` = `"PRO"`), and are returned in upper case.
- Any other plan or country is invalid: `InvalidPricingRequestException`.

### PR-2 — Billing period

- `MONTHLY`: the subtotal is the monthly price.
- `ANNUAL`: the subtotal is **10 ×** the monthly price (two months free).
- No period: `InvalidPricingRequestException`.

### PR-3 — Coupons

**Creating a coupon** (`addCoupon`):

- The code is required (not `null`, not blank). It is stored trimmed and in upper case, and it is unique: a second coupon with the same code, in any case, is a `PricingConflictException`.
- The type (`PERCENT` or `FIXED`) and the value are required.
- `PERCENT`: the value is greater than 0 and at most 100.
- `FIXED`: the value is an amount greater than 0.
- `validUntil` is required.
- `minimumSubtotal` is optional. When it is not given it is `0.00`. It cannot be negative.
- A broken rule above (except the duplicate code) is an `InvalidPricingRequestException`.
- Amounts and values are stored with two decimals, rounded half-up: `10` → `10.00`.

**Using a coupon in a quote:**

- The code is matched ignoring case and surrounding spaces.
- An unknown code is a `CouponNotFoundException`.
- The coupon can be used **up to and including** its `validUntil` date. After that date: `PricingConflictException`.
- The quote's subtotal must be **at least** the coupon's `minimumSubtotal`. Below it: `PricingConflictException`.
- No coupon (`null` or blank) means no discount.

### PR-4 — Discount

- `PERCENT`: subtotal × value ÷ 100, rounded half-up to two decimals.
- `FIXED`: the coupon's value, but **never more than the subtotal**. A total is never negative.

Example: a 15% coupon on `PRO` monthly gives a discount of `4.35`.

### PR-5 — VAT and total

- VAT = (subtotal − discount) × the country's rate, rounded half-up to two decimals.
- Total = subtotal − discount + VAT.
- Every amount has exactly two decimals.
- A quote is stored with a new id and `quotedOn` = today, and can be read again by its id.

| Plan, period, country | Coupon | Subtotal | Discount | VAT | Total |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `PRO`, `MONTHLY`, `EG` | none | 29.00 | 0.00 | 4.06 | 33.06 |
| `ENTERPRISE`, `MONTHLY`, `DE` | 10% | 199.00 | 19.90 | 34.03 | 213.13 |

### PR-6 — Upgrade in the middle of a cycle

A monthly customer moves to a more expensive plan before the month is over. They pay the difference for the days that are left (`upgrade(fromPlan, toPlan, cycleStart, country)`).

- Both plans and the country follow PR-1. `cycleStart` is required: without it, `InvalidPricingRequestException`.
- The target plan must cost more than the current one. The same plan or a cheaper one: `PricingConflictException`.
- The cycle starts on `cycleStart` and ends one calendar month later (`cycleEnd`, not included). `cycleDays` = the number of days from `cycleStart` to `cycleEnd`.
- Today must be inside the cycle: on or after `cycleStart`, and before `cycleEnd`. Otherwise: `InvalidPricingRequestException`.
- `remainingDays` = the days from today to `cycleEnd`, **today included**.
- Amount = (new monthly price − old monthly price) × `remainingDays` ÷ `cycleDays`, rounded half-up to two decimals.
- VAT and total as in PR-5, on the amount.

Example: cycle starting 2024-03-01 (`cycleEnd` 2024-04-01, 31 days), today 2024-03-17, `PRO` → `ENTERPRISE` in `US`: 15 remaining days, amount = 170.00 × 15 ÷ 31 = `82.26`, VAT `0.00`, total `82.26`.

---

## 3. The API

### 3.1 Running it

**In a test** (the normal way): every test starts its own server, with no data, on a free port.

```java
PricingApi api = PricingApi.start();   // in @BeforeEach
RestAssured.port = api.getPort();
// ... requests ...
api.stop();                             // in @AfterEach
```

**By hand**, to explore it with Postman or a browser: run `PricingApi.main()` from IntelliJ. The API listens on `http://localhost:8080`.

### 3.2 General rules

- Requests and responses are JSON (`Content-Type: application/json`). Request bodies are flat objects.
- Dates are strings in ISO format: `"2024-01-15"`.
- Money in responses is a **string** with two decimals: `"29.00"`. In requests, money and percentages may be a string (`"10"`) or a number (`10`).
- Every new server starts on the date **2024-01-15**. Move it with `PUT /test/clock` (section 3.5).

### 3.3 Status codes

| Code | When |
| :--- | :--- |
| `200 OK` | Successful read, or an upgrade charge calculated |
| `201 Created` | A coupon or a quote was created; the `Location` header holds its URL |
| `400 Bad Request` | Malformed JSON, a missing or wrongly typed field, an unknown plan, country, period or coupon type, or a broken input rule (PR-1, PR-2, PR-3 creation, PR-6 dates) |
| `404 Not Found` | Unknown coupon or quote, or unknown endpoint |
| `405 Method Not Allowed` | Known endpoint, wrong HTTP method |
| `409 Conflict` | Valid input that cannot be applied: duplicate coupon code, expired coupon, subtotal below the minimum, not an upgrade |
| `500 Internal Server Error` | Never expected: always a defect |

Every error response has the body `{"error": "<message>"}`.

### 3.4 Endpoints

| Method and path | Body | Success | Errors |
| :--- | :--- | :--- | :--- |
| `POST /coupons` | `{"code", "type", "value", "validUntil", "minimumSubtotal"?}` | `201` + coupon, `Location: /coupons/{CODE}` | `400`, `409` |
| `GET /coupons/{code}` | — | `200` + coupon (code in any case) | `404` |
| `POST /quotes` | `{"plan", "period", "country", "coupon"?}` | `201` + quote, `Location: /quotes/{id}` | `400`, `404` (unknown coupon), `409` |
| `GET /quotes/{id}` | — | `200` + quote | `404` |
| `POST /upgrades` | `{"fromPlan", "toPlan", "cycleStart", "country"}` | `200` + upgrade charge | `400`, `409` |

`?` = optional field.

- Coupon: `{"code", "type", "value", "validUntil", "minimumSubtotal"}`.
- Quote: `{"id", "plan", "period", "country", "coupon", "subtotal", "discount", "vat", "total", "quotedOn"}`. `coupon` is the code used, or `null`.
- Upgrade charge: `{"fromPlan", "toPlan", "cycleStart", "cycleEnd", "cycleDays", "remainingDays", "amount", "vat", "total"}`. `cycleDays` and `remainingDays` are numbers.

### 3.5 Moving time

| Method and path | Body | Result |
| :--- | :--- | :--- |
| `PUT /test/clock` | `{"date": "2024-01-31"}` | `200` + `{"date"}`; from now on "today" is that date |
| `GET /test/clock` | — | `200` + `{"date"}` |

Example: create a coupon valid until 2024-01-31, move the clock to 2024-02-01, and a quote with that coupon is refused with `409`.
