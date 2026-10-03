package com.reevan.reevzhabitz.ui.theme

import androidx.annotation.DrawableRes
import com.reevan.reevzhabitz.R

/**
 * An icon a habit can be given, shown pure white on the habit card's coloured edge.
 *
 * [key] is what the database stores — never the resource id, which changes between builds. Never
 * rename or remove a key; add new ones instead. The drawables come from Google's Material Symbols
 * via `tools/fetch_habit_icons.py`, which must list the same keys.
 */
data class HabitIcon(
    val key: String,
    /** Spoken by TalkBack, and shown wherever the icon needs a name. */
    val label: String,
    @param:DrawableRes val drawable: Int,
)

object HabitIcons {

    /** In picker order, grouped loosely by theme. */
    val all: List<HabitIcon> = listOf(
        // Fitness
        HabitIcon("dumbbell", "Dumbbell", R.drawable.habit_dumbbell),
        HabitIcon("run", "Run", R.drawable.habit_run),
        HabitIcon("walk", "Walk", R.drawable.habit_walk),
        HabitIcon("steps", "Steps", R.drawable.habit_steps),
        HabitIcon("bike", "Bike", R.drawable.habit_bike),
        HabitIcon("swim", "Swim", R.drawable.habit_swim),
        HabitIcon("yoga", "Yoga", R.drawable.habit_yoga),
        HabitIcon("stretch", "Stretch", R.drawable.habit_stretch),
        HabitIcon("hike", "Hike", R.drawable.habit_hike),
        HabitIcon("sprint", "Sprint", R.drawable.habit_sprint),
        HabitIcon("martial_arts", "Martial arts", R.drawable.habit_martial_arts),
        HabitIcon("football", "Football", R.drawable.habit_football),
        HabitIcon("basketball", "Basketball", R.drawable.habit_basketball),
        HabitIcon("tennis", "Tennis", R.drawable.habit_tennis),
        HabitIcon("cricket", "Cricket", R.drawable.habit_cricket),
        // Health
        HabitIcon("water", "Water", R.drawable.habit_water),
        HabitIcon("pill", "Pill", R.drawable.habit_pill),
        HabitIcon("sleep", "Sleep", R.drawable.habit_sleep),
        HabitIcon("heart", "Heart", R.drawable.habit_heart),
        HabitIcon("heartbeat", "Heartbeat", R.drawable.habit_heartbeat),
        HabitIcon("weight", "Weight", R.drawable.habit_weight),
        HabitIcon("tooth", "Tooth", R.drawable.habit_tooth),
        HabitIcon("shower", "Shower", R.drawable.habit_shower),
        HabitIcon("spa", "Spa", R.drawable.habit_spa),
        HabitIcon("eye", "Eye", R.drawable.habit_eye),
        HabitIcon("no_smoking", "No smoking", R.drawable.habit_no_smoking),
        HabitIcon("no_alcohol", "No alcohol", R.drawable.habit_no_alcohol),
        // Food
        HabitIcon("meal", "Meal", R.drawable.habit_meal),
        HabitIcon("fruit", "Fruit", R.drawable.habit_fruit),
        HabitIcon("egg", "Egg", R.drawable.habit_egg),
        HabitIcon("cooking", "Cooking", R.drawable.habit_cooking),
        HabitIcon("coffee", "Coffee", R.drawable.habit_coffee),
        HabitIcon("tea", "Tea", R.drawable.habit_tea),
        HabitIcon("no_junk_food", "No junk food", R.drawable.habit_no_junk_food),
        // Mind
        HabitIcon("book", "Book", R.drawable.habit_book),
        HabitIcon("study", "Study", R.drawable.habit_study),
        HabitIcon("journal", "Journal", R.drawable.habit_journal),
        HabitIcon("mind", "Mind", R.drawable.habit_mind),
        HabitIcon("idea", "Idea", R.drawable.habit_idea),
        HabitIcon("language", "Language", R.drawable.habit_language),
        HabitIcon("math", "Math", R.drawable.habit_math),
        HabitIcon("science", "Science", R.drawable.habit_science),
        HabitIcon("code", "Code", R.drawable.habit_code),
        HabitIcon("podcast", "Podcast", R.drawable.habit_podcast),
        HabitIcon("gratitude", "Gratitude", R.drawable.habit_gratitude),
        // Creative
        HabitIcon("music", "Music", R.drawable.habit_music),
        HabitIcon("piano", "Piano", R.drawable.habit_piano),
        HabitIcon("mic", "Mic", R.drawable.habit_mic),
        HabitIcon("headphones", "Headphones", R.drawable.habit_headphones),
        HabitIcon("brush", "Brush", R.drawable.habit_brush),
        HabitIcon("palette", "Palette", R.drawable.habit_palette),
        HabitIcon("camera", "Camera", R.drawable.habit_camera),
        HabitIcon("movie", "Movie", R.drawable.habit_movie),
        // Productivity
        HabitIcon("task", "Task", R.drawable.habit_task),
        HabitIcon("checklist", "Checklist", R.drawable.habit_checklist),
        HabitIcon("alarm", "Alarm", R.drawable.habit_alarm),
        HabitIcon("timer", "Timer", R.drawable.habit_timer),
        HabitIcon("calendar", "Calendar", R.drawable.habit_calendar),
        HabitIcon("work", "Work", R.drawable.habit_work),
        HabitIcon("laptop", "Laptop", R.drawable.habit_laptop),
        HabitIcon("email", "Email", R.drawable.habit_email),
        HabitIcon("no_phone", "No phone", R.drawable.habit_no_phone),
        HabitIcon("focus", "Focus", R.drawable.habit_focus),
        HabitIcon("savings", "Savings", R.drawable.habit_savings),
        HabitIcon("wallet", "Wallet", R.drawable.habit_wallet),
        // Home
        HabitIcon("home", "Home", R.drawable.habit_home),
        HabitIcon("clean", "Clean", R.drawable.habit_clean),
        HabitIcon("laundry", "Laundry", R.drawable.habit_laundry),
        HabitIcon("bed", "Bed", R.drawable.habit_bed),
        HabitIcon("kitchen", "Kitchen", R.drawable.habit_kitchen),
        HabitIcon("groceries", "Groceries", R.drawable.habit_groceries),
        HabitIcon("plant", "Plant", R.drawable.habit_plant),
        HabitIcon("garden", "Garden", R.drawable.habit_garden),
        HabitIcon("pet", "Pet", R.drawable.habit_pet),
        HabitIcon("recycle", "Recycle", R.drawable.habit_recycle),
        // People
        HabitIcon("call", "Call", R.drawable.habit_call),
        HabitIcon("chat", "Chat", R.drawable.habit_chat),
        HabitIcon("friends", "Friends", R.drawable.habit_friends),
        HabitIcon("family", "Family", R.drawable.habit_family),
        // Other
        HabitIcon("sun", "Sun", R.drawable.habit_sun),
        HabitIcon("sunrise", "Sunrise", R.drawable.habit_sunrise),
        HabitIcon("star", "Star", R.drawable.habit_star),
        HabitIcon("trophy", "Trophy", R.drawable.habit_trophy),
        HabitIcon("bolt", "Bolt", R.drawable.habit_bolt),
        HabitIcon("rocket", "Rocket", R.drawable.habit_rocket),
        HabitIcon("flag", "Flag", R.drawable.habit_flag),
        HabitIcon("target", "Target", R.drawable.habit_target),
        HabitIcon("tree", "Tree", R.drawable.habit_tree),
        HabitIcon("forest", "Forest", R.drawable.habit_forest),
        HabitIcon("travel", "Travel", R.drawable.habit_travel),
        HabitIcon("car", "Car", R.drawable.habit_car),
        HabitIcon("globe", "Globe", R.drawable.habit_globe),
        HabitIcon("smile", "Smile", R.drawable.habit_smile),
    )

    private val byKey: Map<String, HabitIcon> = all.associateBy { it.key }

    /** The icon for a stored key. An unknown key falls back rather than crashing a screen. */
    fun forKey(key: String): HabitIcon = byKey[key] ?: all.first()
}
