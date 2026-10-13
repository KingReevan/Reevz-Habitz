# Third-party assets

## Material Symbols

The habit icons (`app/src/main/res/drawable/habit_*.xml`), the navigation icons
(`ic_arrow_back.xml`) and the Timer's icons (`ic_timer`, `ic_play`, `ic_pause`, `ic_stop`,
`ic_backspace`) are Google's [Material Symbols](https://github.com/google/material-design-icons),
Rounded style. The habit icons are fetched by `tools/fetch_habit_icons.py`; the others were
fetched once by hand from the same source.

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
Modification: the `android:tint` attribute is removed from each file so Compose can tint the icons.

## VS Code and Tokyo Night colours

The VS Code Dark and Tokyo Night themes reuse colour values from VS Code's default dark theme
(Dark+) and the Tokyo Night theme by enkia. Colour values only; no code or assets are copied.
