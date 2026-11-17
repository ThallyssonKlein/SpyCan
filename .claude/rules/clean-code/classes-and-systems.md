# Classes and Systems Rule

A class must have one reason to change. A system is healthy when each of its classes is cohesive, minimal, and expressive.

## Hard Rules

1. **Apply the Single Responsibility Principle (SRP).**
   A class must have exactly one reason to change. A screen that handles UI rendering, business rules, *and* networking has three reasons to change — split it into a composable/screen, a ViewModel, and a repository.

   ```kotlin
   // ✅ Correct — each class owns one responsibility
   @HiltViewModel
   class OrderViewModel @Inject constructor(
       private val submitOrder: SubmitOrderUseCase,
   ) : ViewModel() {
       fun onSubmitClicked(order: Order) {
           viewModelScope.launch { submitOrder(order) }
       }
   }

   class SubmitOrderUseCase(private val repository: OrderRepository) {
       suspend operator fun invoke(order: Order) = repository.submit(order)
   }

   // ❌ Wrong — an Activity validating, calling the API, and writing to the database
   class OrderActivity : AppCompatActivity() {
       fun submit() {
           if (name.text.isEmpty()) return
           val response = retrofit.create(OrderApi::class.java).submit(...).execute()
           database.orderDao().insert(response.body()!!)
       }
   }
   ```

2. **Keep classes cohesive.**
   Most functions in a class should use most of its properties. If a subset of functions only touches a subset of properties, they belong in a separate class.

   ```kotlin
   // ✅ Correct — every function uses the single injected dependency
   class SettingRepository(private val api: SettingApi) {
       suspend fun getAll(): List<Setting> = api.fetchAll()
       suspend fun getById(id: String): Setting = api.fetchById(id) ?: throw SettingNotFoundException(id)
       suspend fun create(data: SettingBody) = api.create(data)
   }

   // ❌ Wrong — two unrelated groups of functions share one class
   class SettingAndCacheRepository(private val api: SettingApi, private val cache: CacheClient) {
       suspend fun getAll() = api.fetchAll()   // never touches cache
       fun flushCache() = cache.flush()        // never touches api
   }
   ```

3. **Depend on abstractions and inject dependencies — never construct a collaborator inside business code.**
   Receive dependencies through the constructor (Hilt, Koin, or manual DI) so tests can replace them. Never reach for singletons, `object` service locators, or `Context.applicationContext` from domain code.

   ```kotlin
   // ✅ Correct
   class InvoiceService(private val taxCalculator: TaxCalculator)

   // ❌ Wrong — hard-wired collaborator
   class InvoiceService {
       private val taxCalculator = BrazilTaxCalculator()
   }
   ```

4. **Layer the app and keep dependencies pointing inward.**
   `UI (Compose/Fragment) → ViewModel → UseCase → Repository interface (domain) ← Repository implementation (data)`. The domain layer is plain Kotlin: no `android.*` imports. The UI layer never touches `Dao`s or API services directly.

5. **Follow the four rules of Simple Design (Kent Beck — in priority order).**
   A design is simple when it:
   1. Passes all tests.
   2. Contains no duplication.
   3. Expresses the intent of the author.
   4. Minimizes the number of classes and functions.

   Rules 1–3 take priority over rule 4: do not merge classes or collapse abstractions just to reduce count if doing so introduces duplication or obscures intent.

   ```kotlin
   // ✅ Correct — intent expressed through a named private function; no duplication
   class CustomerRepository(private val api: CustomerApi) {
       suspend fun fetchById(id: String): Customer =
           api.findById(id) ?: throw notFound(id)

       suspend fun update(id: String, data: CustomerBody) {
           val customer = fetchById(id)
           api.save(customer.with(data))
       }

       private fun notFound(id: String) = CustomerNotFoundException(id)
   }
   ```

## Why

A class with a single responsibility is easy to name, easy to test, and easy to replace. On mobile, separating UI, state holder, and data layers is what lets state survive configuration changes and process death, and what keeps business logic testable on the JVM without an emulator. The four rules of simple design impose a floor: tests catch regressions, no-duplication enforces a single point of change, expressiveness makes code self-documenting, and minimalism resists speculative abstractions.
