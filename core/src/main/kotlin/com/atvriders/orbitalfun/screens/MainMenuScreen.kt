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
import com.atvriders.orbitalfun.game.GameMode

/** Title screen with mode selection. */
class MainMenuScreen(private val game: OrbitalGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val sandboxBtn = Rectangle()
    private val survivalBtn = Rectangle()
    private var width = 0f
    private var height = 0f

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val x = screenX.toFloat()
            val y = height - screenY.toFloat()
            when {
                sandboxBtn.contains(x, y) -> game.setScreen(GameScreen(game, GameMode.SANDBOX))
                survivalBtn.contains(x, y) -> game.setScreen(GameScreen(game, GameMode.SURVIVAL))
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
        val bw = 280f
        val bh = 80f
        sandboxBtn.set(this.width / 2f - bw / 2f, this.height * 0.45f, bw, bh)
        survivalBtn.set(this.width / 2f - bw / 2f, this.height * 0.45f - bh - 24f, bw, bh)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.12f, 0.18f, 0.28f, 1f)
        shapes.rect(sandboxBtn.x, sandboxBtn.y, sandboxBtn.width, sandboxBtn.height)
        shapes.rect(survivalBtn.x, survivalBtn.y, survivalBtn.width, survivalBtn.height)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.valueOf("9fefff")
        font.data.setScale(3f)
        center("ORBITAL FUN", height * 0.78f)
        font.data.setScale(1.2f)
        font.color = Color(1f, 1f, 1f, 0.7f)
        center("gravity, slingshots & maneuver nodes", height * 0.78f - 60f)
        font.color = Color.WHITE
        font.data.setScale(1.6f)
        center("SANDBOX", sandboxBtn.y + sandboxBtn.height / 2f + 10f)
        center("SURVIVAL", survivalBtn.y + survivalBtn.height / 2f + 10f)
        font.data.setScale(1f)
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
