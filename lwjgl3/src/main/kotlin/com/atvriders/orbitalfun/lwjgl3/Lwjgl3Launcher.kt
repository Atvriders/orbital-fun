package com.atvriders.orbitalfun.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.atvriders.orbitalfun.OrbitalGame

/** Desktop entry point — used to play/test the game without an Android device. */
object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Orbital Fun")
            setWindowedMode(1280, 800)
            useVsync(true)
            setForegroundFPS(60)
        }
        Lwjgl3Application(OrbitalGame(), config)
    }
}
