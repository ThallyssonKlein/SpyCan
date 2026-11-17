# Collections and Sequences Rule

Use the right Kotlin collection operation for each task. Never write a manual `for` loop when a declarative operation expresses the same intent more clearly.

## Hard Rules

1. **Use `map` for transformations — never a loop that adds to a new list.**

   ```kotlin
   // ✅ Correct
   val names = users.map { it.name }

   // ❌ Wrong — loop + add is a manual map
   val names = mutableListOf<String>()
   for (user in users) names.add(user.name)
   ```

2. **Use `filter` for exclusions — never add conditionally inside a loop.**

   ```kotlin
   // ✅ Correct
   val active = orders.filter { it.status == OrderStatus.ACTIVE }

   // ❌ Wrong
   val active = mutableListOf<Order>()
   for (order in orders) if (order.status == OrderStatus.ACTIVE) active.add(order)
   ```

3. **Use `firstOrNull` / `first` / `single` to locate an element — never a loop with a flag or early `break`.**
   Resolve absence explicitly with `?:`, `?.let`, or `firstOrNull { ... } ?: throw ...`.

   ```kotlin
   // ✅ Correct
   val target = items.firstOrNull { it.id == id } ?: throw ItemNotFoundException(id)

   // ❌ Wrong
   var target: Item? = null
   for (item in items) { if (item.id == id) { target = item; break } }
   ```

4. **Use `any` / `all` / `none` for boolean checks — never a manual flag variable.**

   ```kotlin
   // ✅ Correct
   val hasActive = orders.any { it.status == OrderStatus.ACTIVE }
   val allConfirmed = orders.all { it.confirmed }
   ```

5. **Use `flatMap` instead of nested loops that flatten.**

   ```kotlin
   // ✅ Correct
   val tags = posts.flatMap { it.tags }

   // ❌ Wrong
   val tags = mutableListOf<Tag>()
   for (post in posts) tags.addAll(post.tags)
   ```

6. **Use `in` / `contains` instead of `indexOf(...) != -1`.**

   ```kotlin
   // ✅ Correct
   if (order.status in allowedStatuses) { ... }
   ```

7. **Use `groupBy`, `associate`, `associateBy`, `partition`, and `fold` before reaching for manual accumulation.**

   ```kotlin
   // ✅ Correct
   val byTenant = orders.groupBy { it.tenantId }
   val labelById = items.associate { it.id to it.label }
   val (paid, unpaid) = orders.partition { it.paid }

   // ❌ Wrong — manual grouping in a loop
   val byTenant = mutableMapOf<String, MutableList<Order>>()
   for (order in orders) byTenant.getOrPut(order.tenantId) { mutableListOf() }.add(order)
   ```

8. **Never mutate the source collection — always produce a new one.**
   `sort()`, `reverse()`, `removeAll`, and `shuffle()` mutate in place. Use `sortedBy`, `reversed`, `filterNot`, and friends.

   ```kotlin
   // ✅ Correct — new sorted list, source untouched
   val sorted = orders.sortedBy { it.createdAt }

   // ❌ Wrong — sorts the caller's list as a side effect
   orders.sortBy { it.createdAt }
   ```

9. **Expose read-only collection types and copy at the boundary.**
   Public properties and return types are `List`, `Set`, `Map` — never `MutableList` / `MutableMap`. Keep mutable state `private` and expose a read-only view or a copy (`toList()`). State exposed to Compose/ViewModel consumers must be an immutable snapshot (`StateFlow<UiState>` holding read-only collections; consider `kotlinx.collections.immutable` for Compose stability).

   ```kotlin
   // ✅ Correct
   private val _tags = mutableListOf<Tag>()
   val tags: List<Tag> get() = _tags.toList()

   // ❌ Wrong — leaks internal mutable state
   val tags: MutableList<Tag> = mutableListOf()
   ```

10. **Never use `forEach` or `onEach` to build a result or mutate outside state.**
    Lambdas in a collection pipeline must be side-effect free. `forEach` is acceptable only for a real side effect (e.g. sending each message).

    ```kotlin
    // ✅ Correct
    return items.map(::transform)

    // ❌ Wrong — mutating outside state from inside the pipeline
    val results = mutableListOf<Result>()
    items.forEach { results.add(transform(it)) }
    ```

11. **Use `asSequence()` for large collections with three or more chained operations.**
    Eager chains allocate an intermediate list per step. For big lists (or lists processed on the main thread during scrolling), switch to a `Sequence` and end with `toList()`.

12. **Chain at most two intermediate operations inline. Extract named intermediate results beyond that.**

    ```kotlin
    // ✅ Correct — two operations
    val activeNames = users.filter { it.active }.map { it.name }

    // ✅ Correct — three steps, intermediates named
    val activeUsers = users.filter { it.active }
    val enriched = activeUsers.map(::enrich)
    val ranked = enriched.sortedBy { it.score }

    // ❌ Wrong — four inline operations obscure what each step produces
    val result = users.filter { it.active }.map(::enrich).sortedBy { it.score }.take(MAX_ITEMS)
    ```

## Why

Declarative operations communicate *what* is being computed, not *how*. Immutability prevents aliasing bugs — especially important when the same list is shared between a ViewModel and Compose recomposition. Capping chain length keeps intermediate types inspectable and failures easier to isolate.
