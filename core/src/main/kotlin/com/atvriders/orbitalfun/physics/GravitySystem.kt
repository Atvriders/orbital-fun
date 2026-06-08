package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2

/**
 * Computes Newtonian gravitational acceleration on a point from a set of bodies.
 *
 * Uses real `a = G * m / r^2` directed toward each body, with a softening term
 * so a near-direct hit does not produce an infinite force (which would blow up
 * the integrator). Constants are tuned for playable timescales rather than SI
 * realism.
 */
object GravitySystem {

    /** Tuned gravitational constant. Larger = stronger pull / faster orbits. */
    var G: Float = 6000f

    /** Softening length (world units) added in quadrature to the distance. */
    var softening: Float = 4f

    private val diff = Vector2()

    /**
     * Sum the gravitational acceleration at [pos] due to all [bodies], writing the
     * result into [out] (also returned). Bodies are read at their current
     * positions — advance them with [Body.updatePosition] first if needed.
     */
    fun acceleration(pos: Vector2, bodies: List<Body>, out: Vector2): Vector2 {
        out.set(0f, 0f)
        val soft2 = softening * softening
        for (i in bodies.indices) {
            val b = bodies[i]
            diff.set(b.position).sub(pos)
            val r2 = diff.len2() + soft2
            // a = G m / r^2, in the unit direction of diff = diff / |diff|.
            // Combine: (G m / r^2) * (diff / r) = G m * diff / r^3.
            val invR = 1f / Math.sqrt(r2.toDouble()).toFloat()
            val scale = G * b.mass * invR / r2
            out.add(diff.x * scale, diff.y * scale)
        }
        return out
    }
}
