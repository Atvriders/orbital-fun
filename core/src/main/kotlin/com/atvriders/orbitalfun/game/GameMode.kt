package com.atvriders.orbitalfun.game

/**
 * The two play modes.
 *
 * - [SANDBOX]: fuel is unlimited, there is no fail state, but the fuel-used
 *   counter still ticks up so you can judge how efficient a maneuver was.
 * - [SURVIVAL]: fuel is finite (bounded by [Upgrades.fuelCapacity]); you earn
 *   money by ranging farther out and selling scans, and spend it on fuel and
 *   ship upgrades. Running dry far from a dock strands you (game over).
 */
enum class GameMode(val unlimitedFuel: Boolean, val canFail: Boolean) {
    SANDBOX(unlimitedFuel = true, canFail = false),
    SURVIVAL(unlimitedFuel = false, canFail = true),
}
