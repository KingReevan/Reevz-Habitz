# Reevz Habitz

## What this project is

Reevz Habitz is a **personal Android habit tracker** for my own phone.

**The product spec is [`docs/SPEC.md`](docs/SPEC.md) — read it before working on any feature.** It
is the source of truth for screens and behaviour. Do not invent features beyond it; where it is
silent or ambiguous, ask. The phased build order, open decisions and data model are in
[`docs/PLAN.md`](docs/PLAN.md); keep its status table and decision answers up to date.

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
is attached. They are:
- `HabitDatabaseTest` — DAO queries against an in-memory database.
- `AppFlowsTest` — end-to-end flows through the real Activity and **the real database, which it
  wipes before every test**: Home ticking/sorting, navigation, Add (validation, capitalisation,
  double tap), Remove (keep / delete stats), Edit (save, discard, double tap), Statistics (sections,
  numbers), Settings (theme, clear deleted stats), and state surviving rotation. Emulator only.

Add a flow test here when adding or changing a user-facing flow.

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
- Navigation is the hand-rolled back stack in `ui/navigation/`, not a navigation library
  (decision T2 in `docs/PLAN.md`).

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
- **No backups, by the owner's choice.** `allowBackup="false"`, and `data_extraction_rules.xml` /
  `backup_rules.xml` exclude every domain from cloud backup *and* device-to-device transfer, so
  habit data never leaves the phone. Don't re-enable either without asking.
- **Editing a habit must never change its statistics.** Edit only rewrites name, description,
  colour and icon via `HabitDao.update` (an in-place SQL UPDATE); id, start date, creation time
  and completions stay put. Never save habits with `OnConflictStrategy.REPLACE` — SQLite does it
  as delete + insert, and the delete cascades through `completions`. Pinned by
  `HabitDatabaseTest.editingAHabit_keepsItsHistoryAndScheduleIntact` and
  `AppFlowsTest.editingAHabit_leavesItsStatisticsExactlyAsTheyWere`.
- Do not delete or reset user data as part of a normal feature implementation. Treat logged habit
  history as valuable and non-recoverable.

## Current project state

All eight phases of `docs/PLAN.md` are done: every screen in `docs/SPEC.md` is built and on the phone. Single Gradle module `:app`, package `com.reevan.reevzhabitz`.
Target device is a **Nothing Phone (2a) on Android 15 (API 35)**, adb serial `00050146M001006`; `minSdk 26` / `targetSdk 37`.

- `data/` — Room schema v1: `habits` (soft delete via `deletedOn`), `completions` (one row per
  habit per day done, cascades on hard delete), `app_settings` (theme). Dates are `LocalDate`
  stored as epoch days (`Converters`). DAOs hold only basic reads/writes; screen-specific queries
  are added by the phase that needs them.
- `util/TodayClock` — the single source of "today". Ticks at local midnight; MainActivity restarts
  it on every return to the foreground. Never call `LocalDate.now()` elsewhere — read
  `TodayClock.instance.today`.
- `ui/navigation/` — hand-rolled back stack (`Destination` sealed class, pure `push`/`pop`, saved
  as route strings). `Destination`'s companion lists must stay `by lazy` (see the comment there).
- `ui/common/AppHeader` — `DateHeader` (Home) and `BreadcrumbHeader` (every other screen).
- `ui/menu/MenuScreen` — the five Menu buttons, styled as a game main menu.
- `ui/settings/SettingsScreen` — theme picker (radio rows with a swatch per theme).
- `ui/common/HabitCard` — the shared habit card (coloured edge + white icon, name in the habit
  colour, optional description, trailing slot) and `HabitCardDivider`. Every list of habits uses it.
- `ui/theme/` — four fixed themes (Light, Dark, VS Code Dark, Tokyo Night; default Dark), dynamic
  colour off. Material roles via `MaterialTheme.colorScheme`; app-only colours (card divider,
  Statistics done/missed/future/today ring) via `HabitzTheme.colors`. `HabitColors` (35) and
  `HabitIcons` (98) map stable string keys — the values stored in `habits.colorKey/iconKey` — to
  colours and drawables. Keys are never renamed or removed; a colour taken out of the picker
  moves to `HabitColors.retired` so habits using it still resolve. No habit colour may look like
  the ticked-habit grey (`doneHabit`) — `ThemeColorsTest` keeps them ΔE ≥ 12 apart in every theme. Each habit colour has a light-theme
  and a dark-theme shade; read it with `habitColor.current`. `ThemeColorsTest` enforces contrast for
  every theme and habit colour — run it after touching any colour.
