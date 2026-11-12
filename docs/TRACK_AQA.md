# AQA Engineer Track (A4 – A7)

You do not build the rest of the system: you **test** software, the way an automation QA engineer tests a release before it reaches customers. A4 tests the classes you wrote in M1 and M3. From A5 on you test **SubStream Pricing**, a finished part of the system, at two levels: **unit tests** on its service, then **API tests** on the running server.

Start after M3. Opening `feature/a4-unit-tests` is your track choice.

| Milestone | Branch | Points | Level reached | New tool |
| :--- | :--- | :--- | :--- | :--- |
| A4 Unit tests for your own code | `feature/a4-unit-tests` | 15 | 🥈 Silver | JUnit 5 |
| A5 Unit tests of SubStream Pricing | `feature/a5-service-tests` | 15 | | — |
| A6 API tests: one request at a time | `feature/a6-api-tests` | 15 | 🥇 Gold | RestAssured |
| A7 API scenarios over time | `feature/a7-billing-api-tests` | 15 | 💎 Platinum | — |

**You are not expected to know these tools already.** Each one comes with a complete example test and a one-page primer. Learning a tool from an example is part of the challenge.

Each milestone below has a **checklist**, in the order to work on it.

---

## Your files

From A5 on, you test **SubStream Pricing**: a small, finished pricing system that is already in your project. It is separate from the classes you wrote in M1 – M3, so your tests never depend on your own code being right.

| Use | Ignore |
| :--- | :--- |
| `src/test/java/com/substream/domain/` — your A4 tests | `src/main/java/com/substream/service/` — Developer track only |
| `src/main/java/com/substream/aqa/` — the system you test (do not modify it) | |
| `src/test/java/com/substream/aqa/` — the two example tests, and your A5 – A7 tests | `docs/TRACK_DEVELOPER.md` |
| `docs/aqa/` — the specification and the RestAssured primer | |

Every file of the AQA track says `AQA TRACK ONLY` at the top of the file or in its path.

## The system under test

SubStream Pricing is **finished, and it may contain defects.** Finding them is your job. Its rules are `PR-1` to `PR-6` in `docs/aqa/PRICING_SPEC.md`, and its API is in section 3 of the same file. Do not modify its files: the pipeline rejects it.

## How your tests are scored

On every Merge Request, a NIX bot runs your tests against a **correct build** of SubStream Pricing and lists any of your tests that fail there. A test that fails on correct code is a *false alarm*: re-read the rule and fix the test.

After the deadline, your tests also run against **builds with defects**. A defect counts as found when at least one of your tests fails on it. These results are not shown before the deadline.

So a strong test checks exact expected values (amounts, dates, status codes, exception types), covers boundaries, and checks what must **not** happen. A defect you find earns a little more when its test carries a correct defect note (next section).

## When a test fails on SubStream Pricing

First decide: is my test wrong, or is the code wrong? Re-read the rule.

- **Your test is wrong:** fix it.
- **The code is wrong:** you found a defect. Keep the test exactly as the specification requires, tag it, and write a **defect note** on the same line: the rule, what happens, and what the specification expects.

```java
@Test
@Tag("known-bug")   // PR-1: a HU quote has 25% VAT, expected 27%
void hungarianVatIs27Percent() { ... }
```

The rule is `PR-1` to `PR-6`, or `API` for anything in PRICING_SPEC section 3. This one line is your bug report: no other document is needed.

The tag tells a defect you found apart from a mistake in your test. Your tests never block a merge; the final scoring runs all of them. Never weaken a test to make it pass on wrong code.

---

## A4 — Unit tests for your own code

**Branch:** `feature/a4-unit-tests`

**Goal:** prove that the classes you wrote in M1 and M3 follow the rules. This is the easiest step: you know the code, you only learn the tool.

**You will practise:** JUnit 5 · parameterized tests · boundary values

- Study `SubscriptionPlanTest` (provided) and `docs/primers/JUNIT5.md`.
- Write two test classes under `src/test/java/com/substream/domain/`: `CustomerTest` and `SubscriptionTest`. These names are a suggestion: every test you merge counts, whatever its class is called.

| ✓ | Rule | What to test | Test class |
| :--- | :--- | :--- | :--- |
| ☐ | BR-101 | Blank names, 100 and 101 characters after trimming, trimming itself | `CustomerTest` |
| ☐ | BR-102 | Each of the three email conditions, `null`, trimming and lower case. Use `@ParameterizedTest` | `CustomerTest` |
| ☐ | BR-201 | Every row of the renewal table, including 31 January | `SubscriptionTest` |
| ☐ | BR-104 | Every row of the plan-change table, including 10 and 11 seats | `SubscriptionTest` |
| ☐ | BR-401 | Every cell of the Module D table | `SubscriptionTest` |

