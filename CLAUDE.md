# Reevz Habitz

## What this project is

Reevz Habitz is a **personal Android habit tracker** for my own phone. Details of the features are
still to come — do not invent them.

It is a personal app — not a multi-user SaaS product. Treat that as a design constraint, not a
temporary phase. Its sibling project, Reevz Mealz (`..\ReevzMealz`), uses the same stack and
conventions and is a good reference for how things are done here.

## Stack

- Kotlin + Jetpack Compose
- Material 3
- Room for local persistence (KSP for its code generation)
- MVVM: `ViewModel` + `StateFlow`, each ViewModel exposing a `Factory` built with
  `viewModelFactory { initializer { ... } }`. No DI framework — DAOs come from
  `HabitDatabase.getInstance(application)`.
- Android-first, local-first / offline-first

## Core principles

- Keep the app simple and fast to use.
- **Do not introduce a backend, authentication, cloud database, analytics, or AI** unless I
  explicitly ask for it.
- Prefer local storage and offline functionality.
- Prefer Android/Jetpack libraries when they are appropriate.
- Keep the architecture understandable rather than over-engineered.
- Do not build abstractions for hypothetical future requirements. Solve the problem in front of you.

## Working rules

- Make small, focused changes. Do not modify unrelated files.
- Preserve existing functionality when implementing new features.
- **Before making an architectural change, explain the reason first.**
- **Before adding a dependency, explain why it is needed** and why an existing library or the
  standard library is not enough.
- Run the appropriate build/tests after significant changes.
- **Never claim a feature works without verifying it.** If verification did not run, or failed, say
  so plainly and show the output.
- Do not silently change project-wide configuration (Gradle files, version catalog,
  `gradle.properties`, manifest, theme). If a config change is genuinely required, call it out and
  explain it.

### Feature workflow

1. Inspect the existing implementation.
2. Explain the proposed approach briefly.
3. Make the smallest reasonable change.
4. Build/test the project.
5. Report what changed and whether verification succeeded.

## Build and verify

Windows / PowerShell (primary shell here):

```
.\gradlew.bat assembleDebug          # compile the app
.\gradlew.bat testDebugUnitTest      # host unit tests
.\gradlew.bat lint                   # Android lint
```

Instrumented tests (`connectedDebugAndroidTest`) need a running emulator or device; don't assume one
is attached.

## Git

- Keep commits small and logically focused.
- Do not commit generated build artifacts, `local.properties`, IDE-specific files, or secrets.
- Do not rewrite Git history unless explicitly requested.
- **Do not push to GitHub unless explicitly requested.**

## UI guidelines

- Design primarily for a **phone**.
- Optimize for quick **one-handed** interaction — common actions reachable with a thumb.
- Prioritize clarity and ease of use over visual complexity.
- Avoid unnecessary screens and navigation. Fewer taps to log a habit is the goal.
- Use Jetpack Compose and Material 3.
- Support **light and dark themes** through `ReevzHabitzTheme`; use colour *roles*, never
  hardcoded colours.
- Use accessible touch targets (48dp minimum) and readable typography.
- Section switching is plain state in `ReevzHabitzApp`, not a navigation library, until sections
  actually grow sub-screens.

## Data guidelines

- Habit data is stored **locally** in Room (`reevz-habitz.db`).
- Once persistent data exists on the phone, schema changes **must** use proper Room migrations. Do
  not rely on `fallbackToDestructiveMigration`.
- Room schemas are exported to `app/schemas/` and **are committed** — they are the baseline every
  future migration is written against. Schema changes mean bumping `@Database(version = ...)` and
  adding a migration.
- Additive schema changes (new table, new nullable column) should use `@AutoMigration`.
  Non-additive changes need a manual `Migration`.
- Room migrations can only be tested with instrumented tests (`MigrationTestHelper`), so they need
  a device. Say plainly when a migration has not been exercised on one.
- **`connectedAndroidTest` uninstalls the app when it finishes, which deletes the database.** Back
  up the phone's database first
  (`adb exec-out run-as com.reevan.reevzhabitz cat databases/reevz-habitz.db`) and restore it after.
- Do not delete or reset user data as part of a normal feature implementation. Treat logged habit
  history as valuable and non-recoverable.

## Current project state

Scaffold only. Single Gradle module `:app`, package `com.reevan.reevzhabitz`. Target device is a
**Nothing Phone (2a)**; `minSdk 24` / `targetSdk 37` covers it.

- `data/HabitDatabase` — Room database, schema v1, containing only `app_settings` (theme mode).
- `ui/settings/PreferencesViewModel` — exposes `AppSettings`; MainActivity reads the theme from it.
- `ui/ReevzHabitzApp` — Scaffold shell with a single placeholder section.
- `ui/theme` — still the template palette with Material You dynamic colour on. The app has no
  visual identity yet.

The schema has never been installed on the phone, so until the first real install v1 can still be
edited freely; after that, every change needs a migration.

## Toolchain notes (non-obvious — read before touching Gradle)

- **AGP 9.3.3**, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01, Room 2.8.4, KSP
  2.2.10-2.0.2.
- **There is no `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself. Only
  `com.android.application`, `org.jetbrains.kotlin.plugin.compose` and `com.google.devtools.ksp`
  are applied. Don't "fix" this by adding the Kotlin Android plugin.
- `compileSdk` uses the AGP 9 block form: `compileSdk { version = release(37) }`.
- Release build type uses `optimization { enable = false }` — the AGP 9 replacement for
  `isMinifyEnabled` / `proguardFiles`. R8 is currently off.
- R8 keep rules live in `app/src/main/keepRules/rules.keep`, not `proguard-rules.pro`.
- Gradle **configuration cache is enabled**. Build logic that isn't configuration-cache-safe will
  fail the build.
- All dependencies and versions go through the version catalog at `gradle/libs.versions.toml` —
  never hardcode a version in `app/build.gradle.kts`.
- **`android.disallowKotlinSourceSets=false` in `gradle.properties` is load-bearing. Do not remove
  it.** KSP registers its generated sources through `kotlin.sourceSets`, which built-in Kotlin
  rejects by default (AGP issue #386221070).
- KSP must track the Kotlin version: Kotlin 2.2.10 pairs with KSP `2.2.10-2.0.2`.
- `compileOptions` targets **Java 17** while the Gradle daemon toolchain is Java 25. That is
  intentional — the daemon JVM is not the bytecode target. No `jvmTarget` / `jvmToolchain` block is
  needed.
