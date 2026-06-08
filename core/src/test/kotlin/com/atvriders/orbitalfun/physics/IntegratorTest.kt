package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sqrt

class IntegratorTest {

    private val zero = Vector2()

    @BeforeEach
    fun setup() {
        GravitySystem.G = 100f
        GravitySystem.softening = 0.001f
    }

    @Test
    fun `circular orbit keeps near-constant radius over one period`() {
        val mass = 1000f
        val star = Body("Star", mass, 10f, Vector2(0f, 0f))
        val bodies = listOf(star)

        val r = 200f
        val mu = GravitySystem.G * mass
        val v = sqrt(mu / r) // circular speed

        val pos = Vector2(r, 0f)
        val vel = Vector2(0f, v)

        val period = (2.0 * PI * r / v).toFloat()
        val dt = 1f / 240f
        val steps = (period / dt).toInt()

        var minR = Float.MAX_VALUE
        var maxR = 0f
        var t = 0f
        repeat(steps) {
            Integrator.step(pos, vel, zero, bodies, t, dt)
            t += dt
            val rad = pos.len()
            if (rad < minR) minR = rad
            if (rad > maxR) maxR = rad
        }

        // Radius should stay within 1% of the circular radius.
        assertTrue((maxR - minR) / r < 0.01f, "radius drift too large: min=$minR max=$maxR")
        // And the orbit should close: end near the start point.
        assertTrue(pos.dst(Vector2(r, 0f)) / r < 0.02f, "orbit did not close: end=$pos")
    }

    @Test
    fun `prograde thrust raises apoapsis`() {
        val mass = 1000f
        val star = Body("Star", mass, 10f, Vector2(0f, 0f))
        val bodies = listOf(star)
        val r = 200f
        val mu = GravitySystem.G * mass
        val v = sqrt(mu / r)

        val pos = Vector2(r, 0f)
        val vel = Vector2(0f, v)
        val before = OrbitMath.elements(Vector2(pos), Vector2(vel), mass)

        // Burn prograde (along +y) briefly.
        val thrust = Vector2(0f, 60f)
        val dt = 1f / 240f
        var t = 0f
        repeat(120) {
            Integrator.step(pos, vel, thrust, bodies, t, dt)
            t += dt
        }
        val after = OrbitMath.elements(Vector2(pos), Vector2(vel), mass)
        assertTrue(after.apoapsis > before.apoapsis, "apoapsis should rise: ${before.apoapsis} -> ${after.apoapsis}")
    }
}
