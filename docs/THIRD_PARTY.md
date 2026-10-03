# Third-party assets

## Material Symbols

The habit icons (`app/src/main/res/drawable/habit_*.xml`) and the navigation icons
(`ic_arrow_back.xml`) are Google's [Material Symbols](https://github.com/google/material-design-icons),
Rounded style, fetched by `tools/fetch_habit_icons.py`.

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
Modification: the `android:tint` attribute is removed from each file so Compose can tint the icons.

## VS Code and Tokyo Night colours

The VS Code Dark and Tokyo Night themes reuse colour values from VS Code's default dark theme
(Dark+) and the Tokyo Night theme by enkia. Colour values only; no code or assets are copied.
