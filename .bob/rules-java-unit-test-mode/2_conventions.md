# Java Unit Test Writer — Conventions

These conventions are derived directly from the existing tests in this repo.
Do not deviate from them.

---

## Annotations

### Service / component tests

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;
}
```

- Use `@ExtendWith(MockitoExtension.class)` — never `@SpringBootTest` for unit tests.
- Declare mocks with `@Mock`, inject with `@InjectMocks`.
- Never use `MockitoAnnotations.openMocks(this)` manually.

### Controller tests

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;
}
```

- Use `@WebMvcTest` with the specific controller class — loads only the web layer.
- Use `@MockBean` (Spring) for service dependencies, not `@Mock` (Mockito).

---

## Assertions

**Always use AssertJ.** Never use JUnit 5's `assertEquals`, `assertTrue`, `assertNull`, etc.

```java
// Correct
assertThat(result).isPresent();
assertThat(result.get().getProduct()).isEqualTo("Widget");
assertThat(orders).hasSize(1);
assertThat(orders).isEmpty();

// Exception assertions
assertThatThrownBy(() -> service.doSomething(id))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("expected fragment");
```

Static imports to use:
```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

---

## Mocking

Use Mockito static imports. Never use PowerMock.

```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
```

- Stub with `when(...).thenReturn(...)` before the call under test.
- Verify side effects with `verify(mock).method(args)` only when there is no
  return value to assert on (e.g. void delete methods).

---

## Test Naming

Pattern: `methodUnderTest_scenario_expectedOutcome`

```java
void getAllOrders_returnsAllOrders()
void getOrderById_existingId_returnsOrder()
void getOrderById_nonExistingId_returnsEmpty()
void updateOrderStatus_invalidTransition_throwsException()
void deleteOrder_callsRepository()
```

Rules:
- All three segments are camelCase.
- Separate segments with a single underscore.
- No underscores within a segment.
- No `test` prefix on method names.

---

## Fixture Setup

Declare shared test objects as fields. Build them in `@BeforeEach`:

```java
private Order testOrder;

@BeforeEach
void setUp() {
    testOrder = new Order();
    testOrder.setId(1L);
    testOrder.setCustomerName("John Doe");
    testOrder.setProduct("Widget");
    testOrder.setAmount(new BigDecimal("29.99"));
    testOrder.setStatus("PENDING");
}
```

- Only put fields in `@BeforeEach` that are shared across multiple tests.
- For test-local objects, declare them inline inside the test method.
