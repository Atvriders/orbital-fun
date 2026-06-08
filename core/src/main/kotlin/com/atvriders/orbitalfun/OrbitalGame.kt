package com.atvriders.orbitalfun

import com.badlogic.gdx.Game
import com.atvriders.orbitalfun.screens.MainMenuScreen

/** Shared application entry point used by both the desktop and Android launchers. */
class OrbitalGame : Game() {
    override fun create() {
        setScreen(MainMenuScreen(this))
    }
}
