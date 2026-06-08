package com.atvriders.orbitalfun.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.game.ControlScheme
import com.atvriders.orbitalfun.game.GameMode
import com.atvriders.orbitalfun.game.ThrustDirection
import com.atvriders.orbitalfun.game.World

/** Actions the HUD can emit when a button is tapped. */
enum class HudAction {
    PROGRADE, RETROGRADE, RADIAL_IN, RADIAL_OUT, CUT,
    THROTTLE_UP, THROTTLE_DOWN,
    SPEED_DOWN, SPEED_UP, RECENTER,
    SCAN, SHOP,
    MANEUVER_TOGGLE, PRO_UP, PRO_DOWN, RAD_UP, RAD_DOWN,
    NODE_EARLIER, NODE_LATER, EXEC, CLEAR_NODE,
    MENU, RESET,
}

private class Button(val action: HudAction, val label: String) {
    val rect = Rectangle()
}

/**
 * Touch-first heads-up display drawn in screen space. Renders readouts and a set
 * of rectangular buttons, and hit-tests taps. The same code works with a mouse
 * on desktop.
 */
class Hud {

    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private var width = 0f
    private var height = 0f

    private val buttons = ArrayList<Button>()

    // Layout constants.
    private val bw = 92f   // button width
    private val bh = 60f   // button height
    private val pad = 10f
    private val sideMargin = 36f
    private val topMargin = 28f

    // Where to draw the current-speed label (between the << / >> buttons).
    private var speedLabelX = 0f
    private var speedLabelY = 0f

    // Camera follow state, passed in each frame for the CENTER button highlight.
    private var following = true

    /** Active control scheme; set by the screen before resize/render. */
    var controlScheme: ControlScheme = ControlScheme.BUTTONS

    // Virtual joystick geometry/state (screen y-up space, matching the HUD camera).
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private val joyRadius = 120f
    private val joyKnobRadius = 46f
    private val joyKnob = Vector2()   // knob offset from center, clamped to joyRadius
    private var joyActive = false
    private val tmpStick = Vector2()

    fun resize(width: Int, height: Int) {
        this.width = width.toFloat()
        this.height = height.toFloat()
        camera.setToOrtho(false, this.width, this.height)
        camera.update()
        layoutButtons()
    }

    private fun layoutButtons() {
        buttons.clear()

        val col = bw + pad
        val row = bh + pad
        // Raise both thumb pads off the bottom edge so they sit under the thumbs,
        // not jammed into the corners.
        val baseY = height * 0.12f

        // --- Left thumb area ---
        if (controlScheme == ControlScheme.BUTTONS) {
            // Thrust D-pad + throttle, lower-left, raised/inset.
            val lLeft = sideMargin
            val lMid = sideMargin + col
            val lRight = sideMargin + 2f * col
            // Bottom row: THR-  RETRO  THR+
            add(HudAction.THROTTLE_DOWN, "THR -", lLeft, baseY)
            add(HudAction.RETROGRADE, "RETRO", lMid, baseY)
            add(HudAction.THROTTLE_UP, "THR +", lRight, baseY)
            // Middle row: RAD IN  CUT  RAD OUT
            add(HudAction.RADIAL_IN, "RAD IN", lLeft, baseY + row)
            add(HudAction.CUT, "CUT", lMid, baseY + row)
            add(HudAction.RADIAL_OUT, "RAD OUT", lRight, baseY + row)
            // Top: PRO+ (above CUT)
            add(HudAction.PROGRADE, "PRO+", lMid, baseY + 2f * row)
        } else {
            // Virtual joystick occupies the lower-left thumb area instead.
            joyCenterX = sideMargin + joyRadius
            joyCenterY = baseY + joyRadius
            joyKnob.set(0f, 0f)
        }

        // --- Right thumb pad: maneuver-node planner, lower-right, raised/inset. ---
        val rRight = width - sideMargin - bw
        val rMid = rRight - col
        val rLeft = rRight - 2f * col
        add(HudAction.MANEUVER_TOGGLE, "NODE", rLeft, baseY + 2f * row)
        add(HudAction.PRO_UP, "PG +", rMid, baseY + 2f * row)
        add(HudAction.PRO_DOWN, "PG -", rRight, baseY + 2f * row)
        add(HudAction.NODE_EARLIER, "T -", rLeft, baseY + row)
        add(HudAction.RAD_UP, "RD +", rMid, baseY + row)
        add(HudAction.RAD_DOWN, "RD -", rRight, baseY + row)
        add(HudAction.NODE_LATER, "T +", rLeft, baseY)
        add(HudAction.EXEC, "BURN", rMid, baseY)
        add(HudAction.CLEAR_NODE, "CLR", rRight, baseY)

        // --- Top utility row, inset and right-aligned (clear of top-left readouts).
        // Visual order L->R: CENTER  <<  [speed]  >>  SCAN  SHOP  RESET  MENU
        val topY = height - topMargin - bh
        val xMenu = width - sideMargin - bw
        val xReset = xMenu - col
        val xShop = xReset - col
        val xScan = xShop - col
        val xSpeedUp = xScan - col
        val xSpeedLabel = xSpeedUp - col
        val xSpeedDown = xSpeedLabel - col
        val xCenter = xSpeedDown - col
        add(HudAction.MENU, "MENU", xMenu, topY)
        add(HudAction.RESET, "RESET", xReset, topY)
        add(HudAction.SHOP, "SHOP", xShop, topY)
        add(HudAction.SCAN, "SCAN", xScan, topY)
        add(HudAction.SPEED_UP, ">>", xSpeedUp, topY)
        add(HudAction.SPEED_DOWN, "<<", xSpeedDown, topY)
        add(HudAction.RECENTER, "CENTER", xCenter, topY)
        speedLabelX = xSpeedLabel + bw / 2f
        speedLabelY = topY + bh / 2f
    }

