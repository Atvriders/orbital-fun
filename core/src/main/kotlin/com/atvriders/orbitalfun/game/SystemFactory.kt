package com.atvriders.orbitalfun.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.physics.Body
import com.atvriders.orbitalfun.physics.GravitySystem
import com.atvriders.orbitalfun.physics.OrbitingBody

/**
 * Builds the hand-tuned star system used by both game modes. One central star
 * with several planets on rails (one of them the home dock). Orbital angular
 * velocities are chosen so each planet sits on a near-circular orbit for the
 * tuned gravity constant, and so periods are a handful of seconds — fast enough
 * to make gravity assists practical to set up.
 */
object SystemFactory {

    private data class PlanetSpec(
        val name: String,
        val orbitRadius: Float,
        val mass: Float,
        val radius: Float,
        val phase: Float,
        val color: Color,
        val isDock: Boolean = false,
    )

    private const val STAR_MASS = 1000f
    private const val STAR_RADIUS = 34f

    private val SPECS = listOf(
        PlanetSpec("Rilla", 200f, 70f, 16f, 0.0f, Color.valueOf("ff8a3d")),
        PlanetSpec("Haven Station", 260f, 45f, 13f, 1.2f, Color.valueOf("4db8ff"), isDock = true),
        PlanetSpec("Borealis", 400f, 90f, 20f, 2.5f, Color.valueOf("57e0c8")),
        PlanetSpec("Cthon", 580f, 120f, 24f, 4.0f, Color.valueOf("ff5b5b")),
        PlanetSpec("Vesper", 780f, 80f, 18f, 5.4f, Color.valueOf("c98bff")),
    )

    fun build(mode: GameMode): World {
        // Keep the tuned constants in sync with the speeds computed below.
        GravitySystem.G = 6000f
        GravitySystem.softening = 4f

        val star = Body(
            name = "Sol",
            mass = STAR_MASS,
            radius = STAR_RADIUS,
            position = Vector2(0f, 0f),
            color = Color.valueOf("ffe066"),
        )

        val mu = GravitySystem.G * STAR_MASS
        val planets = SPECS.map { spec ->
            // Circular-orbit angular velocity: w = sqrt(mu / r^3).
            val w = Math.sqrt((mu / (spec.orbitRadius * spec.orbitRadius * spec.orbitRadius)).toDouble()).toFloat()
            OrbitingBody(
                name = spec.name,
                mass = spec.mass,
                radius = spec.radius,
                parent = star,
                orbitRadius = spec.orbitRadius,
                angularVelocity = w,
                phase = spec.phase,
                color = spec.color,
                isDock = spec.isDock,
            )
        }

        val dock = planets.first { it.isDock }

        // Start the ship docked: just outside the station, matching its velocity.
        val startPos = Vector2(dock.position).add(
            Vector2(dock.position).sub(star.position).nor().scl(dock.radius + 10f)
        )
        val startVel = Vector2(dock.velocity)
        val ship = Spacecraft(startPos, startVel)
        ship.heading = startVel.angleRad()

        return World(
            star = star,
            planets = planets,
            dock = dock,
            ship = ship,
            mode = mode,
        )
    }
}
