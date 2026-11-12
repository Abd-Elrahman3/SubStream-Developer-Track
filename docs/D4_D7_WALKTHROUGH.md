# D4–D7 implementation and test walkthrough

This project contains the completed Developer track. Each implementation is in its original production file; each test is in the package required by `docs/TRACK_DEVELOPER.md`. D5 and D6 intentionally share `BillingEngine.java` and `BillingEngineTest.java`, as the specification requires.

The supplied ZIP was the starter project: M1–M3 were still unimplemented. Those required foundations are completed separately in the commit history so this checkout builds and tests on its own. Original comments, including TODO markers, are preserved at the user's request; these retained markers do not mean the method is still unimplemented. Provided files, the Maven build, and the supplied tests are unchanged.

## Files to read individually

| Milestone | Implementation | Tests |
| --- | --- | --- |
| D4 | [SubscriptionService.java](../src/main/java/com/substream/service/SubscriptionService.java) | [SubscriptionServiceTest.java](../src/test/java/com/substream/service/SubscriptionServiceTest.java) |
| D5, D6 | [BillingEngine.java](../src/main/java/com/substream/service/BillingEngine.java) | [BillingEngineTest.java](../src/test/java/com/substream/service/BillingEngineTest.java) |
| D7 reporting | [BillingMetrics.java](../src/main/java/com/substream/service/BillingMetrics.java) | [BillingMetricsTest.java](../src/test/java/com/substream/service/BillingMetricsTest.java) |
| D7 notifications | [TemplateNotificationService.java](../src/main/java/com/substream/service/TemplateNotificationService.java), [SlackNotificationService.java](../src/main/java/com/substream/service/SlackNotificationService.java) | [NotificationServiceTest.java](../src/test/java/com/substream/service/NotificationServiceTest.java) |

## Running the tests

Install a full JDK 17 or newer and Maven, then run these commands from the folder containing `pom.xml`:

```bash
mvn test
mvn -Dtest=SubscriptionServiceTest,BillingEngineTest,BillingMetricsTest,NotificationServiceTest,ServicesStructureTest test
```

The complete suite passed locally: **82 tests, 0 failures, 0 errors, 0 skipped**. This includes all existing structure tests, the supplied example tests, 44 Developer-track test invocations, and 10 new foundation regression tests. Parameterized tests count once per input value. These are local results; the external NIX grading tests were not run here.

A gateway or notification service in a test is a lambda controlled by the test. Nothing is charged and no email/Slack network request is sent. The missing-customer and throwing-notifier tests verify that unexpected failures count in the summary while later subscriptions continue. Billing uses the existing `PaymentResult`, `InvoiceStatus`, `SubscriptionStatus`, and `BillingRunSummary`; it does not introduce an outcome enum or a logger.



---

# D4 — Subscription service

Implementation: `src/main/java/com/substream/service/SubscriptionService.java`  
Tests: `src/test/java/com/substream/service/SubscriptionServiceTest.java`

The service coordinates domain objects and repositories. Domain objects remain responsible for validation and lifecycle rules. Each method finishes construction or a domain operation **before** calling `save`, so an exception reaches the caller without a repository write. Existing comments and public signatures are preserved.

## Every implementation method

- **`SubscriptionService(customerRepository, subscriptionRepository, clock)`** receives its collaborators instead of constructing them. `Objects.requireNonNull` fails immediately if any is missing. Supplying the clock makes dates reproducible in tests, including its time zone.
- **`registerCustomer(name, email)` — BR-501 / BR-202.** Generates a fresh UUID with `UUID.randomUUID().toString()`. Constructs the customer using `LocalDate.now(clock)` for `createdAt`, then saves and returns it. Invalid name/email exceptions from `Customer` occur before saving.
- **`subscribe(customerId, plan, seatCount)` — BR-502 / BR-202.** Calls `findById` first. The returned `Optional` either contains a customer or `orElseThrow` raises `CustomerNotFoundException`. A known customer gets a new subscription with a fresh UUID and the clock's current date. `Subscription` enforces BR-103 and initializes its ACTIVE status; the service saves and returns the result. No check forbids multiple subscriptions for the same customer.
- **`changePlan(subscriptionId, targetPlan)` — BR-503.** Finds the subscription or throws `SubscriptionNotFoundException`. Delegates plan-change validation to `subscription.changePlan(targetPlan)`, then saves and returns the changed subscription. A forbidden change throws before `save`.
- **`cancel(subscriptionId)` — BR-503.** Finds the subscription or throws `SubscriptionNotFoundException`. Calls its lifecycle method `cancel()`, then saves and returns it. For example, attempting to cancel an already cancelled subscription propagates `InvalidStatusTransitionException` without a new write.

