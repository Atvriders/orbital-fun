package com.atvriders.orbitalfun.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.atvriders.orbitalfun.OrbitalGame
import com.atvriders.orbitalfun.game.ControlScheme
import com.atvriders.orbitalfun.game.Settings

/** Settings: choose the control scheme. Persisted via [Settings]. */
class SettingsScreen(private val game: OrbitalGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val controlsBtn = Rectangle()
    private val backBtn = Rectangle()
    private var width = 0f
    private var height = 0f

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val x = screenX.toFloat()
            val y = height - screenY.toFloat()
            when {
                controlsBtn.contains(x, y) -> Settings.controlScheme =
                    if (Settings.controlScheme == ControlScheme.BUTTONS) ControlScheme.JOYSTICK else ControlScheme.BUTTONS
                backBtn.contains(x, y) -> game.setScreen(MainMenuScreen(game))
            }
            return true
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = input
    }

    override fun resize(width: Int, height: Int) {
        this.width = width.toFloat()
        this.height = height.toFloat()
        camera.setToOrtho(false, this.width, this.height)
        camera.update()
        val bw = 420f
        val bh = 80f
        controlsBtn.set(this.width / 2f - bw / 2f, this.height * 0.5f, bw, bh)
        backBtn.set(this.width / 2f - bw / 2f, this.height * 0.5f - bh - 28f, bw, bh)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.12f, 0.18f, 0.28f, 1f)
        shapes.rect(controlsBtn.x, controlsBtn.y, controlsBtn.width, controlsBtn.height)
        shapes.color = Color(0.12f, 0.20f, 0.16f, 1f)
        shapes.rect(backBtn.x, backBtn.y, backBtn.width, backBtn.height)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.valueOf("9fefff")
        font.data.setScale(2.4f)
        center("SETTINGS", height * 0.78f)
        font.data.setScale(1.4f)
        font.color = Color.WHITE
        center("Controls: ${Settings.controlScheme.label}  (tap to switch)", controlsBtn.y + controlsBtn.height / 2f + 10f)
        center("BACK", backBtn.y + backBtn.height / 2f + 10f)
        font.data.setScale(1f)
        font.color = Color(1f, 1f, 1f, 0.6f)
        center(
            if (Settings.controlScheme == ControlScheme.JOYSTICK)
                "Joystick: push to thrust in any direction; deflection = throttle."
            else
                "Buttons: prograde/retrograde/radial thrust + throttle for precise orbits.",
            height * 0.30f,
        )
        batch.end()
    }

    private fun center(text: String, y: Float) {
        layout.setText(font, text)
        font.draw(batch, text, width / 2f - layout.width / 2f, y)
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }
}
