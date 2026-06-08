package com.atvriders.orbitalfun.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.physics.Body

/** Which way the engine pushes, relative to the current motion / central body. */
enum class ThrustDirection { NONE, PROGRADE, RETROGRADE, RADIAL_IN, RADIAL_OUT }

/**
 * The player's ship. Physically a test particle: its own mass does not affect
 * its motion (gravity acceleration is mass-independent), so [mass] is cosmetic.
 *
 * Thrust is expressed as an acceleration in a direction chosen relative to the
 * velocity (prograde/retrograde) or the central body (radial in/out), scaled by
 * [throttle].
 */
class Spacecraft(
    startPos: Vector2,
    startVel: Vector2,
    radius: Float = 3f,
    color: Color = Color.valueOf("9fefff"),
) : Body("Ship", mass = 1f, radius = radius, position = Vector2(startPos), velocity = Vector2(startVel), color = color) {

    /** 0..1 fraction of available engine power. */
    var throttle: Float = 1f
    var activeDir: ThrustDirection = ThrustDirection.NONE

    /** Remaining fuel (used in Survival; ignored when fuel is unlimited). */
    var fuel: Float = 0f

    /** Cumulative fuel consumed — always tracked, shown in both modes. */
    var fuelUsed: Float = 0f

    /** Facing angle (radians) for rendering the ship sprite. */
    var heading: Float = 0f

    /** Scratch buffer for the most recently computed thrust acceleration. */
    val thrustAccel = Vector2()

    /** True when the player has selected a thrust direction and opened the throttle. */
    val wantsThrust: Boolean
        get() = activeDir != ThrustDirection.NONE && throttle > 0f

    /**
     * Compute the thrust acceleration this frame given the central body position
     * and the (upgrade-derived) [thrustPower]. Result is stored in [thrustAccel]
     * and returned. Returns a zero vector when not thrusting.
     */
    fun computeThrustAccel(centralPos: Vector2, thrustPower: Float): Vector2 {
        thrustAccel.set(0f, 0f)
        if (!wantsThrust) return thrustAccel
        when (activeDir) {
            ThrustDirection.PROGRADE -> thrustAccel.set(velocity)
            ThrustDirection.RETROGRADE -> thrustAccel.set(velocity).scl(-1f)
            ThrustDirection.RADIAL_OUT -> thrustAccel.set(position).sub(centralPos)
            ThrustDirection.RADIAL_IN -> thrustAccel.set(centralPos).sub(position)
            ThrustDirection.NONE -> return thrustAccel
        }
        if (thrustAccel.len2() < 1e-6f) {
            thrustAccel.set(0f, 0f)
            return thrustAccel
        }
        thrustAccel.nor().scl(thrustPower * throttle)
        heading = thrustAccel.angleRad()
        return thrustAccel
    }
}
