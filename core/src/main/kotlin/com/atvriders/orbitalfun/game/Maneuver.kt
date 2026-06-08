package com.atvriders.orbitalfun.game

/**
 * A KSP-style maneuver node: a planned instantaneous burn placed [leadTime]
 * seconds ahead on the current coast trajectory. The burn is expressed in the
 * orbital frame at the node — [prograde] along the velocity (negative = retro)
 * and [radial] away from the central body (negative = toward it).
 *
 * The planner integrates the coast up to the node, applies this delta-v, then
 * integrates onward so the player can preview the resulting orbit before
 * committing — exactly like dragging a maneuver node in Kerbal Space Program.
 */
class Maneuver {
    var active: Boolean = false

    /** Seconds from "now" until the node fires. */
    var leadTime: Float = 6f

    /** Delta-v components (world units/sec) in the node's orbital frame. */
    var prograde: Float = 0f
    var radial: Float = 0f

    val totalDeltaV: Float
        get() = Math.sqrt((prograde * prograde + radial * radial).toDouble()).toFloat()

    fun clear() {
        active = false
        prograde = 0f
        radial = 0f
        leadTime = 6f
    }
}
