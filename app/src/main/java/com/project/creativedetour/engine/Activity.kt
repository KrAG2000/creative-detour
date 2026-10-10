package com.project.creativedetour.engine

/**
 * The menu of things a nudge can suggest. The app picks the activity (ActivityPicker);
 * Gemma only writes it up. Stored in the database by [name], so never rename an entry.
 */
enum class Activity(
    val label: String,
    /** Concrete examples handed to the model, so it suggests one specific thing. */
    val ideas: String,
    val outdoor: Boolean,
    val gentle: Boolean,
    val minutes: Int,
    /** Used when the model isn't available. */
    val template: String,
) {
    OUTDOOR_WALK(
        "a short walk outside", "a loop around the block, to the nearest tree or park, the long way to fetch something",
        outdoor = true, gentle = true, minutes = 7,
        template = "Step outside for a 5-minute loop around the block. Fresh air beats another scroll.",
    ),
    TOUCH_GRASS(
        "a mindful minute outdoors", "step outside, look up at the sky, notice three sounds, find something green to touch",
        outdoor = true, gentle = true, minutes = 3,
        template = "Go stand outside for two minutes. Look up, find three sounds, touch something green.",
    ),
    STRETCH(
        "a stretch break", "reach for the ceiling, a slow forward fold, a doorway chest stretch, side bends",
        outdoor = false, gentle = true, minutes = 3,
        template = "Stand up, reach for the ceiling, then fold forward. Three slow breaths each.",
    ),
    MOBILITY(
        "a quick mobility reset", "shoulder rolls, neck circles, wrist circles, balancing on one leg for 30 seconds",
        outdoor = false, gentle = true, minutes = 2,
        template = "Roll your shoulders ten times, then balance on one leg for 30 seconds each side.",
    ),
    BODYWEIGHT(
        "a mini workout", "10 squats, 10 wall push-ups, 20 calf raises, a few lunges",
        outdoor = false, gentle = false, minutes = 3,
        template = "Quick set: 10 squats, 10 wall push-ups, 20 calf raises. Done in three minutes.",
    ),
    STAIRS(
        "a stair climb", "walking up and down a flight of stairs a few times",
        outdoor = false, gentle = false, minutes = 3,
        template = "Find a staircase and walk it up and down three times.",
    ),
    DANCE(
        "one song of movement", "putting on one favourite song and dancing, or marching in place",
        outdoor = false, gentle = false, minutes = 4,
        template = "Put on one song you love and move until it ends. Nobody's watching.",
    ),
    ACTIVE_CHORE(
        "an active chore", "watering the plants, tidying one surface, taking the trash out, refilling water the long way",
        outdoor = false, gentle = true, minutes = 5,
        template = "Pick one small chore that gets you up: water a plant or take the trash out.",
    ),
    WALK_AND_TALK(
        "a walk-and-talk", "calling a friend while walking, or walking over to someone instead of texting",
        outdoor = false, gentle = true, minutes = 10,
        template = "Got someone to message? Call them instead and walk while you talk.",
    );

    companion object {
        fun fromName(name: String?) = entries.find { it.name == name }
    }
}
