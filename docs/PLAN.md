# Reevz Habitz — Implementation Plan

Phased plan for building the app described in [`SPEC.md`](SPEC.md). Each phase ends with a
working, buildable app and follows the feature workflow in `CLAUDE.md` (inspect → explain → smallest
change → build/test → report). At the end of every phase, update "Current project state" in
`CLAUDE.md` and tick the phase off here.

| # | Phase | Delivers | Status |
|---|-------|----------|--------|
| 1 | Foundations | Data model, "today" clock, navigation, thin header, Menu | ✅ |
| 2 | Look & feel | 4 themes, habit colours, icon set, habit card, app icon, Settings (theme) | ✅ |
| 3 | Add Habit | Create a habit with name/description/colour/icon/start date | ✅ |
| 4 | Home | Today's habits, tick/untick, sort toggle, midnight rollover — **first install on phone** | ✅ (installed on the phone 2026-10-03) |
| 5 | Remove Habit | Multi-select delete with keep-stats toggle; Settings "Clear deleted stats" | ☐ |
| 6 | Edit Habit | List + editor with discard-changes guard | ☐ |
| 7 | Statistics | Habit list (active + deleted), month calendar, stats accordion | ☐ |
| 8 | Polish | Empty states, theme pass, lint, on-device verification | ☐ |

---

## Decisions needed

### Spec questions

Each is tagged with the phase that needs the answer; it doesn't block earlier phases.

| # | Question | Needed by | Answer |
|---|----------|-----------|--------|
| Q1 | Stats strip says "four statistics" but lists three. Is one missing? | 7 | |
| Q2 | Days before the habit existed are red. Do they count in X/Y and break streaks? Is "created" the creation day or the Start From date? | 7 | |
| Q3 | For a deleted habit, what colour are the days after deletion? | 7 | |
| Q4 | Newest/Oldest sort: by creation time or Start From? Is the sort choice remembered? | 4 | **Creation time; remembered across restarts.** Only the checkbox ticks a habit (not the whole card). |
| Q5 | Do habits whose Start From is in the future appear in Remove / Edit / Statistics before they start? | 5 | |
| Q6 | The spec lists 4 themes. Drop the "follow system" option? | 2 | **Yes** — only Light, Dark, VS Code Dark, Tokyo Night. |
| Q7 | "Every single day" = no weekday-only schedules? | 1 | **Yes** — every habit is due every day once started. |
| Q8 | Current streak while today is still unticked: show yesterday's streak until the day ends (Duolingo-style), or 0? | 7 | |
| Q9 | Add Habit: which fields are required? | 3 | **Every field is required, nothing preselected** (Start From still defaults to tomorrow). |
| Q10 | Headers on Add/Remove/Edit/Statistics/Settings: back arrow + breadcrumb like Menu (e.g. `Home > Menu > Add Habit`)? | 1 | **Yes** — every screen after Menu. |

### Technical decisions

All four accepted as recommended. The phone runs Android 15 (API 35). For T3, include a wide, varied set of
colours and icons.

| # | Decision | Recommendation | Why |
|---|----------|----------------|-----|
| T1 | Date handling | **Raise `minSdk` 24 → 26** and use `java.time` (`LocalDate`) | Streaks, calendars and "today" are all date maths; `java.time` makes it simple and testable. It needs API 26. The Nothing Phone (2a) runs Android 14+, so nothing is lost. Alternative: keep 24 and add core-library desugaring (a dependency + build config). |
| T2 | Navigation | **Hand-rolled back stack** (sealed `Destination` list in `ReevzHabitzApp`, `BackHandler`, saveable) | Max depth is 4 (Home › Menu › Edit › editor) with one argument (habit id). A ~40-line stack beats adding Navigation Compose + kotlinx-serialization. Revisit if it grows. |
| T3 | Habit icons | **~40 vector drawables** in `res/drawable/habit_*.xml`, sourced from Google's Material Symbols (Apache 2.0) | Same approach as Reevz Mealz's `ic_*.xml`. `material-icons-extended` is no longer maintained and adds thousands of icons we won't use. |
| T4 | Storing colour & icon | **Stable string keys** (`"teal"`, `"dumbbell"`), not ARGB ints or resource IDs | Resource IDs change between builds. Keys let each colour have light/dark variants, so a habit name stays readable as text in every theme. |

