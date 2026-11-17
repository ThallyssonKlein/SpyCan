# Early Return Rule

Exit a function as soon as preconditions fail. Never wrap the happy path in a conditional block.

## Hard Rules

1. **Guard at the top, return early, keep the happy path flat.**
   Use `require`, `check`, and `requireNotNull` for argument and state preconditions.

   ```kotlin
   // ✅ Correct — guards up top, happy path flat
   fun publishEvent(params: PublishEventParams) {
       require(params.tenantId.isNotBlank()) { "[EventProducer] tenantId is required" }
       require(params.eventType.isNotBlank()) { "[EventProducer] eventType is required" }

       val hub = resolveHub(params.tenantId)
       hub.publish(params)
   }

   // ❌ Wrong — happy path buried inside nested ifs
   fun publishEvent(params: PublishEventParams) {
       if (params.tenantId.isNotBlank()) {
           if (params.eventType.isNotBlank()) {
               resolveHub(params.tenantId).publish(params)
           }
       }
   }
   ```

2. **Use the Elvis operator (`?:`) with `return` / `throw` to unwrap nullables early.**
   Never nest `if (x != null)` blocks.

   ```kotlin
   // ✅ Correct
   fun fetchById(id: String): Customer {
       val customer = dao.findById(id) ?: throw CustomerNotFoundException(id)
       return customer.toDomain()
   }

   // ❌ Wrong
   fun fetchById(id: String): Customer {
       val customer = dao.findById(id)
       if (customer != null) {
           return customer.toDomain()
       } else {
           throw CustomerNotFoundException(id)
       }
   }
   ```

3. **Never use `else` after a block that throws or returns.**

   ```kotlin
   // ✅ Correct
   fun resolveStatus(code: Int): String {
       if (code == HTTP_NOT_FOUND) return "not_found"
       if (code == HTTP_BAD_REQUEST) return "bad_request"

       return "unknown"
   }

   // ❌ Wrong
   fun resolveStatus(code: Int): String {
       if (code == HTTP_NOT_FOUND) {
           return "not_found"
       } else if (code == HTTP_BAD_REQUEST) {
           return "bad_request"
       } else {
           return "unknown"
       }
   }
   ```

4. **Use `when` as an expression for closed sets of values.**
   For `enum` and `sealed` types, omit the `else` branch so the compiler flags a missing case.

   ```kotlin
   // ✅ Correct — exhaustive, no else
   fun describe(status: OrderStatus): String = when (status) {
       OrderStatus.ACTIVE -> "active"
       OrderStatus.CANCELLED -> "cancelled"
       OrderStatus.SHIPPED -> "shipped"
   }
   ```

5. **In `Unit` functions and lambdas, return early to skip the rest of the body.**
   Use labeled returns (`return@launch`, `return@forEach`) instead of wrapping the rest in an `if`.

   ```kotlin
   // ✅ Correct
   fun onSubmitClicked() {
       if (state.value.isSubmitting) return

       viewModelScope.launch { submitOrder(state.value.order) }
   }

   // ❌ Wrong — whole body wrapped
   fun onSubmitClicked() {
       if (!state.value.isSubmitting) {
           viewModelScope.launch { submitOrder(state.value.order) }
       }
   }
   ```

## Why

Nesting is the enemy of readability. Early returns keep functions linear: a reader scans through a list of exit conditions, then reaches the single happy path — unindented, unambiguous, and easy to follow.