**Done when:** every row above has at least one test, and every test passes on your own code.

As in A5 – A7, the NIX bot runs these tests on NIX's correct `Customer`, `EmailValidator` and `Subscription` and lists any that fail there. After the deadline they also run on copies of those classes with one mistake each: a mistake counts as found when one of your tests fails on it.

## A5 — Unit tests of SubStream Pricing

**Branch:** `feature/a5-service-tests`

**Goal:** test the pricing rules one method call at a time, after deciding *what* to test.

**You will practise:** decision tables · boundary values · fixed clocks in tests · comparing money

- Study `PricingServiceExampleTest` (provided, in `src/test/java/com/substream/aqa/pricing/`) and `docs/aqa/PRICING_SPEC.md`, sections 1 and 2.
- Before writing tests, list your cases, on paper or as comments: for using a coupon (PR-3), every combination of the conditions in a small **decision table**; for every limit, the value just below it, on it and just above it (**boundary values**). There is nothing to hand in: your tests are what counts.
- Write your tests in the same package, for example `QuoteTest` and `CouponTest`, with a fixed clock:
  `Clock.fixed(Instant.parse("2024-01-15T00:00:00Z"), ZoneOffset.UTC)`.

| ✓ | Rule | What to test |
| :--- | :--- | :--- |
| ☐ | PR-1 | Each plan and each country; case and spaces; an unknown plan and country |
| ☐ | PR-2 | `MONTHLY` and `ANNUAL` for each paid plan |
| ☐ | PR-3 | Creating coupons: every input rule, duplicates in another case. Using them: every row of your decision table and every limit |
| ☐ | PR-4 | `PERCENT` with rounding; `FIXED` below, equal to and above the subtotal |
| ☐ | PR-5 | Both examples of the PR-5 table; a quote can be found again by its id |

**Done when:** every case you listed has a test, and every defect your tests expose is tagged with a note.

## A6 — API tests: one request at a time

**Branch:** `feature/a6-api-tests`

**Goal:** test the system from the outside, through its REST API.

**You will practise:** HTTP and REST · status codes · RestAssured · JSON paths and matchers

- Study `CouponApiExampleTest` (provided, in `src/test/java/com/substream/aqa/pricing/api/`), `docs/aqa/RESTASSURED.md` and `docs/aqa/PRICING_SPEC.md`, section 3.
- Every test starts its own server, so tests never depend on each other.
- Check **the status code and the body** of every response.

| ✓ | Endpoint (PRICING_SPEC 3.4) | What to test |
| :--- | :--- | :--- |
| ☐ | `POST /coupons`, `GET /coupons/{code}` | Created coupon and `Location`; `400` for each invalid input; `409` for a duplicate; `404` for an unknown code |
| ☐ | `POST /quotes`, `GET /quotes/{id}` | Created quote, `Location`, every amount, `quotedOn` = the server's date; `400`, `404` and `409` cases; `404` for an unknown id |
| ☐ | `POST /upgrades` | The PR-6 example after moving the clock; `409` for the same plan or a cheaper one |
| ☐ | Any endpoint | `405` for a wrong method; the `{"error": ...}` body on every error |

**Done when:** every endpoint above has a happy-path test and at least one error test, and every status code of PRICING_SPEC 3.3 is checked somewhere.

## A7 — API scenarios over time

**Branch:** `feature/a7-billing-api-tests`

**Goal:** test whole business stories across several requests, moving the server's date.

**You will practise:** end-to-end scenarios · test data · tagging tests · defect notes

- Use `PUT /test/clock` (PRICING_SPEC 3.5) and the helper methods of `docs/aqa/RESTASSURED.md`.

| ✓ | Rule | Scenario |
| :--- | :--- | :--- |
| ☐ | PR-3 | A coupon's life: before, on and after its `validUntil` date |
| ☐ | PR-3, PR-4 | One coupon used in several quotes for different plans, periods and countries; read each quote back by its id |
| ☐ | PR-6 | An upgrade on the first day of a cycle, in the middle, and on the last day; one day before the cycle and on `cycleEnd` |
| ☐ | PR-6 | Cycles of different lengths: February in a leap year, a 30-day month, a 31-day month |

**Done when:** every rule above has a scenario, and every defect you found is tagged with a note.
