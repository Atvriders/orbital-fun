package com.atvriders.orbitalfun.game

import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.physics.Body

/**
 * Tracks which bodies the player has scanned and the value of map data not yet
 * sold. Bodies farther from the system origin yield more valuable maps, so
 * ranging outward and scanning is the core money loop in Survival.
 */
class Scanner {
    val discovered = mutableSetOf<String>()

    /** Money value of scans collected but not yet sold at a dock. */
    var unsoldValue: Int = 0
        private set

    var totalSold: Int = 0
        private set

    /**
     * Discover any not-yet-known bodies within [scannerRange] of [shipPos].
     * Returns how many new bodies were found this scan.
     */
    fun scan(shipPos: Vector2, bodies: List<Body>, scannerRange: Float, origin: Vector2): Int {
        var found = 0
        for (b in bodies) {
            if (b.name in discovered) continue
            if (shipPos.dst(b.position) <= scannerRange) {
                discovered.add(b.name)
                val distFromOrigin = b.position.dst(origin)
                unsoldValue += MAP_BASE_VALUE + (distFromOrigin * MAP_VALUE_PER_DISTANCE).toInt()
                found++
            }
        }
        return found
    }

    /** Sell all collected map data, returning the money earned. */
    fun sell(): Int {
        val value = unsoldValue
        totalSold += value
        unsoldValue = 0
        return value
    }

    companion object {
        const val MAP_BASE_VALUE = 25
        const val MAP_VALUE_PER_DISTANCE = 0.25f
    }
}
