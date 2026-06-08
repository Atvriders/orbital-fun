package com.atvriders.orbitalfun.physics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

/**
 * A gravitating body. The base [Body] is a static attractor (e.g. a star).
 * Its [position] is fixed unless a subclass overrides [updatePosition].
 *
 * Positions/velocities are in world units; world units are tuned for fun, not
 * for matching real SI values (see [GravitySystem.G]).
 */
open class Body(
    val name: String,
    /** Gravitational mass. Drives the attraction this body exerts. */
    val mass: Float,
    /** Visual + collision radius in world units. */
    val radius: Float,
    val position: Vector2 = Vector2(),
    val velocity: Vector2 = Vector2(),
    val color: Color = Color.WHITE.cpy(),
    /** Distance from the system origin at which this body becomes scannable, etc. */
    val isDock: Boolean = false,
) {
    /**
     * Advance this body to the simulation time [time] (seconds). Static bodies do
     * nothing; bodies on rails recompute their [position] and [velocity].
     */
    open fun updatePosition(time: Float) {
        // Static body: nothing to do.
    }
}
