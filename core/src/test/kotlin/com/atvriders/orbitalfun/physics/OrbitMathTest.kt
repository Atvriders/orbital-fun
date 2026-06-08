package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class OrbitMathTest {

    @BeforeEach
    fun setup() {
        GravitySystem.G = 100f
        GravitySystem.softening = 0.001f
    }

    @Test
    fun `circular state has near-zero eccentricity and matching radius`() {
        val mass = 1000f
        val mu = GravitySystem.G * mass
        val r = 200f
        val v = sqrt(mu / r)

        val e = OrbitMath.elements(Vector2(r, 0f), Vector2(0f, v), mass)
        assertTrue(e.bound, "circular orbit should be bound")
        assertEquals(r, e.semiMajorAxis, r * 0.01f, "semi-major axis ~ r")
        assertTrue(e.eccentricity < 0.01f, "eccentricity ~ 0, was ${e.eccentricity}")
        assertEquals(r, e.apoapsis, r * 0.02f)
        assertEquals(r, e.periapsis, r * 0.02f)
    }

    @Test
    fun `sub-circular speed yields a bound ellipse with periapsis below start`() {
        val mass = 1000f
        val mu = GravitySystem.G * mass
        val r = 200f
        val vCircular = sqrt(mu / r)
        val v = vCircular * 0.85f // slower than circular -> elliptical, start is apoapsis

        val e = OrbitMath.elements(Vector2(r, 0f), Vector2(0f, v), mass)
        assertTrue(e.bound)
        assertTrue(e.eccentricity > 0f && e.eccentricity < 1f, "should be elliptical, e=${e.eccentricity}")
        assertTrue(e.periapsis < r, "periapsis should drop below start radius")
        assertTrue(e.apoapsis >= r - 1f, "apoapsis should be at/above start radius")
    }

    @Test
    fun `escape speed produces an unbound trajectory`() {
        val mass = 1000f
        val mu = GravitySystem.G * mass
        val r = 200f
        val vEscape = sqrt(2f * mu / r)
        val v = vEscape * 1.1f

        val e = OrbitMath.elements(Vector2(r, 0f), Vector2(0f, v), mass)
        assertTrue(!e.bound, "should be unbound above escape speed")
    }
}
