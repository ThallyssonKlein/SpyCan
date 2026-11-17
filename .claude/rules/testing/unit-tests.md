---
name: Unit Test Rule
description: Isolate the subject under test; mock all dependencies; use builder functions for repeated mock shapes; test coroutines with runTest.
type: feedback
---

# Unit Test Rule

## The Three Laws of TDD

1. Do not write production code without a failing test first.
2. Do not write more test code than is necessary to fail.
3. Do not write more production code than is necessary to pass the failing test.

## F.I.R.S.T. Rules

Tests must be **Fast**, **Independent**, **Repeatable** (in any environment), **Self-validating** (boolean pass/fail), and **Timely** (written just before the production code).

---

When writing unit tests, **isolate the subject under test completely**. Every external dependency must be mocked or faked — no real I/O, no real databases, no real HTTP calls, no real `Context`.

Stack: **JUnit 5** (or JUnit 4 if the project already uses it) + **MockK** + **kotlinx-coroutines-test** + **Turbine** (for `Flow`) + **Truth/AssertJ/kotlin.test** assertions. Tests are named `<ClassName>Test` and run on the JVM (`src/test`).

## Hard Rules

1. **Mock all dependencies injected into the subject under test.**
   Use `mockk<T>()` for collaborators. Never instantiate real repositories, data sources, or API clients inside a unit test.

2. **Never test more than one layer at a time.**
   - Testing a `ViewModel`? Mock the use case / repository — do not touch a real data layer. Assert on the exposed `StateFlow` / `UiState` and on emitted one-shot events.
   - Testing a use case? Mock the repository.
   - Testing a repository? Mock the `Dao`, the Retrofit `Service`, and the data-store — do not open a real database or network. Query correctness belongs in an instrumented test (`androidTest`).
   - Testing a mapper or validator? Call it directly with valid and invalid input.
   - Testing a `@Composable`? That is a UI test (`androidTest` or Robolectric), not a unit test.

   Every testable layer must have its own test class. Do not skip layers.

3. **Never import concrete data-layer classes in domain or presentation tests.**
   A `ViewModel` test imports the ViewModel and the interfaces it depends on — never `RoomSettingRepository` or a Retrofit-generated class.

4. **Never use Robolectric, Hilt test components, an in-memory Room database, a real `Context`, or a real network call in unit tests.**
   Anything that needs the Android framework or real infrastructure is an integration/instrumented test and lives in `src/androidTest` (or is explicitly labeled).

5. **Assert on mock interactions and returned values, not on side effects.**
   Verify behavior with `coVerify { repository.findAll() }` — not by inspecting a database or external state.

6. **Never assert on log calls.**
   Logging is an observability side effect. Do not mock `Timber`, do not plant a capturing `Tree`, and do not verify log output.

7. **Test coroutines deterministically — never with `Thread.sleep` or `delay` in real time.**
   Use `runTest`, inject the `CoroutineDispatcher` into production code (never hardcode `Dispatchers.IO`), and replace `Dispatchers.Main` with a `StandardTestDispatcher` / `UnconfinedTestDispatcher` through a `MainDispatcherRule`. Test `Flow` with Turbine's `test { awaitItem() }`.

   ```kotlin
   // ✅ Correct — virtual time, no sleeping
   @Test
   fun `should emit loading then success`() = runTest {
       val repository = mockk<SettingRepository>()
       coEvery { repository.getAll() } returns listOf(Setting("1", "x"))
       val viewModel = SettingsViewModel(repository)

       viewModel.state.test {
           assertEquals(SettingsState.Loading, awaitItem())
           assertEquals(SettingsState.Success(listOf(Setting("1", "x"))), awaitItem())
       }
   }

   // ❌ Wrong — real time and a real dispatcher
   Thread.sleep(500)
   ```

8. **Use mock builder functions when the same mock shape appears in more than one test.**
   Define a `private fun makeMockRepository(): SettingRepository` that returns a fresh mock on every call. Call it inside each test — never store the result in a property.

9. **Test names describe behavior: `` `should <outcome> when <condition>` ``.**
   Use backtick names. Prefer `@ParameterizedTest` over copy-pasted tests that differ only by input.

## Standard Pattern

```kotlin
// ✅ Mock builder — defined in the test file, called inside each test
private fun makeMockRepository(): SettingRepository = mockk()

// ✅ Use case unit test
@Test
fun `should return all settings`() = runTest {
    val repository = makeMockRepository()
    coEvery { repository.getAll() } returns listOf(Setting("1", "x"))
    val useCase = GetAllSettingsUseCase(repository)

    val result = useCase()

    coVerify { repository.getAll() }
    assertEquals(listOf(Setting("1", "x")), result)
}

// ✅ Failure path
@Test
fun `should throw when the setting is not found`() = runTest {
    val repository = makeMockRepository()
    coEvery { repository.findById("x") } returns null
    val useCase = GetSettingUseCase(repository)

    assertFailsWith<SettingNotFoundException> { useCase("x") }
}
```

```kotlin
// ❌ Wrong — real repository injected into a use case test
val realRepository = RoomSettingRepository(database.settingDao())
val useCase = GetAllSettingsUseCase(realRepository) // crosses layer boundary

// ❌ Wrong — real Context in a JVM unit test
val context = ApplicationProvider.getApplicationContext<Context>()
```

## Why

Unit tests must be fast, deterministic, and scoped to a single class's behavior. Crossing layer boundaries or touching the Android framework turns a unit test into an integration test — it slows the suite, introduces flakiness, and makes failures harder to locate. Virtual-time coroutine tests remove the largest source of mobile test flakiness: real timing.

Builder functions eliminate copy-paste drift: when an interface gains a method, you update one builder instead of every test.
