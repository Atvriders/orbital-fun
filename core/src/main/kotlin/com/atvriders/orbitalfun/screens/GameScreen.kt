package com.atvriders.orbitalfun.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.input.GestureDetector
import com.atvriders.orbitalfun.OrbitalGame
import com.atvriders.orbitalfun.game.GameMode
import com.atvriders.orbitalfun.game.SystemFactory
import com.atvriders.orbitalfun.game.ThrustDirection
import com.atvriders.orbitalfun.game.World
import com.atvriders.orbitalfun.render.GameCamera
import com.atvriders.orbitalfun.render.WorldRenderer
import com.atvriders.orbitalfun.ui.Hud
import com.atvriders.orbitalfun.ui.HudAction

/**
 * The main gameplay screen. Owns the [World] simulation, camera, world renderer
 * and touch [Hud]. Steps the physics at a fixed sub-step, scaled by time-warp,
 * and routes HUD taps to game actions.
 */
class GameScreen(private val game: OrbitalGame, private val mode: GameMode) : ScreenAdapter() {

    private var world: World = SystemFactory.build(mode)
    private lateinit var camera: GameCamera
    private val renderer = WorldRenderer()
    private val hud = Hud()

    // Time speed: 0x (paused), slow-mo, real-time, and fast-forward steps.
    private val speeds = floatArrayOf(0f, 0.5f, 1f, 2f, 4f, 8f)
    private var warpIndex = DEFAULT_SPEED_INDEX
    private val timeWarp: Float get() = speeds[warpIndex]

    private var physicsAccumulator = 0f
    private var predictionTimer = 0f

    // Pinch-zoom state.
    private var pinchBaseZoom = 1f
    private var pinching = false

    private val gestures = GestureDetector(object : GestureDetector.GestureAdapter() {
        override fun pan(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            camera.panByScreen(dx, dy)
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (!pinching) {
                pinching = true
                pinchBaseZoom = camera.zoom
            }
            if (distance > 0f) camera.setZoom(pinchBaseZoom * initialDistance / distance)
            return true
        }

        override fun pinchStop() {
            pinching = false
        }
    })

    private val screenInput: InputProcessor = object : InputProcessor {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val action = hud.touchDown(screenX, screenY)
            if (action != null) {
                handle(action)
                return true // consume so the gesture detector ignores button taps
            }
            return false
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            camera.zoomBy(1f + amountY * 0.12f)
            return true
        }

        override fun keyDown(keycode: Int) = false
        override fun keyUp(keycode: Int) = false
        override fun keyTyped(character: Char) = false
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
        override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false
        override fun mouseMoved(screenX: Int, screenY: Int) = false
    }

    override fun show() {
        camera = GameCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.centerOn(world.ship.position)
        hud.resize(Gdx.graphics.width, Gdx.graphics.height)
        world.updatePrediction()
        Gdx.input.inputProcessor = InputMultiplexer(screenInput, gestures)
    }

    override fun resize(width: Int, height: Int) {
        camera.resize(width.toFloat(), height.toFloat())
        hud.resize(width, height)
    }

    private fun handle(action: HudAction) {
        val ship = world.ship
        when (action) {
            HudAction.PROGRADE -> ship.activeDir = ThrustDirection.PROGRADE
            HudAction.RETROGRADE -> ship.activeDir = ThrustDirection.RETROGRADE
            HudAction.RADIAL_IN -> ship.activeDir = ThrustDirection.RADIAL_IN
            HudAction.RADIAL_OUT -> ship.activeDir = ThrustDirection.RADIAL_OUT
            HudAction.CUT -> ship.activeDir = ThrustDirection.NONE
            HudAction.THROTTLE_UP -> ship.throttle = (ship.throttle + 0.1f).coerceAtMost(1f)
            HudAction.THROTTLE_DOWN -> ship.throttle = (ship.throttle - 0.1f).coerceAtLeast(0f)
            HudAction.SPEED_UP -> warpIndex = (warpIndex + 1).coerceAtMost(speeds.size - 1)
            HudAction.SPEED_DOWN -> warpIndex = (warpIndex - 1).coerceAtLeast(0)
            HudAction.RECENTER -> {
                camera.followShip = true
                camera.centerOn(world.ship.position)
            }
            HudAction.SCAN -> if (mode == GameMode.SURVIVAL) world.performScan()
            HudAction.SHOP -> if (mode == GameMode.SURVIVAL && world.isNearDock()) {
                game.setScreen(ShopScreen(game, this, world))
            }
            HudAction.MANEUVER_TOGGLE -> world.maneuver.active = !world.maneuver.active
            HudAction.PRO_UP -> { world.maneuver.active = true; world.maneuver.prograde += DV_STEP }
            HudAction.PRO_DOWN -> { world.maneuver.active = true; world.maneuver.prograde -= DV_STEP }
            HudAction.RAD_UP -> { world.maneuver.active = true; world.maneuver.radial += DV_STEP }
            HudAction.RAD_DOWN -> { world.maneuver.active = true; world.maneuver.radial -= DV_STEP }
            HudAction.NODE_EARLIER -> world.maneuver.leadTime = (world.maneuver.leadTime - 1f).coerceAtLeast(0.5f)
            HudAction.NODE_LATER -> world.maneuver.leadTime = (world.maneuver.leadTime + 1f).coerceAtMost(MAX_NODE_LEAD)
            HudAction.EXEC -> world.executeManeuver()
            HudAction.CLEAR_NODE -> world.maneuver.clear()
            HudAction.MENU -> game.setScreen(MainMenuScreen(game))
            HudAction.RESET -> reset()
        }
        world.updatePrediction()
    }

    private fun reset() {
        world = SystemFactory.build(mode)
        camera.centerOn(world.ship.position)
        camera.followShip = true
        warpIndex = DEFAULT_SPEED_INDEX
        world.updatePrediction()
    }

    /** Called by ShopScreen so this screen rebinds input when resumed. */
    fun rebindInput() {
        Gdx.input.inputProcessor = InputMultiplexer(screenInput, gestures)
    }

    override fun render(delta: Float) {
        // Fixed-step physics scaled by time warp, capped to avoid spiral-of-death.
        physicsAccumulator += delta * timeWarp
        var steps = 0
        while (physicsAccumulator >= PHYS_DT && steps < MAX_STEPS_PER_FRAME) {
            world.update(PHYS_DT)
            physicsAccumulator -= PHYS_DT
            steps++
        }
        if (physicsAccumulator > PHYS_DT * MAX_STEPS_PER_FRAME) physicsAccumulator = 0f

        predictionTimer += delta
        if (predictionTimer >= PREDICT_REFRESH) {
            predictionTimer = 0f
            world.updatePrediction()
        }

        camera.update(world.ship.position)

        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderer.render(world, camera)
        hud.render(world, timeWarp, camera.followShip)
    }

    override fun dispose() {
        renderer.dispose()
        hud.dispose()
    }

    companion object {
        const val PHYS_DT = 1f / 120f
        const val MAX_STEPS_PER_FRAME = 240
        const val PREDICT_REFRESH = 0.08f
        const val DV_STEP = 5f
        const val MAX_NODE_LEAD = 18f
        const val DEFAULT_SPEED_INDEX = 2 // 1x in `speeds`
    }
}
