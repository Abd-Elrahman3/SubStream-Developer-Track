# Shared Milestones (M1 – M3)

Everyone completes these three milestones, whatever track they choose later. They are about object-oriented design: modelling the domain with classes that protect their own data and rules.

| Milestone | Branch | Points | Level reached |
| :--- | :--- | :--- | :--- |
| M1 Domain core | `feature/m1-domain` | 15 | — |
| M2 Invoices and generic storage | `feature/m2-invoices` | 10 | 🥉 Bronze |
| M3 Subscriptions | `feature/m3-subscription` | 15 | — |

Each milestone has a **structure test** in `src/test/java/com/substream/structure/`: `M1StructureTest`, `M2StructureTest`, `M3StructureTest`. It checks the structure of your classes (names, parameters, return types, private fields, no setters), not their logic. Run it on your machine when you finish the milestone, before you merge. On every Merge Request the NIX bot also runs it, and checks the rules of the milestone with the examples of the README.

The final evaluation checks the same rules with more cases than the README shows, so think about the limits yourself: you may write your own tests for them (optional, not graded).

Each milestone below has a **checklist of its rules**, in the order to work on them. Work from the checklist; open the README only for the full rule, its table and its examples.

---

## M1 — Domain core (started in the workshop)

**Branch:** `feature/m1-domain`

**Goal:** three small classes that hold valid data only.

**You will practise:** enums with data · encapsulation · validation and exceptions · string handling · static utility classes · `equals` and `hashCode`

- Files: `SubscriptionPlan`, `EmailValidator`, `Customer` (the customer uses the validator).

| ✓ | Rule | What to do | README |
| :--- | :--- | :--- | :--- |
| ☐ | Plan data | `SubscriptionPlan`: tier, price and user limit per constant; the five methods | 4.2 |
| ☐ | BR-102 | `EmailValidator`: the three conditions, after trimming; `null` is invalid | 5, Module A |
| ☐ | BR-101 | `Customer`: name rules after trimming | 5, Module A |
| ☐ | Fields | `Customer`: private final fields, one getter each, email stored lower-case | 4, 4.1 |
| ☐ | Equality | `equals` and `hashCode` on `id` only | 4.7 |

**Done when:** `M1StructureTest` and `SubscriptionPlanTest` pass.
**Traps:** validating the name *before* trimming it; deciding upgrades with `ordinal()`; adding setters "just in case".

## M2 — Invoices and generic storage

**Branch:** `feature/m2-invoices`

**Goal:** one more domain class in the same style as M1, and one repository that works for every entity.

**You will practise:** `BigDecimal` · object state changes · dates · generics · collections · `Optional`

- Files: `Invoice`, `InMemoryRepository`.
- `InMemoryRepository<T, ID>` is written **once** with generics. The provided customer, subscription and invoice repositories reuse it, so they start working when yours does.
- `equals` and `hashCode` of `Invoice` are provided: the same pattern you wrote for `Customer`.

| ✓ | Rule | What to do | README |
| :--- | :--- | :--- | :--- |
| ☐ | Fields | `Invoice`: private fields, getters, amount always scale 2, a negative amount → `IllegalArgumentException`, starts `UNPAID`. Store the values as given: BR-302 is the engine's job in D5 | 4.6 |
| ☐ | Paying | `markPaid()`: paying twice throws `IllegalStateException` | 4.6 |
| ☐ | BR-602 | `isOverdue(today)`: only the `Invoice` method; the report comes in D7 | 5, Module F |
| ☐ | Storage | `InMemoryRepository`: insertion order, `findAll()` cannot be changed by the caller | 6 |

**Done when:** `M2StructureTest` passes, an amount like `29` is stored as `29.00`, paying twice is refused, and `findAll()` keeps insertion order and cannot be changed by the caller.
**Traps:** comparing money with `equals` (is `29.0` equal to `29.00`?); returning your internal list from `findAll()`.

## M3 — Subscriptions

**Branch:** `feature/m3-subscription`

**Goal:** the central class of the system: a subscription that knows its dates and follows every state rule.

**You will practise:** date arithmetic · calculated values · state machines · decision tables

- File: `Subscription`. Commit after each row of the checklist. `equals` and `hashCode` are provided.

| ✓ | Rule | What to do | README |
| :--- | :--- | :--- | :--- |
| ☐ | Fields, BR-103 | Fields and constructor; the seat count must fit the plan | 4.4, 5 Module A |
| ☐ | BR-201 | `getRenewalDate()`, calculated on every call, never stored | 5, Module B |
| ☐ | BR-301 | `isDue(today)`: `ACTIVE` and renewal date today **or earlier** | 5, Module C |
| ☐ | BR-104 | `changePlan()`: every row of the plan-change table | 5, Module A |
| ☐ | BR-305 – BR-307 | `recordPaymentSuccess()` and `recordPaymentFailure(max)`: the counters and the suspension inside `Subscription` only (the Javadoc of each method lists what changes and which exception). Invoices and notifications come in the Developer track | 5, Module C |
| ☐ | BR-401 | `cancel()` and every forbidden move of the Module D table | 5, Module D |

**Done when:** `M3StructureTest` passes, every row of the BR-201 table, every row of the BR-104 table and every cell of the Module D table behaves as specified.
**Traps:** storing the renewal date in a field, or adding one month to the previous date (look at the 31 January row); a downgrade check that rejects 10 seats on PRO.

---

After M3, choose your track: [`TRACK_DEVELOPER.md`](TRACK_DEVELOPER.md) or [`TRACK_AQA.md`](TRACK_AQA.md).