---

## Data model (designed once in Phase 1)

The schema has to cover deletion and statistics from day one, because after the first install on
the phone (end of Phase 4) every change needs a Room migration. v1 has never been installed, so
it is rewritten in place.

```
habits
  id           INTEGER PK autoincrement
  name         TEXT      -- title-cased on entry
  description  TEXT
  colorKey     TEXT      -- key into HabitColors
  iconKey      TEXT      -- key into HabitIcons
  startDate    INTEGER   -- LocalDate epoch day; first day it shows on Home
  createdAt    INTEGER   -- epoch millis; sort tiebreaks / "newest"
  deletedOn    INTEGER?  -- epoch day; null = active. Set = deleted but stats kept

completions                -- one row = "habit done on that day"
  habitId      INTEGER FK → habits.id ON DELETE CASCADE
  date         INTEGER   -- epoch day
  PRIMARY KEY (habitId, date)

app_settings               -- single row
  themeMode    TEXT      -- LIGHT / DARK / VSCODE_DARK / TOKYO_NIGHT
  homeSort     TEXT      -- ALPHABETICAL / NEWEST_FIRST / OLDEST_FIRST (if Q4 = remembered)
```

- **No daily reset job.** Completion is stored per date, so a new day has no rows and every habit
  is automatically unticked. "Reset at midnight" is just the UI's notion of *today* moving on.
- **Remove, keep stats** → set `deletedOn`. **Remove, don't keep** → delete the row; completions
  cascade.
- **Clear deleted stats** → delete every habit with `deletedOn` set; completions cascade.

---

## Phase 1 — Foundations

No user-visible features beyond the Menu. Sets up everything later phases build on.

- **minSdk 26** (T1) and a small `util/Dates.kt` (formatting such as `03 October`, epoch-day
  helpers).
- **Schema v1 rewrite**: `Habit`, `Completion`, updated `AppSettings`; `HabitDao`,
  `CompletionDao`; type converters for `LocalDate`. Regenerate `app/schemas/.../1.json`.
- **Today clock**: a `StateFlow<LocalDate>` that ticks over at local midnight and re-checks
  whenever the app comes back to the foreground (covers the phone sleeping through midnight and
  time-zone changes).
- **Navigation** (T2): `Destination` sealed type (`Home`, `Menu`, `AddHabit`, `RemoveHabit`,
  `EditList`, `EditHabit(id)`, `StatsList`, `HabitStats(id)`, `Settings`), back stack, system back.
- **`AppHeader`**: one thin header composable with two layouts: *date + actions* (Home) and
  *back arrow + breadcrumb* (everything else).
- **Menu screen**: video-game main menu, five stacked buttons with gaps, each opening a
  placeholder for its screen.
- **Tests**: back stack behaviour, date helpers.

## Phase 2 — Look & feel

Built before the screens so every screen is built against the final look.

- **Four themes**: Light, Dark, VS Code Dark (Dark+ greys `#1E1E1E`/`#252526`, accent
  `#007ACC`), Tokyo Night (`#1A1B26` background, `#7AA2F7` / `#BB9AF7` accents). **Material You
  dynamic colour turned off**: named palettes and wallpaper colours can't both be in charge.
- **Habit colours**: ~18 named colours, each with a variant readable in light and in dark themes.
- **Status colours** (green done, red missed, grey future, yellow today ring) as theme-aware
  helpers, used by Statistics.
