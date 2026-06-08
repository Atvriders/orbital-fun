package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.math.Vector2

/**
 * Fourth-order Runge-Kutta integrator for a point mass moving through the
 * (time-varying) gravity field of a set of [Body] objects, plus a constant
 * thrust acceleration applied over the step.
 *
 * RK4 is accurate enough that a circular orbit stays visibly circular over many
 * periods, which matters for a game where the player reads the orbit shape.
 *
 * Not thread-safe: it reuses scratch vectors, which is fine for the single
 * game/predictor thread.
 */
object Integrator {

    private val k1p = Vector2(); private val k1v = Vector2()
    private val k2p = Vector2(); private val k2v = Vector2()
    private val k3p = Vector2(); private val k3v = Vector2()
    private val k4p = Vector2(); private val k4v = Vector2()
    private val tmp = Vector2()
    private val zero = Vector2()

    private fun updateBodies(bodies: List<Body>, time: Float) {
        for (i in bodies.indices) bodies[i].updatePosition(time)
    }

    /**
     * Advance [pos]/[vel] by [dt] seconds starting at simulation time [t], under
     * gravity from [bodies] plus the constant [thrust] acceleration. Bodies are
     * advanced through the sub-steps and left at time `t + dt`, matching the
     * caller's new simulation time.
     */
    fun step(
        pos: Vector2,
        vel: Vector2,
        thrust: Vector2,
        bodies: List<Body>,
        t: Float,
        dt: Float,
    ) {
        val half = dt * 0.5f

        updateBodies(bodies, t)
        k1p.set(vel)
        GravitySystem.acceleration(pos, bodies, k1v).add(thrust)

        tmp.set(pos).mulAdd(k1p, half)
        k2p.set(vel).mulAdd(k1v, half)
        updateBodies(bodies, t + half)
        GravitySystem.acceleration(tmp, bodies, k2v).add(thrust)

        tmp.set(pos).mulAdd(k2p, half)
        k3p.set(vel).mulAdd(k2v, half)
        // bodies already at t + half
        GravitySystem.acceleration(tmp, bodies, k3v).add(thrust)

        tmp.set(pos).mulAdd(k3p, dt)
        k4p.set(vel).mulAdd(k3v, dt)
        updateBodies(bodies, t + dt)
        GravitySystem.acceleration(tmp, bodies, k4v).add(thrust)

        val sixth = dt / 6f
        pos.add(
            (k1p.x + 2f * k2p.x + 2f * k3p.x + k4p.x) * sixth,
            (k1p.y + 2f * k2p.y + 2f * k3p.y + k4p.y) * sixth,
        )
        vel.add(
            (k1v.x + 2f * k2v.x + 2f * k3v.x + k4v.x) * sixth,
            (k1v.y + 2f * k2v.y + 2f * k3v.y + k4v.y) * sixth,
        )
    }

    /**
     * Forward-integrate a *copy* of the ship state to produce a predicted path,
     * advancing the moving bodies along with it. Points are sampled every
     * [sampleEvery] steps into [out]. Bodies are restored to [startTime] before
     * returning, so calling this does not disturb the live simulation.
     */
    fun predict(
        startPos: Vector2,
        startVel: Vector2,
        thrust: Vector2,
        bodies: List<Body>,
        startTime: Float,
        dt: Float,
        steps: Int,
        sampleEvery: Int,
        out: MutableList<Vector2>,
    ) {
        out.clear()
        val p = Vector2(startPos)
        val v = Vector2(startVel)
        var t = startTime
        out.add(Vector2(p))
        for (i in 1..steps) {
            step(p, v, thrust, bodies, t, dt)
            t += dt
            if (i % sampleEvery == 0) out.add(Vector2(p))
        }
        updateBodies(bodies, startTime)
    }

    /**
     * Preview a KSP-style maneuver node. Coasts a copy of the ship state for
     * [leadSteps] (reaching the node), records the node position in [outNodePos],
     * applies an instantaneous delta-v of [prograde] along velocity and [radial]
     * away from [centralPos], then coasts [postSteps] more — sampling the
     * post-burn path into [out]. Bodies are restored to [startTime] afterward.
     *
     * Returns the post-burn velocity magnitude at the node (for readouts).
     */
    fun predictManeuver(
        startPos: Vector2,
        startVel: Vector2,
        bodies: List<Body>,
        startTime: Float,
        dt: Float,
        leadSteps: Int,
        postSteps: Int,
        sampleEvery: Int,
        centralPos: Vector2,
        prograde: Float,
        radial: Float,
        outNodePos: Vector2,
        out: MutableList<Vector2>,
    ) {
        out.clear()
        val p = Vector2(startPos)
        val v = Vector2(startVel)
        var t = startTime
        for (i in 1..leadSteps) {
            step(p, v, zero, bodies, t, dt)
            t += dt
        }
        outNodePos.set(p)

        // Apply the burn in the node's orbital frame.
        updateBodies(bodies, t)
        val pDir = Vector2(v).nor()
        val rDir = Vector2(p).sub(centralPos).nor()
        v.add(pDir.x * prograde + rDir.x * radial, pDir.y * prograde + rDir.y * radial)

        out.add(Vector2(p))
        for (i in 1..postSteps) {
            step(p, v, zero, bodies, t, dt)
            t += dt
            if (i % sampleEvery == 0) out.add(Vector2(p))
        }
        updateBodies(bodies, startTime)
    }
}
