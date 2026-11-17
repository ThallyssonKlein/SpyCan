# Comments Rule

Code must explain itself. A comment is an admission that the code failed to communicate its intent on its own.

## Hard Rules

1. **Do not comment bad code — clean it instead.**
   If a block requires a comment to be understood, refactor it until the code speaks for itself.

2. **Never write redundant comments or KDoc.**
   Do not restate what the code already says. KDoc that only repeats the function name and signature (`@param id the id`) is noise — delete it.

   ```kotlin
   // ✅ Correct — no comment needed
   val daysSinceLastModification = ChronoUnit.DAYS.between(lastModifiedAt, now)

   // ❌ Wrong — comment repeats the code
   // Gets the days since last modification
   val daysSinceLastModification = ChronoUnit.DAYS.between(lastModifiedAt, now)

   // ❌ Wrong — KDoc restates the signature
   /**
    * Finds a setting by id.
    * @param id the id
    * @return the setting
    */
   suspend fun findById(id: String): Setting?
   ```

3. **Never commit commented-out code.**
   Delete it. Version control preserves history.

4. **Never write historical, changelog, or `@author` / `@since` comments.**
   That belongs in git commits and PR descriptions.

5. **Good comments are allowed when they add information the code cannot.**
   Acceptable uses:
   - **Legal notices** at the top of a file (copyright, license).
   - **Intent explanations** — *why* a non-obvious decision was made, not *what* the code does (OEM quirks, API-level workarounds, backend contract constraints).
   - **Consequence warnings** — side effects or gotchas a caller must know.
   - **KDoc on public API** of a library module, when it documents contract details the signature cannot (units, thread/dispatcher expectations, nullability, thrown exceptions).
   - **TODO notes** — tracked, actionable, and short.

   ```kotlin
   // ✅ Intent — explains a non-obvious constraint
   // Samsung devices below API 29 drop the notification channel if the importance is changed after creation.
   channel.importance = NotificationManager.IMPORTANCE_HIGH

   // ✅ Warning — consequence a caller must know
   // Must be called from the main thread; it touches the WebView.
   fun clearWebCache()

   // ✅ TODO — actionable and short
   // TODO: replace with Paging 3 once the backend supports cursors
   ```

## Why

Every comment is maintenance debt: it must stay in sync with the code it describes, or it becomes a lie. Comments that survive are the ones that capture intent, constraints, or context that no identifier or structure can express alone.
