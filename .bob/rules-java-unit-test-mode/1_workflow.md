# Java Unit Test Writer — Workflow

## Before Writing Any Test

Always read the related existing test file first.

- For a service class → read `order-service/src/test/java/com/example/orders/service/OrderServiceTest.java`
- For a controller class → read `order-service/src/test/java/com/example/orders/controller/OrderControllerTest.java`

Then read the production class under test so you know the real method signatures,
return types, and exception paths before writing a single assertion.

---

## Step-by-Step Process

### 1. Identify what to test

- Ask the user which class or method needs coverage, or infer it from context.
- Read the production source file to understand inputs, outputs, and side effects.

### 2. Read existing tests

- Read the relevant `*Test.java` file in full.
- Note: assertion style, mock setup pattern, fixture fields, `@BeforeEach` usage,
  and test naming convention used in this file.

### 3. Plan the test cases

Before writing code, list the cases you intend to cover:

- Happy path (normal, valid input)
- Edge cases (empty collections, zero/null values, boundary conditions)
- Expected exceptions (what the production code throws and under what condition)

Do not pad the list — every test must assert something meaningful.

### 4. Write the tests

Follow the conventions observed in step 2. See `2_conventions.md` for the rules.

### 5. Verify completeness

Check that:

- Every new test method has at least one `assertThat` or `assertThatThrownBy` call.
- No test silently passes with an empty body.
- Exception tests assert the exception type and a meaningful part of the message.
- Verify-only tests (e.g. `verify(repo).deleteById(id)`) are used only when the
  method has no return value to assert on.

---

## File Placement

| Test type         | Directory                                                        |
|-------------------|------------------------------------------------------------------|
| Service unit test | `order-service/src/test/java/com/example/orders/service/`       |
| Controller test   | `order-service/src/test/java/com/example/orders/controller/`    |

New test files must match the package of the class under test.
