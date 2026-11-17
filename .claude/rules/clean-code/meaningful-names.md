# Meaningful Names Rule

Every identifier — variable, function, class, object, interface — must reveal its purpose without requiring a comment.

## Hard Rules

1. **Names must reveal intent.**

   ```kotlin
   // ✅ Correct
   val daysSinceLastModification = 5
   fun getActiveAccounts(userId: String): List<Account>

   // ❌ Wrong
   val d = 5 // elapsed days
   fun get(id: String): List<Account>
   ```

2. **Avoid disinformation.**
   Do not use `List` in a name unless the value is actually a `List`. Do not use `Data` or `Info` as meaningless suffixes.

   ```kotlin
   // ✅ Correct
   val accountGroup: Set<Account> = hashSetOf()

   // ❌ Wrong
   val accountList: Set<Account> = hashSetOf() // it's a Set, not a List
   ```

3. **Make meaningful distinctions.**
   Avoid numbered suffixes (`a1`, `a2`) and noise words (`Manager`, `Processor`, `Handler`, `Helper`, `Util`) unless the distinction is real and clear.

4. **Use pronounceable and searchable names.**
   Single-letter names are allowed **only** for loop counters in a tiny scope and for implicit `it` / one-token lambda parameters. Use named lambda parameters when a lambda nests or exceeds one line.

5. **Follow Kotlin and Android naming conventions.**
   - Classes, objects, interfaces, enums, type aliases: `PascalCase`.
   - Functions, properties, parameters, local variables: `camelCase`.
   - Constants (`const val`, top-level or `object` `val` holding deeply immutable data): `UPPER_SNAKE_CASE`.
   - Packages: all lowercase, no underscores (`com.acme.billing.invoice`).
   - **Composable functions that emit UI are `PascalCase` nouns** (`OrderCard`); composables that return a value are `camelCase` (`rememberOrderState`).
   - Backing properties for exposed state: `_state` (private mutable) and `state` (public read-only).
   - Do **not** prefix interfaces with `I` or suffix implementations with `Impl` when a meaningful name exists (`SettingRepository` and `RoomSettingRepository`, not `ISettingRepository` / `SettingRepositoryImpl`).
   - Boolean properties read as predicates: `isActive`, `hasExpired`, `canSubmit`.
   - Android resources use `snake_case` with a type/screen prefix (`order_list_title`, `ic_order_24`, `fragment_orders`).
   - Android components follow their role suffix: `OrdersActivity`, `OrdersViewModel`, `SyncWorker`, `OrdersUiState`.

6. **Classes must be nouns; functions must be verbs.**

   ```kotlin
   // ✅ Correct
   class CustomerRepository
   suspend fun fetchAll(): List<Customer>
   fun onSubmitClicked()

   // ❌ Wrong
   class CustomerManager       // "Manager" is a noise word
   fun allCustomers()          // missing verb
   ```

7. **Name UI events with `on<Subject><Event>` and state with `<Screen>UiState`.**
   Callbacks passed into composables are `onXxxClick`, `onXxxChange`; ViewModel entry points mirror them (`onSubmitClicked`).

## Why

Poorly named identifiers shift cognitive load onto every reader. A name that reveals intent is self-documenting: it removes the need for comments, makes diffs easier to review, and reduces bugs caused by misreading what a variable holds or what a function does.
