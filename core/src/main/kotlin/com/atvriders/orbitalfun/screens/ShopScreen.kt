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
import com.atvriders.orbitalfun.game.Upgrades
import com.atvriders.orbitalfun.game.World

/**
 * Dock shop (Survival mode): sell maps, buy fuel, and purchase ship upgrades with
 * money earned from exploration. Opened from the game HUD when near the dock.
 */
class ShopScreen(
    private val game: OrbitalGame,
    private val gameScreen: GameScreen,
    private val world: World,
) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private var width = 0f
    private var height = 0f

    private enum class Act { SELL, BUY_FUEL, UP_THRUST, UP_FUEL, UP_EFF, UP_SCAN, RESUME }
    private class ShopButton(val act: Act) { val rect = Rectangle() }

    private val buttons = Act.values().map { ShopButton(it) }

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val x = screenX.toFloat()
            val y = height - screenY.toFloat()
            val hit = buttons.firstOrNull { it.rect.contains(x, y) } ?: return true
            handle(hit.act)
            return true
        }
    }

    private fun handle(act: Act) {
        val econ = world.economy
        val up = world.upgrades
        when (act) {
            Act.SELL -> econ.sellMaps(world.scanner)
            Act.BUY_FUEL -> econ.buyFuel(25f, world.ship, up.fuelCapacity)
            Act.UP_THRUST -> econ.tryBuyUpgrade(up, Upgrades.Stat.THRUST)
            Act.UP_FUEL -> econ.tryBuyUpgrade(up, Upgrades.Stat.FUEL_CAPACITY)
            Act.UP_EFF -> econ.tryBuyUpgrade(up, Upgrades.Stat.EFFICIENCY)
            Act.UP_SCAN -> econ.tryBuyUpgrade(up, Upgrades.Stat.SCANNER)
            Act.RESUME -> {
                game.setScreen(gameScreen)
                return
            }
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
        val bw = 360f
        val bh = 56f
        val x = this.width / 2f - bw / 2f
        var y = this.height * 0.72f
        for (b in buttons) {
            b.rect.set(x, y, bw, bh)
            y -= bh + 14f
        }
    }

    private fun labelFor(act: Act): String {
        val up = world.upgrades
        return when (act) {
            Act.SELL -> "Sell Maps  (+$${world.scanner.unsoldValue})"
            Act.BUY_FUEL -> "Buy Fuel +25  ($${25 * com.atvriders.orbitalfun.game.Economy.FUEL_PRICE})"
            Act.UP_THRUST -> "Upgrade Thrust  Lv${up.thrustLevel} -> $${up.cost(Upgrades.Stat.THRUST)}"
            Act.UP_FUEL -> "Upgrade Fuel Tank  Lv${up.fuelCapacityLevel} -> $${up.cost(Upgrades.Stat.FUEL_CAPACITY)}"
            Act.UP_EFF -> "Upgrade Efficiency  Lv${up.efficiencyLevel} -> $${up.cost(Upgrades.Stat.EFFICIENCY)}"
            Act.UP_SCAN -> "Upgrade Scanner  Lv${up.scannerLevel} -> $${up.cost(Upgrades.Stat.SCANNER)}"
            Act.RESUME -> "RESUME FLIGHT"
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.04f, 0.06f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (b in buttons) {
            shapes.color = if (b.act == Act.RESUME) Color(0.12f, 0.28f, 0.18f, 1f) else Color(0.12f, 0.18f, 0.28f, 1f)
            shapes.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height)
        }
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.valueOf("9fefff")
        font.data.setScale(2f)
        center("HAVEN STATION", height * 0.86f)
        font.data.setScale(1.1f)
        font.color = Color.WHITE
        center(
            "Money: $${world.economy.money}    Fuel: ${"%.0f".format(world.ship.fuel)}/${"%.0f".format(world.upgrades.fuelCapacity)}",
            height * 0.86f - 44f,
        )
        for (b in buttons) {
            center(labelFor(b.act), b.rect.y + b.rect.height / 2f + 8f)
        }
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