- **Icon set** (T3): ~40 habit icons, keyed.
- **`HabitCard`**: shared by Home, Remove, Edit and Statistics. Coloured left edge with a pure-white
  icon, name in the habit colour that wraps without touching the trailing slot, optional
  description, optional checkbox / trailing content. Previews in every theme.
- **App icon**: raised fist (victory), as an adaptive icon with a monochrome layer for themed
  icons. Generated by a script in `tools/`, as Reevz Mealz does.
- **Settings screen**: theme picker only.
- **System bars follow the app theme** (found in Phase 1): `enableEdgeToEdge()` picks status-bar
  icon colours from the *phone's* light/dark setting, so dark icons land on the app's dark header.
  Pass a `SystemBarStyle` derived from the selected theme. Also give `Theme.ReevzHabitz` a window
  background matching the theme, so a cold start doesn't flash white.

## Phase 3 — Add Habit

- Form: **Name** (each word capitalised as you type), **Description**, **Colour** grid, **Icon**
  grid, **Start From** (Material 3 `DatePicker`, defaults to tomorrow, past dates disabled).
- **Create** at the bottom right saves the habit and returns to **Menu**.
- Validation per Q9.
- **Tests**: title-casing (cursor position, multiple spaces), selectable-dates rule.

## Phase 4 — Home (the core loop)

- Header: date on the left (`03 October`), sort and menu icons on the right.
- Today's habits = `startDate ≤ today` and not deleted.
- Card stack with no gaps and thin grey dividers. Ticking inserts today's completion: the name is
  crossed out and the card animates to the bottom. Tapping a ticked box asks "Mark as not done?";
  Confirm removes the completion and the card rejoins the undone group.
- **Sort toggle** cycles Alphabetical → Newest→Oldest → Oldest→Newest → Alphabetical, applied
  within the undone and done groups (undone always on top). Persistence per Q4.
- Midnight rollover via the today clock: the header date changes and every card unticks.
- **Tests**: ordering (group + sort), sort cycle.
- 🚩 **Milestone — first install on the Nothing Phone (2a).** From here the schema is frozen: any
  change is a version bump plus a migration, and the phone's database is backed up before
  instrumented tests.

## Phase 5 — Remove Habit

- Same cards as Home, plus descriptions, with multi-select checkboxes.
- **Remove Habit(s)** at the bottom right (enabled once something is selected) opens a dialog with
  a **Keep stats** switch (on by default), then soft or hard delete.
- **Settings → Clear deleted stats**: warning dialog showing how many deleted habits will be
  purged, then a hard delete. These two are the only places the app deletes user data.

## Phase 6 — Edit Habit

- List of habit cards with descriptions, no checkboxes. Tapping one opens the editor.
- Editor: Name, Description, Colour, Icon (the Add Habit form components without Start From).
- **Save** at the bottom right returns to the list.
- Back (header arrow *or* system back gesture) with unsaved changes asks to discard them. With no
  changes it just goes back.

## Phase 7 — Statistics

- List: active habits, then a separate **Deleted** section.
- Detail:
  - Month calendar with month **and year** in its header, previous/next month only.
  - Day circles: green done, red missed, grey future, red before the habit existed (Q2), yellow
    ring on today, after-deletion days per Q3.
  - Opens on the current month, or the deletion month for a deleted habit.
- **Stats strip** under the header, collapsed by default: `done/due` days, current streak,
  longest streak (+ a 4th per Q1, current-streak rule per Q8).
- All the maths lives in a pure-Kotlin `HabitStats` calculator plus a `dayStatus()` function, with
  thorough unit tests: month boundaries, leap years, a streak ending today vs yesterday,
  deleted habits, habits starting in the future.

## Phase 8 — Polish & hardening

- Empty states (no habits yet, nothing due today, no deleted habits).
- Check every screen in all four themes; long-name wrapping; 48dp touch targets.
- Lint clean-up (template colours and PNG mipmaps already removed in Phase 2).
- Full on-device pass on the phone, with a database backup taken first.
