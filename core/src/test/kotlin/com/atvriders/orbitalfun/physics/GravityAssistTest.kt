package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Verifies the core promise of the game: a flyby of a *moving* body transfers
 * energy (gravity assist), while a flyby of a static body conserves it. Energy
 * is measured as the ship's "speed at infinity" relative to the planet, which is
 * conserved exactly for a static attractor.
 */
class GravityAssistTest {

    private val zero = Vector2()

    @BeforeEach
    fun setup() {
        GravitySystem.G = 100f
        GravitySystem.softening = 0.001f
    }

    /** Run a fixed flyby and return the ship's speed-at-infinity after it. */
    private fun flybyVInf(planet: Body): Float {
        val bodies = listOf(planet)
        val pos = Vector2(-1500f, 80f)
        val vel = Vector2(40f, 0f)
        val dt = 1f / 120f
        var t = 0f
        repeat(12000) {
            Integrator.step(pos, vel, zero, bodies, t, dt)
            t += dt
        }
        val rEnd = pos.dst(planet.position)
        val mu = GravitySystem.G * planet.mass
        val vInf2 = vel.len2() - 2f * mu / rEnd
        return sqrt(maxOf(0f, vInf2))
    }

    @Test
    fun `moving planet adds or removes energy depending on flyby direction`() {
        val mass = 2000f

        // Static planet at the origin: energy must be conserved.
        val staticPlanet = Body("Static", mass, 5f, Vector2(0f, 0f))
        val vInfStatic = flybyVInf(staticPlanet)

        // Sanity: static baseline matches the analytic energy at the start point.
        val mu = GravitySystem.G * mass
        val r0 = Vector2(-1500f, 80f).len()
        val expectedStatic = sqrt(40f * 40f - 2f * mu / r0)
        assertTrue(Math.abs(vInfStatic - expectedStatic) < 0.5f,
            "static flyby should conserve energy: got $vInfStatic expected ~$expectedStatic")

        // Planet positioned at the origin via a far parent, moving along -x (forward case).
        val anchor = Body("Anchor", 0f, 0f, Vector2(0f, -20000f))
        val forward = OrbitingBody("Forward", mass, 5f, anchor, 20000f, 4e-4f, (PI / 2).toFloat())
        val vInfForward = flybyVInf(forward)

        // Same geometry but moving along +x (reverse case).
        val reverse = OrbitingBody("Reverse", mass, 5f, anchor, 20000f, -4e-4f, (PI / 2).toFloat())
        val vInfReverse = flybyVInf(reverse)

        val dForward = vInfForward - vInfStatic
        val dReverse = vInfReverse - vInfStatic

        // One direction must gain energy, the other must lose it.
        assertTrue(dForward * dReverse < 0f,
            "assist should be signed by direction: dForward=$dForward dReverse=$dReverse")
        // And the effect must be non-trivial.
        assertTrue(Math.abs(dForward) > 0.2f && Math.abs(dReverse) > 0.2f,
            "assist magnitude too small: dForward=$dForward dReverse=$dReverse")
    }
}