    private fun add(action: HudAction, label: String, x: Float, y: Float) {
        val b = Button(action, label)
        b.rect.set(x, y, bw, bh)
        buttons.add(b)
    }

    /** Hit-test a touch (screen coords, origin top-left). Returns the action or null. */
    fun touchDown(screenX: Int, screenY: Int): HudAction? {
        val x = screenX.toFloat()
        val y = height - screenY.toFloat() // flip to y-up
        for (b in buttons) if (b.rect.contains(x, y)) return b.action
        return null
    }

    /** True if a touch (screen coords, origin top-left) lands on the joystick area. */
    fun joystickContains(screenX: Int, screenY: Int): Boolean {
        if (controlScheme != ControlScheme.JOYSTICK) return false
        val x = screenX.toFloat()
        val y = height - screenY.toFloat()
        val touchR = joyRadius * 1.35f // generous grab area
        return Vector2.dst(x, y, joyCenterX, joyCenterY) <= touchR
    }

    /**
     * Move the joystick knob toward a touch (screen coords) and return the stick
     * vector in world orientation (y up), length 0..1.
     */
    fun setJoystick(screenX: Int, screenY: Int): Vector2 {
        val x = screenX.toFloat()
        val y = height - screenY.toFloat()
        joyKnob.set(x - joyCenterX, y - joyCenterY)
        if (joyKnob.len() > joyRadius) joyKnob.setLength(joyRadius)
        joyActive = true
        return tmpStick.set(joyKnob).scl(1f / joyRadius)
    }

    /** Release the joystick: knob springs back to center, stick goes to zero. */
    fun releaseJoystick() {
        joyKnob.set(0f, 0f)
        joyActive = false
    }

    fun render(world: World, timeWarp: Float, following: Boolean) {
        this.following = following
        // 1) Button backgrounds.
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (b in buttons) {
            if (!visible(b.action, world)) continue
            val highlighted = isHighlighted(b.action, world)
            shapes.color = if (highlighted) HILITE else PANEL
            shapes.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height)
        }
        if (controlScheme == ControlScheme.JOYSTICK) {
            shapes.color = if (joyActive) HILITE else PANEL
            shapes.circle(joyCenterX + joyKnob.x, joyCenterY + joyKnob.y, joyKnobRadius, 24)
        }
        shapes.end()

