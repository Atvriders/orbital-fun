package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2

/** Classical orbital elements of the ship relative to a central body. */
data class OrbitalElements(
    /** Semi-major axis (world units). Negative/NaN for unbound trajectories. */
    val semiMajorAxis: Float,
    val eccentricity: Float,
    /** Farthest distance from the central body; +inf if the orbit is unbound. */
    val apoapsis: Float,
    /** Closest distance from the central body. */
    val periapsis: Float,
    /** True when the orbit is closed (bound), i.e. specific energy < 0. */
    val bound: Boolean,
)

/**
 * Two-body orbital element extraction. Given the ship's position and velocity
 * relative to a single dominant body of mass [centralMass], recover the shape of
 * the (instantaneous) Kepler orbit. Used for HUD readouts.
 */
object OrbitMath {

    /**
     * @param relPos ship position minus central body position
     * @param relVel ship velocity minus central body velocity
     */
    fun elements(relPos: Vector2, relVel: Vector2, centralMass: Float): OrbitalElements {
        val mu = GravitySystem.G * centralMass
        val r = relPos.len()
        val v2 = relVel.len2()

        // Specific orbital energy: e = v^2/2 - mu/r.
        val energy = v2 / 2f - mu / r
        val bound = energy < 0f

        val a = if (energy != 0f) -mu / (2f * energy) else Float.POSITIVE_INFINITY

        // Eccentricity vector: ((v^2 - mu/r) r - (r . v) v) / mu.
        val rDotV = relPos.dot(relVel)
        val ex = ((v2 - mu / r) * relPos.x - rDotV * relVel.x) / mu
        val ey = ((v2 - mu / r) * relPos.y - rDotV * relVel.y) / mu
        val e = Vector2(ex, ey).len()

        val apoapsis = if (bound) a * (1f + e) else Float.POSITIVE_INFINITY
        val periapsis = if (a.isFinite()) Math.abs(a) * (1f - e) else r

        return OrbitalElements(a, e, apoapsis, periapsis, bound)
    }
}
