"""Downloads the habit icon set from Google's Material Symbols and writes it as vector drawables.

Run from anywhere:  python tools/fetch_habit_icons.py

Each icon becomes app/src/main/res/drawable/habit_<key>.xml. The style is Rounded, filled
(`fill1`) where Google publishes a filled variant, because the icons sit small and pure white on
a coloured card edge, where solid shapes read better than outlines.

The `android:tint="?attr/colorControlNormal"` attribute is stripped: Compose's Icon applies its
own tint, and a theme-attribute reference would resolve against the Activity's XML theme instead
of the app's Compose theme.

Keys here must match HabitIcons.kt. Keys are stored in the database, so never rename or remove
one that may already be in use — add new keys instead.

Material Symbols are licensed under Apache 2.0 (see docs/THIRD_PARTY.md).
Pure stdlib.
"""
import os
import re
import sys
import urllib.error
import urllib.request

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable")
BASE = "https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"

# (key, Material Symbols name)
ICONS = [
    # Fitness
    ("dumbbell", "fitness_center"),
    ("run", "directions_run"),
    ("walk", "directions_walk"),
    ("steps", "footprint"),
    ("bike", "directions_bike"),
    ("swim", "pool"),
    ("yoga", "self_improvement"),
    ("stretch", "accessibility_new"),
    ("hike", "hiking"),
    ("sprint", "sprint"),
    ("martial_arts", "sports_martial_arts"),
    ("football", "sports_soccer"),
    ("basketball", "sports_basketball"),
    ("tennis", "sports_tennis"),
    ("cricket", "sports_cricket"),
    # Health
    ("water", "water_drop"),
    ("pill", "medication"),
    ("sleep", "bedtime"),
    ("heart", "favorite"),
    ("heartbeat", "ecg_heart"),
    ("weight", "monitor_weight"),
    ("tooth", "dentistry"),
    ("shower", "shower"),
    ("spa", "spa"),
    ("eye", "visibility"),
    ("no_smoking", "smoke_free"),
    ("no_alcohol", "no_drinks"),
    # Food
    ("meal", "restaurant"),
    ("fruit", "nutrition"),
    ("egg", "egg"),
    ("cooking", "skillet"),
    ("coffee", "local_cafe"),
    ("tea", "emoji_food_beverage"),
    ("no_junk_food", "no_food"),
    # Mind
    ("book", "menu_book"),
    ("study", "school"),
    ("journal", "edit_note"),
    ("mind", "psychology"),
    ("idea", "lightbulb"),
    ("language", "translate"),
    ("math", "calculate"),
    ("science", "science"),
    ("code", "code"),
    ("podcast", "podcasts"),
    ("gratitude", "volunteer_activism"),
    ("brain", "neurology"),
    ("chess", "chess"),
    ("puzzle", "extension"),
    ("library", "local_library"),
    ("quiz", "quiz"),
    # Creative
    ("music", "music_note"),
    ("piano", "piano"),
    ("mic", "mic"),
    ("headphones", "headphones"),
    ("brush", "brush"),
    ("palette", "palette"),
    ("camera", "photo_camera"),
    ("movie", "movie"),
    # Productivity
    ("task", "task_alt"),
    ("checklist", "checklist"),
    ("alarm", "alarm"),
    ("timer", "timer"),
    ("calendar", "calendar_month"),
    ("work", "work"),
    ("laptop", "computer"),
    ("email", "mail"),
    ("no_phone", "mobile_off"),
    ("focus", "do_not_disturb_on"),
    ("savings", "savings"),
    ("wallet", "account_balance_wallet"),
    # Home
    ("home", "home"),
    ("clean", "cleaning_services"),
    ("laundry", "local_laundry_service"),
    ("bed", "bed"),
    ("kitchen", "kitchen"),
    ("groceries", "shopping_cart"),
    ("plant", "potted_plant"),
    ("garden", "yard"),
    ("pet", "pets"),
    ("recycle", "recycling"),
    # People
    ("call", "call"),
    ("chat", "chat"),
    ("friends", "group"),
    ("family", "family_restroom"),
    # Other
    ("sun", "sunny"),
    ("sunrise", "wb_twilight"),
    ("star", "star"),
    ("trophy", "emoji_events"),
    ("bolt", "bolt"),
    ("rocket", "rocket_launch"),
    ("flag", "flag"),
    ("target", "target"),
    ("tree", "park"),
    ("forest", "forest"),
    ("travel", "flight"),
    ("car", "directions_car"),
    ("globe", "public"),
    ("smile", "mood"),
]


def fetch(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def main():
    keys = [key for key, _ in ICONS]
    if len(keys) != len(set(keys)):
        sys.exit("Duplicate keys in ICONS")

    missing = []
    for key, symbol in ICONS:
        folder = f"{BASE}/{symbol}/materialsymbolsrounded"
        xml = fetch(f"{folder}/{symbol}_fill1_24px.xml") or fetch(f"{folder}/{symbol}_24px.xml")
        if xml is None:
            missing.append(f"{key} ({symbol})")
            continue
        xml = re.sub(r'\s*android:tint="[^"]*"', "", xml)
        with open(os.path.join(RES, f"habit_{key}.xml"), "w", encoding="utf-8", newline="\n") as out:
            out.write(xml)

    print(f"Wrote {len(ICONS) - len(missing)} icons to {os.path.normpath(RES)}")
    if missing:
        sys.exit("Not found: " + ", ".join(missing))


if __name__ == "__main__":
    main()