## How the test fixture works

JUnit creates a new instance of the test class for each test, so the repositories and counters start fresh. The fixed instant is `2025-03-31T23:30:00Z` with the `Europe/Helsinki` zone: the expected local date is **2025-04-01**. A service that ignores the injected clock or uses UTC directly produces the wrong date.

The counting repositories extend the real in-memory implementations and override `save` to count invocations before delegating. Repository size alone is insufficient to prove “nothing is saved”: overwriting an existing entry leaves the size unchanged. Counting the saves makes these forbidden writes visible.

Each test follows arrange → act → assert: prepare the customer/subscription, call one use case (or compare two deliberate calls), then inspect the observable date, ID, status, plan, stored value, or exception.

## Every test and helper

| Method | Setup, call, and expected result | Bug it detects |
| --- | --- | --- |
| `registrationUsesFixedClockAndSavesCustomer` | Registers a valid customer; expects April 1, the returned customer in storage, and one save. | Ignoring the supplied clock/time zone or forgetting to persist registration. |
| `registrationCreatesDistinctRandomUuidIds` | Registers two customers with the same inputs; expects different valid version-4 UUIDs. | Reusing IDs, generating deterministic IDs from customer data, or using the wrong UUID representation. |
| `unknownCustomerThrowsWithoutSavingAnything` | Subscribes a nonexistent customer; expects `CustomerNotFoundException`, no subscriptions, and no save calls. | Creating orphan subscriptions, throwing the wrong exception, or saving before checking the customer. This is a required D4 checklist test. |
| `invalidCustomerDataIsPropagatedWithoutSaving` | Tries a blank name and then an invalid email; both must raise `InvalidCustomerDataException`, with no customer saves. | Swallowing domain validation errors or writing invalid customer data. |
| `subscribeUsesFixedClockAndAllowsMultipleSubscriptionsForOneCustomer` | Registers one customer and subscribes it twice. Checks the date, ACTIVE status, customer association, plan, seats, persisted results, and distinct UUIDs; customer save count stays at one. | Incorrect subscription construction, missing persistence, duplicate IDs, rejecting a second subscription, or rewriting the customer during subscription. |
| `invalidSeatCountIsPropagatedWithoutSavingSubscription` | Attempts two seats on FREE; expects `IllegalArgumentException` and zero subscription writes. | Bypassing BR-103 or saving after failed construction. |
| `unknownSubscriptionCannotBeChangedOrCancelled` | Calls each operation with an unknown ID; expects `SubscriptionNotFoundException` and no writes. | Mishandling an empty `Optional`, silently ignoring unknown IDs, or saving a substitute subscription. |
| `changePlanSavesOnlyTheSelectedSubscription` | Creates two FREE subscriptions, upgrades the first to PRO, checks its returned ID and stored plan; the other stays FREE. Exactly one additional save occurs. | Changing the wrong subscription, forgetting to save the result, or updating unrelated subscriptions. |
| `forbiddenPlanChangeIsPropagatedWithoutSaving` | Tries to downgrade a two-seat PRO subscription to FREE. Expects `InvalidPlanTransitionException`, the original PRO plan, and no additional save. | Ignoring the domain's downgrade constraint, swallowing its exception, or saving a failed operation. |
| `cancelSavesOnlyTheSelectedSubscription` | Creates two ACTIVE subscriptions and cancels one. Expects that subscription to be stored as CANCELLED, the other to remain ACTIVE, and one additional save. | Cancelling the wrong object or omitting the persistence step. |
| `repeatedCancellationIsPropagatedWithoutAnotherSave` | Cancels a subscription successfully and tries again. Expects `InvalidStatusTransitionException`, unchanged CANCELLED state, and no third save. | Treating cancellation as repeatable or saving despite a forbidden transition. |
| `assertRandomUuid(id)` | Parses the ID, compares its canonical string form, and verifies UUID version 4. | A shared assertion helper used by the ID tests; it is not a separate JUnit test. |
| `CountingCustomerRepository.save(customer)` | Increments `saveCalls` and delegates to the real repository. | Test support for observing customer writes. |
| `CountingSubscriptionRepository.save(subscription)` | Increments `saveCalls` and delegates to the real repository. | Test support for observing subscription writes. |

