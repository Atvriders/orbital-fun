package com.atvriders.orbitalfun.game

import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.physics.Body
import com.atvriders.orbitalfun.physics.Integrator
import com.atvriders.orbitalfun.physics.OrbitMath
import com.atvriders.orbitalfun.physics.OrbitalElements

/**
 * The live simulation: the star system, the ship, and the per-mode state
 * (fuel/economy/scanner/upgrades). Owns the fixed-step update that drives the
 * ship through the moving gravity field.
 */
class World(
    val star: Body,
    val planets: List<Body>,
    val dock: Body,
    val ship: Spacecraft,
    val mode: GameMode,
    val upgrades: Upgrades = Upgrades(),
    val economy: Economy = Economy(),
    val scanner: Scanner = Scanner(),
) {
    /** Star + planets — everything that exerts gravity. */
    val gravityBodies: List<Body> = ArrayList<Body>(planets.size + 1).apply {
        add(star)
        addAll(planets)
    }

    val origin: Vector2 = Vector2(star.position)

    /** Active control scheme; set by the screen from persisted [Settings]. */
    var controlScheme: ControlScheme = ControlScheme.BUTTONS

    var simTime: Float = 0f
        private set

    /** Latest predicted coast path, sampled points. */
    val predictedPath: ArrayList<Vector2> = ArrayList()

    /** KSP-style maneuver planning. */
    val maneuver: Maneuver = Maneuver()

    /** Predicted trajectory after the planned burn (empty when no node). */
    val maneuverPath: ArrayList<Vector2> = ArrayList()

    /** World position of the maneuver node (valid when [maneuver].active). */
    val maneuverNode: Vector2 = Vector2()
    var hasManeuverPreview: Boolean = false
        private set

    private val zeroThrust = Vector2()

    init {
        if (!mode.unlimitedFuel) ship.fuel = upgrades.fuelCapacity
        gravityBodies.forEach { it.updatePosition(0f) }
    }

    /** Advance the simulation by [dt] seconds (already scaled by time-warp). */
    fun update(dt: Float) {
        applyThrustAndFuel(dt)
        Integrator.step(ship.position, ship.velocity, ship.thrustAccel, gravityBodies, simTime, dt)
        simTime += dt

        if (ship.thrustAccel.isZero) {
            ship.heading = ship.velocity.angleRad()
        }
        if (mode == GameMode.SURVIVAL) {
            economy.updateExplorationReward(ship.position.dst(origin))
        }
    }

    private fun applyThrustAndFuel(dt: Float) {
        // Determine whether we're thrusting and the effective throttle (0..1),
        // which differs by control scheme.
        val thrusting: Boolean
        val effectiveThrottle: Float
        if (controlScheme == ControlScheme.JOYSTICK) {
            val mag = ship.stick.len().coerceAtMost(1f)
            thrusting = mag > STICK_DEADZONE
            effectiveThrottle = mag
        } else {
            thrusting = ship.wantsThrust
            effectiveThrottle = ship.throttle
        }
        if (!thrusting) {
            ship.thrustAccel.set(0f, 0f)
            return
        }

        val burn = effectiveThrottle * FUEL_BURN_RATE / upgrades.fuelEfficiency * dt
        val allowed: Boolean
        if (mode.unlimitedFuel) {
            ship.fuelUsed += burn
            allowed = true
        } else if (ship.fuel <= 0f) {
            allowed = false
        } else {
            val consumed = minOf(burn, ship.fuel)
            ship.fuel -= consumed
            ship.fuelUsed += consumed
            allowed = true
        }
        if (allowed) {
            if (controlScheme == ControlScheme.JOYSTICK) {
                ship.computeJoystickThrust(upgrades.thrustPower)
            } else {
                ship.computeThrustAccel(star.position, upgrades.thrustPower)
            }
        } else {
            ship.thrustAccel.set(0f, 0f)
        }
    }

    /**
     * Recompute the predicted coast path (where the ship drifts if it stops
     * burning) and, when a maneuver node is active, the post-burn trajectory.
     */
    fun updatePrediction() {
        Integrator.predict(
            ship.position, ship.velocity, zeroThrust, gravityBodies,
            simTime, PREDICT_DT, PREDICT_STEPS, PREDICT_SAMPLE, predictedPath,
        )
        if (maneuver.active) {
            val leadSteps = (maneuver.leadTime / PREDICT_DT).toInt().coerceIn(1, PREDICT_STEPS)
            Integrator.predictManeuver(
                ship.position, ship.velocity, gravityBodies, simTime, PREDICT_DT,
                leadSteps, PREDICT_STEPS, PREDICT_SAMPLE, star.position,
                maneuver.prograde, maneuver.radial, maneuverNode, maneuverPath,
            )
            hasManeuverPreview = true
        } else {
            hasManeuverPreview = false
            maneuverPath.clear()
        }
    }

    /**
     * Commit the planned burn as an instantaneous impulse at the ship's current
     * state (idealized), charging fuel for the delta-v. Returns false if there is
     * no node or (in Survival) not enough fuel.
     */
    fun executeManeuver(): Boolean {
        if (!maneuver.active || maneuver.totalDeltaV <= 0f) return false
        val cost = maneuver.totalDeltaV * MANEUVER_FUEL_PER_DV / upgrades.fuelEfficiency
        if (mode.unlimitedFuel) {
            ship.fuelUsed += cost
        } else {
            if (ship.fuel < cost) return false
            ship.fuel -= cost
            ship.fuelUsed += cost
        }
        val pDir = Vector2(ship.velocity).nor()
        val rDir = Vector2(ship.position).sub(star.position).nor()
        ship.velocity.add(
            pDir.x * maneuver.prograde + rDir.x * maneuver.radial,
            pDir.y * maneuver.prograde + rDir.y * maneuver.radial,
        )
        maneuver.clear()
        hasManeuverPreview = false
        maneuverPath.clear()
        return true
    }

    /** Run a Scan from the ship's current position; returns bodies newly found. */
    fun performScan(): Int =
        scanner.scan(ship.position, gravityBodies, upgrades.scannerRange, origin)

    /** Distance from the ship to the dock surface (0 when touching it). */
    fun distanceToDock(): Float = (ship.position.dst(dock.position) - dock.radius).coerceAtLeast(0f)

    fun isNearDock(): Boolean = distanceToDock() <= DOCK_RANGE

    /** Out of fuel and not at a dock — the Survival fail/warning condition. */
    fun isStranded(): Boolean =
        mode.canFail && ship.fuel <= 0f && !isNearDock()

    /** Body with the strongest pull at the ship — used for orbit-element readout. */
    fun dominantBody(): Body {
        var best = star
        var bestPull = -1f
        for (b in gravityBodies) {
            val r2 = ship.position.dst2(b.position).coerceAtLeast(1f)
            val pull = b.mass / r2
            if (pull > bestPull) {
                bestPull = pull
                best = b
            }
        }
        return best
    }

    fun orbitElements(about: Body): OrbitalElements {
        val relPos = Vector2(ship.position).sub(about.position)
        val relVel = Vector2(ship.velocity).sub(about.velocity)
        return OrbitMath.elements(relPos, relVel, about.mass)
    }

    fun distanceFromOrigin(): Float = ship.position.dst(origin)

    companion object {
        /** Fuel units burned per second at full throttle, before efficiency. */
        const val FUEL_BURN_RATE = 6f
        const val DOCK_RANGE = 36f

        /** Joystick deflection below this magnitude counts as no input. */
        const val STICK_DEADZONE = 0.12f

        const val PREDICT_DT = 1f / 60f
        const val PREDICT_STEPS = 1200
        const val PREDICT_SAMPLE = 4

        /** Fuel charged per unit of delta-v when executing a maneuver node. */
        const val MANEUVER_FUEL_PER_DV = 0.5f
    }
}
