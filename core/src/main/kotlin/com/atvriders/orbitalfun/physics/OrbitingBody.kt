package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

/**
 * A planet (or moon) that travels a fixed circular orbit around [parent] "on
 * rails": its position is an analytic function of time, so it is perfectly
 * deterministic and stable.
 *
 * This determinism is what makes gravity assists work and repeatable: because
 * the body genuinely moves, the gravity field the spacecraft feels is
 * time-varying, so a flyby transfers energy in the system frame. A flyby behind
 * the body's motion speeds the craft up (slingshot); a flyby ahead of it slows
 * the craft down (reverse assist). No special-case code is needed — it falls
 * straight out of [Integrator] stepping through the moving field.
 */
class OrbitingBody(
    name: String,
    mass: Float,
    radius: Float,
    val parent: Body,
    val orbitRadius: Float,
    /** Angular velocity in radians/second. Sign sets the orbit direction. */
    val angularVelocity: Float,
    /** Starting angle in radians at t = 0. */
    val phase: Float = 0f,
    color: Color = Color.WHITE.cpy(),
    isDock: Boolean = false,
) : Body(name, mass, radius, Vector2(), Vector2(), color, isDock) {

    init {
        updatePosition(0f)
    }

    override fun updatePosition(time: Float) {
        val angle = phase + angularVelocity * time
        val cos = MathUtils.cos(angle)
        val sin = MathUtils.sin(angle)
        position.set(
            parent.position.x + orbitRadius * cos,
            parent.position.y + orbitRadius * sin,
        )
        // Tangential velocity of a circular orbit: v = w * r, perpendicular to radius.
        val speed = angularVelocity * orbitRadius
        velocity.set(-sin * speed, cos * speed)
    }
}