Run only these tests with `mvn -Dtest=SubscriptionServiceTest test`. Run the provided service-signature check with `mvn -Dtest=ServicesStructureTest test`.


---

# D5 and D6 — Billing engine

Implementation: `src/main/java/com/substream/service/BillingEngine.java`.

D5 builds the successful-renewal path. D6 extends that same class with retry handling, failed payments, suspension, and batch isolation. The final file contains both milestones.

## Every implementation method and supporting type

- **Constructor.** Receives the three repositories, gateway, notification service, configuration, and clock. Each is required and stored in a private final field. Tests inject controlled collaborators; production code does not construct its own repositories or clock.
- **`runRenewals()`.** Reads `LocalDate.now(clock)` once for the run and walks the subscription repository once. A subscription is selected by `isDue(today)`: ACTIVE with renewal date today or earlier. It increments `eligible` and processes that subscription once. The returned invoice is the actual domain object: a `PAID` invoice increments `succeeded`; otherwise a `SUSPENDED` subscription increments `suspended`; otherwise `failed` increases. Exactly one counter increases for each selected subscription. It does not loop through missed months. A `RuntimeException` during a selected subscription's processing is counted as failed; the next subscription is still processed. Finally it returns `BillingRunSummary`, with `eligible = succeeded + failed + suspended`.
- **`processRenewal(subscription, today)`.** Resolves the customer, raising `CustomerNotFoundException` if absent. Obtains the period's invoice. Java's short-circuit `||` means FREE immediately succeeds without invoking `charge`. On success, it marks the invoice paid and asks the subscription to advance its anchored billing cycle and reset failures. On failure, it calls `recordPaymentFailure(config.maxFailedAttempts())`; the boolean says whether this attempt caused suspension. It saves the invoice once after the outcome is known, then saves the subscription. If newly suspended, it notifies. It returns the processed `Invoice` so the caller can inspect its existing status. A notification exception propagates to the run's per-subscription catch and counts as `failed`, while the persisted subscription remains suspended. It will therefore be excluded from later runs, avoiding another charge or notice.
- **`findOrCreateInvoice(subscription, today)`.** Uses `(subscription ID, current renewal date)` as the lookup key. An existing unpaid invoice is reused, retaining its ID, amount, issue date, and due date. Otherwise a UUID invoice is created with the anchored renewal date as period start, one monthly plan price regardless of seats, today's issue date, and `today + config.invoiceDueDays()` as due date. The method does not save the invoice. If inconsistent data already has a paid invoice for the same still-due period, it raises `IllegalStateException` to avoid charging it again.
- **`charge(customer, invoice)`.** Makes exactly one gateway call with the customer's ID and the invoice's amount. It reads `PaymentResult.isSuccess()`. Only gateway timeout/runtime exceptions are translated into payment failure here; no new result type is created. Repository and notification errors are handled by the separate per-subscription boundary instead of being mistaken for a declined charge.

## Example: three declines

For a PRO subscription due on March 1 with a failure limit of 3:

| Run | Invoice | Failures | Subscription | Summary `(eligible, succeeded, failed, suspended)` | Notice |
| --- | --- | --- | --- | --- | --- |
| 1 | New, UNPAID | 1 | ACTIVE | `(1, 0, 1, 0)` | None |
| 2 | Same invoice, UNPAID | 2 | ACTIVE | `(1, 0, 1, 0)` | None |
| 3 | Same invoice, UNPAID | 3 | SUSPENDED | `(1, 0, 0, 1)` | Once |
| 4 | No attempt | 3 | SUSPENDED | `(0, 0, 0, 0)` | None |

A successful retry instead marks that same invoice PAID, moves the cycle forward once, and resets the failure counter to zero. Renewal dates always come from the subscription's original start date, so a January 31 start returns to March 31 after February's shorter month.



---

## BillingEngineTest.java (D5 and D6)

