# Developer Track (D4 – D7)

You complete the system yourself. Each milestone is checked by automated tests written by NIX against the business rules in the README, plus a review of your code quality.

Start after M3. Opening `feature/d4-service` is your track choice.

**Your files** are in `src/main/java/com/substream/service/`. Ignore everything marked `AQA TRACK ONLY`: the package `com.substream.aqa`, its tests and `docs/aqa/` belong to the other track.

| Milestone | Branch | Points | Level reached |
| :--- | :--- | :--- | :--- |
| D4 Subscription service | `feature/d4-service` | 15 | 🥈 Silver |
| D5 Billing engine: the happy path | `feature/d5-billing` | 15 | |
| D6 Billing engine: failures | `feature/d6-failures` | 15 | 🥇 Gold |
| D7 Reporting and notifications | `feature/d7-reporting` | 15 | 💎 Platinum |

Each milestone below has a **checklist of its rules**, in the order to work on them. The full text of every rule is in README section 5.

## Your tests

Every milestone asks for **one or two simple JUnit tests** of your own, named in its checklist. They are short on purpose: one setup, one call, one or two assertions. `SubscriptionPlanTest` shows the pattern and `docs/primers/JUNIT5.md` explains it, including how to replace the payment gateway in a test with one line of code.

A service test needs a clock that always gives the same day, and a payment gateway and a notification service that the test controls: `docs/primers/JUNIT5.md` shows how to write each of them.

In IntelliJ: right-click `src/test/java` → **New → Java Class**, and type the full name, for example `com.substream.service.SubscriptionServiceTest`: IntelliJ creates the package for you.

Put them under `src/test/java/com/substream/service/`, in the class its checklist names:

| Milestone | Test class |
| :--- | :--- |
| D4 | `SubscriptionServiceTest` |
| D5, D6 | `BillingEngineTest` |
| D7 | `BillingMetricsTest` |

The class name is required, because the final scoring looks for it; the names of the test methods are yours. These tests are 20% of the milestone's score, and they are scored by what they catch: the class runs on a correct build, where every test must pass, and on builds where exactly the behaviour the checklist names is broken, where at least one test must fail. A missing class, or a test that fails on the correct build, gives 0 for this part. So assert the result itself (the date, the status, the amount, what was saved or sent), not only that no exception was thrown.

`ServicesStructureTest` checks the signatures of the services you are given. It is green from the start and stays green as long as you do not change a provided signature: run it before each merge.

Your tests must pass on your code and on a correct build of SubStream. Run them on your machine against your code; the NIX bot runs them on a correct build on every Merge Request and lists any of your tests that fail there.

---

## D4 — Subscription service

**Branch:** `feature/d4-service`

**Goal:** the use cases a client application calls.

**You will practise:** dependency injection · an injected clock · `Optional` · your first JUnit tests

- File: `SubscriptionService`.
- The repositories' `findById` returns an `Optional`.
- Every rule of the domain classes still applies, and an exception from them means nothing is saved (README, Module E).

| ✓ | Rule | What to do |
| :--- | :--- | :--- |
| ☐ | BR-202 | "Today" is always `LocalDate.now(clock)` |
| ☐ | BR-501 | `registerCustomer`: new customer, random UUID, `createdAt` = today; save and return it |
| ☐ | BR-502 | `subscribe`: unknown customer → `CustomerNotFoundException` and nothing saved |
| ☐ | BR-503 | `changePlan`, `cancel`: unknown id → `SubscriptionNotFoundException` |
| ☐ | Test | `SubscriptionServiceTest`: `createdAt` comes from a fixed clock; an unknown customer throws and nothing is saved |

**Done when:** an unknown customer or subscription produces the right exception and nothing is saved.

## D5 — Billing engine: the happy path

**Branch:** `feature/d5-billing`

**Goal:** a billing run that renews every due subscription whose payment succeeds.

**You will practise:** interfaces · method decomposition · records · lambdas as test doubles

- File: `BillingEngine`.
- Each due subscription is charged **at most once per run** (BR-306); the summary counts each one once, as the table under BR-309 says.

| ✓ | Rule | What to do |
| :--- | :--- | :--- |
| ☐ | BR-301 | Process only the due subscriptions: `ACTIVE`, renewal date today or earlier |
| ☐ | BR-302 | Create the invoice with the right period, amount and dates |
| ☐ | BR-304 | `FREE`: never call the gateway; the invoice is `PAID` |
| ☐ | BR-305 | On success: invoice `PAID`, next cycle, failure counter back to 0 |
| ☐ | BR-309 | Return the summary of the run |
| ☐ | Test | `BillingEngineTest`: a due PRO subscription is billed and renewed; FREE never calls the gateway |

**Done when:** a run on a fixed date bills exactly the due subscriptions, never calls the gateway for `FREE`, and returns a correct summary.

## D6 — Billing engine: failures

**Branch:** `feature/d6-failures`

**Goal:** the engine survives everything the real world throws at it.

**You will practise:** checked and unchecked exceptions · batch robustness

- File: `BillingEngine`.

| ✓ | Rule | What to do |
| :--- | :--- | :--- |
| ☐ | BR-306 | A declined payment, a timeout or any gateway error is a failure: invoice stays `UNPAID`, counter + 1 |
| ☐ | BR-303 | A retry reuses the existing `UNPAID` invoice |
| ☐ | BR-307 | At the limit: `SUSPENDED` and exactly one notification |
| ☐ | BR-308 | One broken subscription never stops the run |
| ☐ | Test | One more in `BillingEngineTest`: three declines suspend the subscription and notify exactly once |

**Done when:** three consecutive failures suspend the subscription and notify exactly once, a retry reuses the unpaid invoice, a timeout counts as a failure, and one broken subscription never stops the run.
**Trap:** catching `Exception` around everything and hiding real errors.

**Notifications in your tests:** `EmailNotificationService` works only after D7 (it extends `TemplateNotificationService`); until then it throws, and by BR-308 the run then counts that subscription as `failed` instead of `suspended`. Your tests need a notification service of their own (`docs/primers/JUNIT5.md`).

## D7 — Reporting and notifications

**Branch:** `feature/d7-reporting`

**Goal:** business metrics with streams, and notifications with the Template Method pattern.

**You will practise:** streams · comparators · abstract classes and the Template Method pattern

- Files: `BillingMetrics`, `TemplateNotificationService`, `SlackNotificationService`.
- `EmailNotificationService` is **provided, complete**: it is your worked example of a subclass. Read it first.
- `BillingMetrics` uses the Stream API only, no loops. New to streams? The official guide: https://dev.java/learn/api/streams/

| ✓ | Rule | What to do |
| :--- | :--- | :--- |
| ☐ | BR-601 | `calculateMrr` with a stream: only `ACTIVE`, scale 2 |
| ☐ | BR-602 | `findOverdue` with a stream, oldest due date first |
| ☐ | BR-603 | `TemplateNotificationService`: build the message, store the line, return an unmodifiable list |
| ☐ | BR-603 | `SlackNotificationService`: the channel check and the `SLACK` line |
| ☐ | Test | `BillingMetricsTest`: the BR-601 example gives `257.00`; an empty list gives `0.00` |

**Done when:** the BR-601 example gives exactly `257.00` and both notification services produce the exact lines of BR-603.