- Home and Remove tick with `HabitCheckSection` (in `HabitCard.kt`): a full-height, separately
  shaded block at the card's right end, the whole block being the tap target. Use it for any
  card checkbox rather than a bare `Checkbox`.
- Accessibility: Home and Remove check sections carry the habit name as their content description;
  headers and buttons use minimum (not fixed) heights so large system fonts don't clip.
- `MainActivity` holds the first frame until settings load (no flash of the wrong theme) and sets
  system bar icon colours from the app theme, not the phone's.
- `ui/addhabit/` — Add Habit. Form state lives in the screen (`rememberSaveable` /
  `rememberTextFieldState`), not the ViewModel: ViewModels are activity-scoped under the
  hand-rolled back stack, so per-visit state kept in one would reappear on the next visit.
  `AddHabitViewModel` only inserts. Create pops back to Menu.
- `ui/habitform/` — form pieces shared with Phase 6's editor: `HabitNameField`, description field,
  `HabitColorPicker`, `HabitIconPicker`, and `HabitDraft` (validation + `toHabit`).
- **Habit name capitalisation is display-only** (`OutputTransformation`); `HabitDraft.toHabit`
  applies `capitalizeWords` when saving. Don't move it back into onValueChange or an
  InputTransformation — rewriting text the keyboard is composing desyncs it.
- `ui/home/` — Home: today's habits via `HabitDao.observeDueOn(today)` (active, started, with a
  `done` flag for that day), ordered by `orderForHome` (to-do first, done last, each group in the
  chosen `HomeSort`; "newest" = most recently *created*). Only the checkbox ticks; unticking asks
  first. A ticked habit is struck through and its edge, name and checkbox fade to the theme's
  `HabitzTheme.colors.doneHabit` grey (contrast pinned by `ThemeColorsTest`). The sort is remembered in `app_settings.homeSort`; the header icon shows the current one.
  `HomeViewModel` lives in the shell because the header's sort button needs it.
- Finishing the day (spec: Home → "All done for the day"): ticking the last habit pops a gold star
  (`HabitzTheme.colors.star`) with a firmer buzz; a green card (`colors.done`) stays pinned at the
  bottom while `isDayComplete`. Whether a *tick* finished the day is decided by the pure
  `DayCompletionWatcher` (pinned by `DayCompletionTest`) once the tick lands in the list, never at
  tap time — ticks arrive from the database a moment later and two quick taps can land together.
- `app_settings` is written with per-column UPDATEs (`setThemeMode`, `cycleHomeSort`), never by
  rewriting the row, so two screens can't overwrite each other's preference.
- Every Add Habit field is required (name, description, colour, icon); nothing is preselected.
  Start From defaults to tomorrow.
- "Today" also restarts on clock / time zone / date change broadcasts (`util/clockChanges`), because
  the midnight delay counts elapsed time and can't see the wall clock jump.
- `ui/removehabit/` — Remove Habit: every active habit (incl. not yet started) in Home's sort
  (`ui/common/HabitOrder` — shared comparator), multi-select by checkbox, confirm dialog with
  "Keep stats" (on by default). `HabitDao.remove` soft-deletes (`deletedOn = today`) or hard-deletes
  (completions cascade). Returns to Menu.
- Settings → "Clear deleted stats" (`DeletedStatsViewModel`, `HabitDao.purgeDeleted`): hard-deletes
  every soft-deleted habit and its completions after a counted warning. This and Remove without
  stats are the only user-data deletes in the app.
- `ui/edithabit/` — Edit Habit: list (active incl. not started, Home's sort, via the shared
  `ui/common/activeHabitsInHomeOrder`) → editor for name/description/colour/icon only. Save is
  enabled only for a real, complete change (`HabitEdits.changes`, normalised like Add). Back with
  changes asks "Discard changes?". The editor is `key`ed by habit id in the shell.
