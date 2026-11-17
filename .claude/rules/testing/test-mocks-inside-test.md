# Mock Locality Rule

All mock setup — values, return values, and subject instantiation — must live **inside the test function** where they are used. Never hoist mocks into properties, `@Before`, `@BeforeEach`, `@BeforeAll`, or annotation-based injection.

## Hard Rules

1. **Declare mocks inside the test function, not in properties or `@Before`.**
   Every test is self-contained: the reader sees everything that matters without scrolling up. Create mocks with `mockk<T>()` inside the test.

2. **Do not use `@Before` / `@BeforeEach` to configure mocks or build the subject.**
   `@After` / `@AfterEach` cleanup of global state (`unmockkAll()`, `Dispatchers.resetMain()`) is fine.

3. **Do not use `@MockK`, `@InjectMockKs`, `@RelaxedMockK`, `MockKAnnotations.init`, or Mockito's `@Mock` / `@InjectMocks`.**
   Re-create the mock and the subject under test inside each test.

4. **Do not share mocked instances across tests via properties.**
   No `private val repository = mockk<SettingRepository>()` at class level.

5. **Prefer hand-written fakes for simple interfaces; use `mockk` for interaction verification.**
   Static / object / constructor mocking (`mockkStatic`, `mockkObject`, `mockkConstructor`) is allowed only inside the test itself, paired with `unmockkAll()` in a `try/finally` or `@After`.

6. **Shared test dispatcher setup is the one allowed exception.**
   A `MainDispatcherRule` (JUnit `TestRule` or JUnit 5 extension) that installs `Dispatchers.setMain` is global infrastructure, not mock configuration.

## Standard Pattern

```kotlin
// ✅ Correct — mock setup inside the test
@Test
fun `should return settings`() = runTest {
    val repository = mockk<SettingRepository>()
    coEvery { repository.findByName("x") } returns listOf(Setting("1", "x"))

    val useCase = GetSettingsUseCase(repository)
    val result = useCase("x")

    coVerify { repository.findByName("x") }
    assertEquals(listOf(Setting("1", "x")), result)
}
```

```kotlin
// ❌ Wrong — mock hoisted to a property / @Before
class GetSettingsUseCaseTest {
    private lateinit var repository: SettingRepository   // ← violates this rule
    private lateinit var useCase: GetSettingsUseCase

    @Before
    fun setUp() {
        repository = mockk()                              // ← and this
        useCase = GetSettingsUseCase(repository)
    }
}
```

## Why

Self-contained tests are easier to read, debug, and refactor. When a test fails, everything needed to understand it is visible in a single function — no tracing `@Before` chains or shared state. Tests that share mock instances through properties can interfere with each other when call counts or state leak between runs.
