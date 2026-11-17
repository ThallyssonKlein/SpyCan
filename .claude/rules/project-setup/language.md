# Language Rule

All code written under `src/main`, `src/test`, and `src/androidTest` must be in **English** — no exceptions.

## Scope

Applies to every file in the repository:
- Variable, function, class, object, enum, and package names
- Comments and KDoc
- Log messages (`Timber.i`, `Timber.w`, `Timber.e`)
- Exception messages
- Test names (backtick names and `@DisplayName`)
- Commit messages and PR descriptions

User-facing text does **not** belong in code at all: it lives in `res/values/strings.xml` (and localized `values-xx/strings.xml`). The default resource set is English.

## Hard Rules

1. **All identifiers must be English.**
   No Portuguese (or any other language) names for variables, functions, classes, or packages.

2. **All log and exception messages must be English.**
   ```kotlin
   // ✅ Correct
   Timber.i("[SettingRepository] setting not found")
   throw IllegalArgumentException("Invalid setting id")

   // ❌ Wrong
   Timber.i("[SettingRepository] Configuração não encontrada")
   ```

3. **All comments must be English.**

4. **All test names must be English.**
   ```kotlin
   // ✅ Correct
   @Test
   fun `should return all settings for the given user`() { ... }

   // ❌ Wrong
   @Test
   fun `deve retornar todas as configuracoes do usuario`() { ... }
   ```

5. **Never hardcode user-facing strings in code or Compose.**
   Use `stringResource(R.string.x)` / `getString(R.string.x)`.

## Why

A consistent English codebase is readable by the full team regardless of native language, aligns with industry conventions, and prevents mixed-language identifiers that are harder to search and maintain.
