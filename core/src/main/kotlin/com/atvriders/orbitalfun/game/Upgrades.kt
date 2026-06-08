package com.atvriders.orbitalfun.game

/**
 * Upgradeable ship stats. Each stat has an integer level; the derived getters
 * turn levels into the actual gameplay values. Costs grow with level so deeper
 * upgrades require ranging farther out to afford.
 */
class Upgrades(
    var thrustLevel: Int = 0,
    var fuelCapacityLevel: Int = 0,
    var efficiencyLevel: Int = 0,
    var scannerLevel: Int = 0,
) {
    /** Engine acceleration (world units / s^2). */
    val thrustPower: Float get() = BASE_THRUST * (1f + 0.30f * thrustLevel)

    /** Maximum fuel the tank holds. */
    val fuelCapacity: Float get() = BASE_FUEL_CAPACITY + 60f * fuelCapacityLevel

    /** Higher efficiency burns less fuel for the same thrust. */
    val fuelEfficiency: Float get() = 1f + 0.25f * efficiencyLevel

    /** Radius within which the scanner can discover bodies. */
    val scannerRange: Float get() = BASE_SCANNER_RANGE + 140f * scannerLevel

    fun cost(stat: Stat): Int {
        val level = levelOf(stat)
        return Math.round(BASE_COST * Math.pow(COST_GROWTH.toDouble(), level.toDouble())).toInt()
    }

    fun levelOf(stat: Stat): Int = when (stat) {
        Stat.THRUST -> thrustLevel
        Stat.FUEL_CAPACITY -> fuelCapacityLevel
        Stat.EFFICIENCY -> efficiencyLevel
        Stat.SCANNER -> scannerLevel
    }

    fun increment(stat: Stat) {
        when (stat) {
            Stat.THRUST -> thrustLevel++
            Stat.FUEL_CAPACITY -> fuelCapacityLevel++
            Stat.EFFICIENCY -> efficiencyLevel++
            Stat.SCANNER -> scannerLevel++
        }
    }

    enum class Stat(val label: String) {
        THRUST("Thrust"),
        FUEL_CAPACITY("Fuel Tank"),
        EFFICIENCY("Efficiency"),
        SCANNER("Scanner"),
    }

    companion object {
        const val BASE_THRUST = 90f
        const val BASE_FUEL_CAPACITY = 120f
        const val BASE_SCANNER_RANGE = 260f
        const val BASE_COST = 120
        const val COST_GROWTH = 1.8f
    }
}