        // 2) Button borders.
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = BORDER
        for (b in buttons) {
            if (!visible(b.action, world)) continue
            shapes.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height)
        }
        if (controlScheme == ControlScheme.JOYSTICK) {
            shapes.color = BORDER
            shapes.circle(joyCenterX, joyCenterY, joyRadius, 48)
        }
        shapes.end()

        // 3) Text: labels + readouts.
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.WHITE
        for (b in buttons) {
            if (!visible(b.action, world)) continue
            layout.setText(font, b.label)
            font.draw(
                batch, b.label,
                b.rect.x + (b.rect.width - layout.width) / 2f,
                b.rect.y + (b.rect.height + layout.height) / 2f,
            )
        }
        // Current time-speed, centered between the << / >> buttons.
        val speed = speedText(timeWarp)
        layout.setText(font, speed)
        font.color = if (timeWarp == 0f) Color.valueOf("ffd166") else Color.WHITE
        font.draw(batch, speed, speedLabelX - layout.width / 2f, speedLabelY + layout.height / 2f)
        drawReadouts(world, timeWarp)
        batch.end()
    }

    private fun speedText(timeWarp: Float): String =
        if (timeWarp == 0f) "Paused" else "${fmt(timeWarp)}x"

    private fun drawReadouts(world: World, timeWarp: Float) {
        val ship = world.ship
        val dominant = world.dominantBody()
        val elements = world.orbitElements(dominant)
        val altitude = (ship.position.dst(dominant.position) - dominant.radius)

        val sb = StringBuilder()
        sb.append("Speed: ").append(fmt(ship.velocity.len())).append("  ")
        sb.append("Alt(").append(dominant.name).append("): ").append(fmt(altitude)).append('\n')
        if (elements.bound) {
            sb.append("Apo: ").append(fmt(elements.apoapsis))
                .append("  Peri: ").append(fmt(elements.periapsis))
                .append("  e: ").append(fmt(elements.eccentricity)).append('\n')
        } else {
            sb.append("Trajectory: ESCAPE (hyperbolic)\n")
        }
        val throttlePct = if (controlScheme == ControlScheme.JOYSTICK) {
            (ship.stick.len().coerceAtMost(1f) * 100).toInt()
        } else {
            (ship.throttle * 100).toInt()
        }
        sb.append("Throttle: ").append(throttlePct).append("%  ")
        sb.append("Speed: ").append(speedText(timeWarp)).append("  ")
        sb.append("Fuel used: ").append(fmt(ship.fuelUsed)).append('\n')

        if (world.mode == GameMode.SURVIVAL) {
            sb.append("Fuel: ").append(fmt(ship.fuel)).append(" / ").append(fmt(world.upgrades.fuelCapacity))
            sb.append("   $").append(world.economy.money)
            sb.append("   Maps held: $").append(world.scanner.unsoldValue)
            if (world.isNearDock()) sb.append("  [DOCKED]")
            sb.append('\n')
        }
        if (world.maneuver.active) {
            sb.append("Node  PG: ").append(fmt(world.maneuver.prograde))
                .append("  RD: ").append(fmt(world.maneuver.radial))
                .append("  in ").append(fmt(world.maneuver.leadTime)).append("s")
                .append("  dV: ").append(fmt(world.maneuver.totalDeltaV))
        }

        font.color = Color.valueOf("d8f0ff")
        font.draw(batch, sb.toString(), pad, height - pad)

        if (world.isStranded()) {
            font.color = Color.valueOf("ff6b6b")
            val msg = "OUT OF FUEL - stranded. RESET or coast to a dock."
            layout.setText(font, msg)
            font.draw(batch, msg, (width - layout.width) / 2f, height * 0.5f)
        }
    }

    private fun visible(action: HudAction, world: World): Boolean = when (action) {
        HudAction.SCAN, HudAction.SHOP -> world.mode == GameMode.SURVIVAL
        else -> true
    }

    private fun isHighlighted(action: HudAction, world: World): Boolean {
        val dir = world.ship.activeDir
        return when (action) {
            HudAction.PROGRADE -> dir == ThrustDirection.PROGRADE
            HudAction.RETROGRADE -> dir == ThrustDirection.RETROGRADE
            HudAction.RADIAL_IN -> dir == ThrustDirection.RADIAL_IN
            HudAction.RADIAL_OUT -> dir == ThrustDirection.RADIAL_OUT
            HudAction.MANEUVER_TOGGLE -> world.maneuver.active
            HudAction.RECENTER -> following
            HudAction.SHOP -> world.mode == GameMode.SURVIVAL && world.isNearDock()
            else -> false
        }
    }

    private fun fmt(v: Float): String = String.format("%.1f", v)

    fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    companion object {
        private val PANEL = Color(0.10f, 0.14f, 0.20f, 0.72f)
        private val HILITE = Color(0.20f, 0.45f, 0.65f, 0.85f)
        private val BORDER = Color(0.5f, 0.7f, 0.9f, 0.6f)
    }
}
