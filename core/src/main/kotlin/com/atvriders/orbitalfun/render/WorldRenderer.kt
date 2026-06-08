package com.atvriders.orbitalfun.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.game.GameMode
import com.atvriders.orbitalfun.game.World
import com.atvriders.orbitalfun.physics.OrbitingBody

/**
 * Draws the top-down view: orbit rings, the coast trajectory, the KSP-style
 * maneuver preview, bodies, the ship, scanner range, and body labels. Pure
 * ShapeRenderer + a default BitmapFont, so there are no external art assets.
 */
class WorldRenderer {

    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    private val coastColor = Color(0.45f, 0.85f, 1f, 0.9f)
    private val maneuverColor = Color(1f, 0.5f, 0.95f, 0.95f)
    private val orbitRingColor = Color(1f, 1f, 1f, 0.12f)
    private val scanColor = Color(0.4f, 1f, 0.6f, 0.18f)

    fun render(world: World, camera: GameCamera) {
        val cam = camera.camera
        shapes.projectionMatrix = cam.combined

        // --- Orbit rings (faint) ---
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = orbitRingColor
        for (planet in world.planets) {
            if (planet is OrbitingBody) {
                shapes.circle(world.star.position.x, world.star.position.y, planet.orbitRadius, 96)
            }
        }
        // --- Trajectories ---
        drawPath(world.predictedPath, coastColor)
        if (world.hasManeuverPreview) drawPath(world.maneuverPath, maneuverColor)
        shapes.end()

        // --- Filled bodies + ship + scanner ---
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = world.star.color
        shapes.circle(world.star.position.x, world.star.position.y, world.star.radius, 48)
        for (planet in world.planets) {
            shapes.color = planet.color
            shapes.circle(planet.position.x, planet.position.y, planet.radius, 32)
        }
        drawShip(world)
        shapes.end()

        // Scanner range (survival only) + maneuver node marker, as line overlays.
        shapes.begin(ShapeRenderer.ShapeType.Line)
        if (world.mode == GameMode.SURVIVAL) {
            shapes.color = scanColor
            shapes.circle(world.ship.position.x, world.ship.position.y, world.upgrades.scannerRange, 64)
        }
        if (world.hasManeuverPreview) {
            shapes.color = maneuverColor
            val r = 8f * camera.zoom
            shapes.circle(world.maneuverNode.x, world.maneuverNode.y, r, 24)
        }
        shapes.end()

        drawLabels(world, camera)
    }

    private fun drawPath(path: List<Vector2>, color: Color) {
        if (path.size < 2) return
        shapes.color = color
        for (i in 0 until path.size - 1) {
            // Dotted look: skip every other segment.
            if (i % 2 == 0) {
                val a = path[i]
                val b = path[i + 1]
                shapes.line(a.x, a.y, b.x, b.y)
            }
        }
    }

    private fun drawShip(world: World) {
        val s = world.ship
        val size = 7f
        val a = s.heading
        // Triangle pointing along heading.
        val nx = MathUtils.cos(a)
        val ny = MathUtils.sin(a)
        val tipX = s.position.x + nx * size
        val tipY = s.position.y + ny * size
        val leftX = s.position.x + MathUtils.cos(a + 2.5f) * size
        val leftY = s.position.y + MathUtils.sin(a + 2.5f) * size
        val rightX = s.position.x + MathUtils.cos(a - 2.5f) * size
        val rightY = s.position.y + MathUtils.sin(a - 2.5f) * size
        shapes.color = if (s.thrustAccel.isZero) s.color else Color.valueOf("ffd166")
        shapes.triangle(tipX, tipY, leftX, leftY, rightX, rightY)
    }

    private fun drawLabels(world: World, camera: GameCamera) {
        val cam = camera.camera
        batch.projectionMatrix = cam.combined
        // Keep text roughly constant in screen size regardless of zoom.
        font.data.setScale(camera.zoom * 0.9f)
        batch.begin()
        val showAll = world.mode == GameMode.SANDBOX
        for (planet in world.planets) {
            val known = showAll || planet.name in world.scanner.discovered
            font.color = if (known) Color.WHITE else Color(1f, 1f, 1f, 0.35f)
            val label = if (known) planet.name else "?"
            font.draw(batch, label, planet.position.x + planet.radius + 4f, planet.position.y + planet.radius)
        }
        batch.end()
        font.data.setScale(1f)
    }

    fun resize(width: Int, height: Int) {
        // Nothing cached on the viewport; camera handles it.
    }

    fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }
}
