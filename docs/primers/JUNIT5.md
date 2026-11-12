# JUnit 5 in One Page

A test is a method that calls your code and checks the result. If a check fails, the test fails and tells you why. The working pattern is `src/test/java/com/substream/domain/SubscriptionPlanTest.java`; open it next to this page.

## Where tests live

Same package as the class, under `src/test/java`, named `<Class>Test`:
`src/main/java/com/substream/domain/Customer.java` → `src/test/java/com/substream/domain/CustomerTest.java`.

Run one: click the green arrow next to the class or method in IntelliJ. Run all: right-click `src/test/java` → **Run 'All Tests'**.

## The shape of every test

```java
@Test
@DisplayName("a name is stored trimmed")
void storesNameTrimmed() {
    // Arrange: prepare the inputs
    LocalDate created = LocalDate.of(2024, 1, 15);
    // Act: call the code under test
    Customer customer = new Customer("c-1", "  Acme  ", "a@acme.com", created);
    // Assert: check the result
    assertEquals("Acme", customer.getName());
}
```

## The things you need

| You want to… | Write |
| :--- | :--- |
| Check a value | `assertEquals(expected, actual)` — expected first |
| Check a condition | `assertTrue(...)`, `assertFalse(...)` |
| Check an exception | `assertThrows(InvalidCustomerDataException.class, () -> new Customer(...))` |
| Check several things at once | `assertAll(() -> ..., () -> ...)` |
| Check money | `assertEquals(0, new BigDecimal("29.00").compareTo(actual))` |
| Run one test with many inputs | `@ParameterizedTest` with `@CsvSource({"in, expected", ...})` or `@ValueSource(strings = {...})` |
| Group related tests | an inner class with `@Nested` |
| Prepare objects before each test | a method with `@BeforeEach` |

## Services: a fixed clock and real repositories

Services read "today" from a `Clock`. In a test, freeze it, so every run sees the same date:

```java
Clock clock = Clock.fixed(Instant.parse("2024-03-01T00:00:00Z"), ZoneOffset.UTC);
InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
InMemorySubscriptionRepository subscriptions = new InMemorySubscriptionRepository();
SubscriptionService service = new SubscriptionService(customers, subscriptions, clock);
```

Use the real in-memory repositories: after the call, ask them what was saved (`subscriptions.count()`, `findById(...)`).

## Replacing the payment gateway (Developer track)

`PaymentGateway` and `NotificationService` each have one method, so a test can replace them with a lambda. No library is needed:

```java
PaymentGateway alwaysPays     = (customerId, amount) -> PaymentResult.success("t-1");
PaymentGateway alwaysDeclines = (customerId, amount) -> PaymentResult.failed("declined");
PaymentGateway mustNotBeCalled = (customerId, amount) -> fail("the gateway was called");

List<String> notified = new ArrayList<>();
NotificationService recorder = (customer, subscription) -> notified.add(subscription.getId());
```

Pass them to the `BillingEngine` constructor, run `runRenewals()`, then check the repositories and the `notified` list.

## Good tests

- One behaviour per test, with a name that says it: `rejectsNameLongerThan100Characters`.
- Test the **boundaries**: for "at most 100 characters", test 100 and 101.
- Test what must **fail** as well as what must work.
- Expected values come from the README, never from running the code and copying its output.

Official guide: https://junit.org/junit5/docs/current/user-guide/