The source of truth is [README Module C](../README.md#module-c--billing), [the Developer Track checklist](TRACK_DEVELOPER.md), and [the JUnit primer](primers/JUNIT5.md). Tests call the public `runRenewals()` method and assert documented results using the existing domain types. They do not inspect private methods or rely on an `Outcome` enum or logging.

The checklist requires three cases: `billsDueProAtMonthlyPriceAndRenews`, `freeRenewsWithoutCallingGateway`, and `threeDeclinesSuspendAndNotifyExactlyOnce`. Their JUnit display names identify them as required. The other cases are regression coverage for the same README rules, with rule IDs in their display names.

| Rule | Coverage |
| --- | --- |
| BR-202 | Fixed clock, including its time zone |
| BR-301 | Due today and overdue; future, suspended and cancelled entries excluded |
| BR-302 | Customer ID, plan price regardless of seats, period, issue date, configured due date |
| BR-303 | Same invoice on retry; exactly one save per attempt, after the gateway outcome |
| BR-304 | FREE creates a paid zero invoice and never calls the gateway |
| BR-305 | Paid invoice, next cycle, ACTIVE status and reset failure counter |
| BR-306 | Decline, checked timeout and runtime error; one charge, unpaid invoice, unchanged renewal |
| BR-307 | Third failure suspends and notifies once; configurable limit; two failures then success |
| BR-308 | Missing customer and notification exception count failed; later subscriptions continue |
| BR-309 | Exclusive success/failure/suspension counts, notification-error exception, empty run |

`RecordingInvoiceRepository` is a small test-only subclass of the supplied `InMemoryInvoiceRepository`. It uses real storage and records the status at each save to check BR-303. Checking repository size alone cannot reveal multiple saves of the same ID. Payment and notification collaborators are lambdas, as shown in the project's primer.

All tests are in `src/test/java/com/substream/service/BillingEngineTest.java`. Each JUnit test gets fresh in-memory repositories. The fixed clock represents March 1, 2024. Lambdas replace the gateway and notification service, so no real charge or notification occurs.

| Method | Setup, action, and what the assertions prove |
|---|---|
| `createCustomer` (`@BeforeEach`) | Saves customer `c-1` before each test, giving valid subscriptions a known owner. |
| `billsDueProAtMonthlyPriceAndRenews` | Creates a due PRO subscription with ten seats. The gateway verifies customer ID and the single `29.00` monthly price, and verifies nothing was saved prematurely. After renewal, checks paid invoice, fixed-clock dates, one save/charge, next cycle and summary. Catches charging per seat, wrong dates, and missing renewal. |
| `freeRenewsWithoutCallingGateway` | Gives FREE a gateway that fails the test immediately if invoked. Checks paid zero invoice, exactly one save, next renewal and success summary. |
| `skipsFutureCancelledAndSuspendedSubscriptions` | Builds one subscription in each excluded category. A fail-on-call gateway and empty invoice repository prove none was billed. |
| `billsAnOverdueSubscriptionOnlyOncePerRun` | Uses a January 1 start with March 1 today. The February period is billed once; the March period remains due for a future run. Checks one charge/cycle and anchored period date. |
| `usesConfiguredInvoiceDueDays` | Supplies a zero-day due interval and verifies the invoice is due today, rather than a hard-coded fourteen days later. |
| `usesTheInjectedClocksTimeZoneForToday` | Supplies a clock whose local date is March 2 although its UTC date is March 1. A March 2 renewal is eligible and its issue date is March 2. This detects using the system date or ignoring the clock's zone. |
| `declineKeepsInvoiceUnpaidAndRenewalDateUnchanged` | Returns a declined result and invokes the shared failure assertions. |
| `timeoutCountsAsPaymentFailure` | Throws the checked timeout exception and invokes the same failure assertions. |
| `gatewayRuntimeExceptionCountsAsPaymentFailure` | Throws a runtime gateway error and verifies it still updates payment-failure state, rather than only affecting the summary. |
| `retryReusesInvoiceAndResetsFailureCounterAfterSuccess` | Declines PRO twice, matching the BR-307 example, then retries successfully the next day. The existing unpaid invoice retains its original ID, `29.00` amount, issue/due dates, and period. Success pays it, advances one cycle and clears the failure counter. The three attempts produce exactly three saves: UNPAID, UNPAID, then PAID. |
| `threeDeclinesSuspendAndNotifyExactlyOnce` | Runs the same engine three times with declines. First and second runs count as failed without notification; third suspends and records one notification. A fourth run does nothing, so there are only three charges and one notice, using one invoice throughout. |
| `usesConfiguredSuspensionLimitAndNotifiesWithCustomerAndSubscription` | Uses a one-failure limit. The recording notifier verifies that it receives the correct customer and subscription after suspension and counter update. |
| `missingCustomerCountsAsFailedAndDoesNotStopLaterSubscriptions` | Places an orphan subscription before a valid one. The orphan counts failed, only the valid customer's gateway is called, and that subscription renews. |
| `notificationExceptionCountsAsFailedAndDoesNotStopLaterSubscriptions` | Starts one subscription one failure below suspension and makes its notifier throw. It remains suspended with an unpaid invoice but counts as failed, while the following FREE subscription renews with a paid invoice. A second run has no eligible subscriptions and sends no second notice. |
| `mixedRunCountsEveryEligibleSubscriptionExactlyOnce` | Processes a free success, an ordinary decline and a decline reaching the suspension limit. Checks each stored invoice and subscription status, all four summary values, and their arithmetic invariant. Both the successful and ordinarily declined subscriptions remain ACTIVE, so status alone cannot identify payment success. |
| `emptyRepositoryProducesEmptySummary` | Verifies an empty run has zero counters, no invoices and no gateway calls. |

Private helpers:

- `dueSubscription` creates and saves a subscription starting one month before the fixed date, so it is due today.
- `engine(PaymentGateway)` supplies the standard recorder, defaults and clock; the overloaded `engine` allows one test to vary a collaborator or configuration.
- `assertPaymentFailure` checks the common contract of decline, timeout and runtime gateway errors: exactly one gateway call, no invoice save before the gateway outcome, one unpaid invoice saved once, one failure, no cycle/date advance, active state and no notice.
- `assertMoney` compares monetary values with `BigDecimal.compareTo`, avoiding accidental sensitivity to scale; the happy-path test checks scale separately where required.
- `RecordingInvoiceRepository.save` stores each invoice's status at the time of saving, then delegates to the real repository. Recording the enum value rather than retaining only the mutable invoice reference reveals an incorrect pre-payment save.


---

# D7 — Reporting and notifications

D7 changes three production classes and adds two test classes. `EmailNotificationService` is provided and remains unchanged. All existing comments in the three production files are retained, including the starter TODO comments; their method bodies are now implemented.

## `BillingMetrics.java`

This class contains calculations that do not change subscriptions or invoices. Its private constructor prevents callers from creating an instance because both public methods are static.

- **`calculateMrr(Collection<Subscription> subscriptions)`** implements BR-601 with a stream. `filter` keeps subscriptions whose status is `ACTIVE`; `map` converts each remaining subscription into its plan's monthly price. `reduce` adds those prices, beginning with `new BigDecimal("0.00")`, so an empty stream already has the right zero value. `setScale(2)` guarantees two decimal places. Seat count never multiplies the price: one PRO subscription contributes `29.00`, whether it has one seat or ten. Suspended and cancelled subscriptions contribute nothing.
- **`findOverdue(Collection<Invoice> invoices, LocalDate today)`** implements BR-602 with another stream. `filter` delegates the overdue decision to `Invoice.isOverdue(today)`, keeping the rule in the domain object: the invoice must be unpaid and its due date must be strictly before today. `sorted(Comparator.comparing(Invoice::getDueDate))` puts the oldest due date first. `toList()` collects the selected invoices without modifying the input collection or invoices. An empty stream yields an empty list.

## `TemplateNotificationService.java`

The Template Method pattern puts the shared algorithm in a parent class while subclasses supply the steps that differ. The private, final `sentMessages` field holds a separate `ArrayList` for each service object. `final` means the list reference cannot be reassigned; the service can still append messages to the list.

- **`notifySuspension(Customer customer, Subscription subscription)`** builds the exact BR-603 message from the customer's name, the subscription's plan and ID, and its current failed-payment count. It then calls `resolveRecipient(customer)` and passes that result with the message to `format(...)`. Only after formatting succeeds does it append the complete line to `sentMessages`. The method is `final`, so subclasses cannot replace the common message-building and storage sequence. This educational notification service stores lines in memory; it does not contact email or Slack services.
- **`getSentMessages()`** returns `List.copyOf(sentMessages)`, an unmodifiable snapshot of all currently stored lines in delivery order. Callers can read the history but cannot add or remove entries through the returned list. Request the list again to see later deliveries. A new service returns an empty list.
- **`resolveRecipient(Customer customer)`** is an existing abstract extension point. Email uses the customer's email address; Slack uses its configured channel.
- **`format(String recipient, String message)`** is the other abstract extension point. It wraps the common message in the channel-specific prefix. The existing email subclass returns `EMAIL to <recipient>: <message>`.

## `SlackNotificationService.java`

- **`SlackNotificationService(String channel)`** rejects `null`, a string that does not start with `#`, or a string shorter than two characters, throwing `IllegalArgumentException`. A valid channel is stored exactly as supplied in a private, final field. It is deliberately not trimmed or otherwise normalized: BR-603 rejects `" #billing"` and permits any character following the initial `#`.
- **`resolveRecipient(Customer customer)`** returns the configured channel. The signature includes a customer because the parent algorithm supports different channels, but Slack's recipient does not depend on that customer.
- **`format(String recipient, String message)`** returns the exact required line: `SLACK <recipient>: <message>`. The inherited final `notifySuspension` method stores that line.

## `BillingMetricsTest.java`

The test class uses a constant date, so test results do not depend on the day the tests run. Monetary assertions compare the numeric value using `BigDecimal.compareTo`, then check `scale()` separately where required.

- **`mrrMatchesTheBusinessRuleExample()`** constructs the six subscriptions from the BR-601 example: two active PRO, one active ENTERPRISE, one active FREE, one suspended PRO, and one cancelled ENTERPRISE. The active subscriptions have different seat counts. It calls `calculateMrr` and checks the numeric total `257.00` and scale `2`. This catches multiplying price by seat count, including inactive subscriptions, leaving out an active plan, and returning the wrong decimal scale.
- **`emptySubscriptionsHaveZeroMrrWithTwoDecimalPlaces()`** passes an empty list and checks numeric zero and scale `2`. It catches reductions that fail on empty input or produce plain `0` without the required scale.
- **`subscriptionsWithoutAnActiveOneHaveZeroMrr()`** passes only suspended and cancelled subscriptions and checks `0.00`. It distinguishes filtering every entry out from receiving an empty collection initially.
- **`overdueInvoicesAreUnpaidStrictlyPastDueAndSortedOldestFirst()`** mixes two overdue unpaid invoices, a paid invoice with an older due date, an invoice due today, and a future invoice. The input deliberately puts the newer overdue invoice before the older one. It checks that the result is exactly the two unpaid past-due invoices, with the oldest first. This covers filtering, the strict date boundary, and sorting together.
- **`emptyInvoicesHaveNoOverdueInvoices()`** checks that an empty input produces an empty result.
- **`invoicesWithoutOverdueEntriesReturnAnEmptyList()`** supplies a paid past-due invoice plus unpaid invoices due today and in the future. It checks that no entry qualifies.
- **`subscription(...)` helper** creates subscriptions with explicit IDs, plans, and seat counts, anchored one month before the fixed date.
- **`invoice(...)` helper** creates an unpaid invoice with a known amount and the due date required by the individual test.

## `NotificationServiceTest.java`

These tests exercise the template algorithm through its real email and Slack subclasses. Expected messages are literal strings rather than being generated with the implementation's formatting logic, so an incorrect implementation cannot accidentally teach the test to expect the same mistake.

- **`emailUsesTheExactSuspensionMessage()`** creates a PRO subscription suspended after three failures, calls the supplied email service, and compares the stored list against the exact required email line. It verifies the recipient, all message fields, punctuation, and spacing.
- **`slackUsesTheConfiguredChannelAndActualFailureCount()`** uses an ENTERPRISE subscription suspended after one failure. It checks the exact Slack line and catches hard-coding either the plan or the default failure threshold of three.
- **`deliveredMessagesStayInDeliveryOrderAndCannotBeChanged()`** sends two different notifications, checks their exact order and content, and verifies that both `add` and `clear` throw `UnsupportedOperationException`. It then checks that the service still retains both messages.
- **`eachServiceHasItsOwnInitiallyEmptyMessageHistory()`** creates two email services and a Slack service. After sending through only the first, it verifies that the first has one message and the others remain empty. This catches accidentally making the history static or sharing it between service objects.
- **`invalidSlackChannelsAreRejected(String channel)`** is a parameterized test. JUnit runs it separately for `""`, `"#"`, `"billing"`, and `" #billing"`; every case must throw `IllegalArgumentException`. These are the invalid strings explicitly listed in BR-603. The test does not assert an exception type for `null`, because the README does not explicitly specify that case; requiring `IllegalArgumentException` for null produced a false alarm on the reference build.
- **`validSlackChannelsAreUsedExactlyAsGiven(String channel)`** runs separately for `"#billing "`, `"# "`, and `"#Billing"`. Each value starts with `#` and has another character. It sends a notification and checks the complete expected line containing the original channel, catching unwanted trimming, case conversion, or stricter validation than BR-603 specifies.
- **`suspendedSubscription(...)` helper** creates a subscription and records the requested number of failures, using that number as the suspension threshold. The result has a genuine suspended state and failure count, without setters, reflection, or mocks.


---

# Prerequisite foundations: M1–M3

The uploaded ZIP contained starter stubs for these classes. D4–D7 call their getters and business methods, so the following implementations are needed for the full project to compile and for the service tests to exercise real domain behavior. Existing specification comments, including original TODO comments, were retained as requested.

## `src/main/java/com/substream/domain/SubscriptionPlan.java`

- The enum constructor stores `tier`, `monthlyPrice`, and `userLimit` in private final fields. FREE costs `0.00` and supports one seat, PRO costs `29.00` and supports ten seats, and ENTERPRISE costs `199.00` and supports up to `Integer.MAX_VALUE` seats.
- `getMonthlyPrice()` and `getUserLimit()` return the stored plan settings.
- `isUnlimited()` checks whether the limit equals `Integer.MAX_VALUE`.
- `canAccommodate(seats)` requires at least one seat and no more than the plan's limit. This is the single capacity rule used by subscription creation.
- `isUpgradeFrom(other)` compares the explicit numeric tiers. It does not rely on enum declaration order or `ordinal()`.

## `src/main/java/com/substream/domain/EmailValidator.java`

- The private constructor prevents creating an unnecessary validator object.
- `isValid(email)` rejects null, trims the input, rejects whitespace within the trimmed value, and uses `indexOf`/`lastIndexOf` to require exactly one `@` after at least one character. A dot must appear after the `@`. It intentionally does not impose additional email rules: for example, `a@.` satisfies the stated BR-102 rule. No regular expressions are used.

## `src/main/java/com/substream/domain/Customer.java`

- The constructor validates the name and email before storing any data. A name must contain 1–100 characters after trimming; invalid data raises `InvalidCustomerDataException`. The stored name retains internal spaces. The email is trimmed and lowercased with `Locale.ROOT`, so the result does not change with the machine's locale.
- `getId()`, `getName()`, `getEmail()`, and `getCreatedAt()` expose immutable data through getters.
- `equals(other)` compares customer IDs only. `hashCode()` uses the same ID, keeping hash-based collections consistent with equality.

## `src/main/java/com/substream/domain/Invoice.java`

- The constructor rejects a negative amount before rounding and stores the amount at scale 2. It uses `HALF_UP` if more than two decimal places are supplied; the required plan prices already have scale 2. The other supplied invoice values are stored and status begins as UNPAID. The billing engine determines which dates and price should be supplied.
- `getId()`, `getSubscriptionId()`, `getPeriodStart()`, `getAmount()`, `getIssueDate()`, `getDueDate()`, and `getStatus()` return the stored values.
- `markPaid()` rejects an already-paid invoice, otherwise changes UNPAID to PAID.
- `isOverdue(today)` returns true only for an unpaid invoice whose due date is strictly earlier than today. An invoice due today is not overdue.
- The provided `equals()` and `hashCode()` implementations remain unchanged and compare IDs.

## `src/main/java/com/substream/domain/Subscription.java`

- The constructor checks the four required non-null inputs and calls `plan.canAccommodate(seatCount)`. Invalid capacity raises `IllegalArgumentException`. A valid subscription starts ACTIVE with billing cycle 0 and failed-payment counter 0.
- The eight ordinary getters return ID, customer ID, plan, status, seat count, start date, billing cycle, and failed-payment count.
- `getRenewalDate()` calculates `startDate.plusMonths(billingCycle + 1)` each time. It uses the original start date so a January 31 subscription renews on February 29 and then March 31 in 2024. The arithmetic is widened to long before adding 1.
- `isDue(today)` requires ACTIVE status and a renewal date on or before today.
- `changePlan(target)` rejects inactive subscriptions and the same plan, then checks that the target accommodates the existing seats. All current plan tiers have increasing capacity, so this permits every upgrade and only valid downgrades. Validation finishes before the plan is changed. Null targets are explicitly rejected.
- `recordPaymentSuccess()` first requires ACTIVE status, increments the paid-cycle count, and clears consecutive failures. The engine separately marks the invoice paid.
- `recordPaymentFailure(max)` first requires ACTIVE status and a positive threshold. It increments the counter before checking the limit. Reaching the limit changes status to SUSPENDED and returns true, telling the engine to send the one suspension notice. It leaves the renewal date unchanged.
- `cancel()` accepts ACTIVE or SUSPENDED subscriptions. A second cancellation throws `InvalidStatusTransitionException` and leaves state unchanged.
- The provided ID-based `equals()` and `hashCode()` remain unchanged.

## `src/main/java/com/substream/repository/InMemoryRepository.java`

- A private `LinkedHashMap` belongs to each repository instance. Its keys are entity IDs and it preserves insertion order.
- `save(entity)` inserts or replaces the entity and returns it. Replacing an existing ID preserves its position and does not increase the count.
- `findById(id)` returns an `Optional`, empty for an unknown ID.
- `findAll()` returns an unmodifiable list copied from the map's values, in insertion order.
- `count()` returns the number of stored IDs.

## `src/test/java/com/substream/domain/DomainFoundationTest.java`

Each test constructs real domain objects and asserts the externally visible outcome.

- `customerTrimsNameNormalizesEmailAndUsesIdForEquality`: verifies trimming, preservation of internal spaces, lowercase email, the supplied creation date, and equal IDs having equal hash codes.
- `invalidCustomerValuesAreRejectedButMinimalValidEmailIsAllowed`: checks blank/overlong names and malformed emails are rejected. It also checks the minimal `a@.` example to prevent imposing stricter email rules than the specification.
- `seatCountsMustFitEachPlanIncludingPositiveMinimum`: checks zero/negative seats are rejected for every plan, exact maximum capacity is accepted, and PRO rejects 11 seats. This directly protects the earlier BR-103 bug.
- `renewalDatesStayAnchoredAtMonthEnd`: starts January 31, 2024, verifies the due boundary, and confirms paid cycles produce March 31 and April 30 rather than drifting from February 29.
- `failureSuspendsAtLimitAndForbiddenCallsLeaveStateUnchanged`: checks that failures one and two return false and the third returns true, preserves the renewal date, verifies forbidden payments do not alter counters, and confirms suspension can be cancelled but cancellation cannot repeat.
- `successClearsConsecutiveFailuresAndAdvancesOneCycle`: records two failures followed by a success and asserts the counter resets, the cycle becomes one, and status remains ACTIVE.
- `planChangesHonorCapacityAndLifecycleWithoutPartialChanges`: checks a permitted downgrade at capacity, forbidden capacity and same-plan changes, an upgrade, and a rejected change after cancellation. It verifies rejected changes preserve the plan.
- `invoiceUsesScaleTwoAndStrictOverdueBoundaryAndCannotBePaidTwice`: checks scale and amount, the strict overdue boundary, payment removing overdue status, rejection of duplicate payment, and rejection of even a tiny negative amount before rounding.
- `subscription(plan, seats)` is a test helper that creates predictable objects anchored at the shared January 31 date.

## `src/test/java/com/substream/repository/InMemoryRepositoryTest.java`

- `replacingAnIdPreservesInsertionOrderAndCount`: saves two customers, replaces the first ID with new data, and checks count, order, replacement content, and an empty result for an unknown ID.
- `resultListIsUnmodifiableAndRepositoryInstancesAreIndependent`: verifies callers cannot clear the returned list and that saving in one repository does not populate another.
- `customer(id, name)` creates valid customer fixtures with a fixed date and email so each test focuses on storage behavior.
