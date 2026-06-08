package com.atvriders.orbitalfun.game

import com.badlogic.gdx.Gdx

/** How the player steers the ship. */
enum class ControlScheme(val label: String) {
    /** Discrete prograde/retrograde/radial buttons + throttle — precise orbital control. */
    BUTTONS("Buttons"),

    /** A virtual joystick: push to thrust in that world direction, deflection = throttle. */
    JOYSTICK("Joystick"),
}

/**
 * Persistent player settings, backed by libGDX [com.badlogic.gdx.Preferences] so
 * choices survive app restarts. Must only be used at runtime (needs `Gdx.app`).
 */
object Settings {
    private const val PREFS = "orbital-fun-settings"
    private const val KEY_CONTROLS = "controlScheme"

    var controlScheme: ControlScheme
        get() {
            val name = Gdx.app.getPreferences(PREFS)
                .getString(KEY_CONTROLS, ControlScheme.JOYSTICK.name)
            return runCatching { ControlScheme.valueOf(name) }.getOrDefault(ControlScheme.JOYSTICK)
        }
        set(value) {
            val prefs = Gdx.app.getPreferences(PREFS)
            prefs.putString(KEY_CONTROLS, value.name)
            prefs.flush()
        }
}