- **One-shot actions (Create, Save, Remove) use `ui/common/Submission`.** The ViewModel owns a
  `SubmissionTracker` and marks a token done when its write finishes; the screen holds the token
  (`rememberSubmission`, saved across rotation) and leaves when it sees it done. Never pass a
  "done" callback into an activity-scoped ViewModel: after a rotation it lands on a dead
  composition, the screen never closes, and Create could insert twice. Call `rememberSubmission`
  before any early return in the screen.
- Forward navigation is `pushFrom(current, to)`: only the screen on top can open another, so two
  taps in one frame can't stack two screens.
- Remove with "Keep stats" soft-deletes only habits that have started; not-yet-started habits
  have no history and are deleted outright (`HabitDao.remove`).
- **Leaving a screen goes through `ui/navigation/LeaveGuard`.** System back, the header arrow and
  a tapped breadcrumb all call the shell's `leaveTo(target stack)`; the screen on top can hold it
  up with `InterceptLeaving` (the Edit Habit editor, to ask "Discard changes?") and later approve
  it, landing on exactly the tapped target. Screens finishing their own job (Create, Save,
  Remove, Statistics' "habit gone") use `finish(screen)` = `popIfCurrent`, never a blind pop, so
  a breadcrumb tapped mid-save is never overruled. Breadcrumb crumbs before the current one are
  buttons (`BreadcrumbHeader`, tagged `breadcrumbs` for tests); `popTo(index)` jumps to them.
- `ui/habitform/HabitFormLayout` — `HabitFormScaffold` (scrolling form + fixed bottom-right action)
  and `HabitDetailsFields` (name, description, colour, icon), shared by Add and Edit.
- `ui/statistics/` — Statistics list (started active habits, then a "Deleted" section) → one
  habit's screen: a collapsible strip (name; X/Y, current and longest streak) over a month calendar.
  **All rules live in the pure `HabitHistory`** (day colours, numbers, paging range) and are pinned
  by `HabitHistoryTest` — change rules there, not in the UI. In short: due every day from Start
  From; earlier days red but uncounted; today/deletion day count only once ticked (streak survives
  an unticked today); after deletion grey. Opens on the current month (deletion month if deleted);
  pages from the creation month. Day numbers pick black/white by best contrast.
- Testing text input on the emulator: `adb shell input text` with a whole string types faster than
  the emulator keyboard keeps up with in a word-capitalising field and drops letters. Send one
  character per `input text` call to get human-pace typing.

Generated assets — edit the script, not the output:
- `tools/fetch_habit_icons.py` → `res/drawable/habit_*.xml` (Material Symbols, Apache 2.0; see
  `docs/THIRD_PARTY.md`). Add icons here *and* in `HabitIcons.kt`.
- `tools/make_icon.py` → the adaptive launcher icon layers (raised fist). minSdk 26 means no PNG
  mipmaps are needed.

**Schema v1 has been on the phone since 2026-10-03 and is frozen.** Every schema change from now
on is a version bump plus a migration (`@AutoMigration` where additive). Never edit v1 in place.
Before any `connectedAndroidTest` on the phone, back up its database (see Data guidelines) — the
test run uninstalls the app and deletes it. Run instrumented tests on the emulator only: **when
the phone is plugged in, set `ANDROID_SERIAL=emulator-5554` first**, or Gradle runs them on every
connected device, the phone included. Update the phone with `adb -s 00050146M001006 install -r`
(keeps data).

Emulator testing tips: run adb from Git Bash with `MSYS_NO_PATHCONV=1`, or device paths like
`/data/local/tmp` get rewritten to Windows paths. `adb root` + `adb shell date MMDDhhmmYY.ss` sets
the clock (turn `auto_time` back on afterwards).

A local emulator, `habitz_test` (Android 14, Pixel 7 profile), exists for verification while the
phone is not connected. Start it with
`emulator -avd habitz_test -no-window -gpu swiftshader_indirect`; it is slow (software GPU) and
its System UI may show a "not responding" dialog on first boot.

## Toolchain notes (non-obvious — read before touching Gradle)

- **AGP 9.3.3**, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01, Room 2.8.4, KSP
  2.2.10-2.0.2.
- **There is no `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself. Only
  `com.android.application`, `org.jetbrains.kotlin.plugin.compose` and `com.google.devtools.ksp`
  are applied. Don't "fix" this by adding the Kotlin Android plugin.
- `minSdk` is 26 so `java.time` is available without desugaring (decision T1).
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
