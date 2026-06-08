package com.atvriders.orbitalfun.game

/**
 * The Survival-mode wallet and the rules that move money in and out:
 * - exploration rewards for reaching a new farthest distance from origin,
 * - selling scanned maps (via [Scanner]),
 * - spending on fuel and upgrades.
 */
class Economy(var money: Int = 0) {

    /** Farthest distance from the system origin reached so far. */
    var maxDistance: Float = 0f
        private set

    /**
     * Reward the player for pushing the exploration frontier. When [distance]
     * exceeds the previous best, pay out proportionally to the gain. Returns the
     * money earned (0 if no new ground was covered).
     */
    fun updateExplorationReward(distance: Float): Int {
        if (distance <= maxDistance) return 0
        val gained = ((distance - maxDistance) * DISTANCE_REWARD).toInt()
        maxDistance = distance
        money += gained
        return gained
    }

    /** Sell all of the scanner's unsold maps into the wallet. Returns earnings. */
    fun sellMaps(scanner: Scanner): Int {
        val earned = scanner.sell()
        money += earned
        return earned
    }

    /**
     * Buy up to [units] of fuel, limited by free tank space ([capacity] minus
     * current fuel) and by what the wallet can afford. Returns units bought.
     */
    fun buyFuel(units: Float, ship: Spacecraft, capacity: Float): Float {
        val space = (capacity - ship.fuel).coerceAtLeast(0f)
        val affordable = money / FUEL_PRICE.toFloat()
        val bought = minOf(units, space, affordable)
        if (bought <= 0f) return 0f
        money -= Math.ceil((bought * FUEL_PRICE).toDouble()).toInt()
        ship.fuel += bought
        return bought
    }

    /** Buy one level of [stat] if affordable. Returns true on success. */
    fun tryBuyUpgrade(upgrades: Upgrades, stat: Upgrades.Stat): Boolean {
        val cost = upgrades.cost(stat)
        if (money < cost) return false
        money -= cost
        upgrades.increment(stat)
        return true
    }

    companion object {
        /** Money per world-unit of new exploration distance. */
        const val DISTANCE_REWARD = 0.5f

        /** Money per unit of fuel. */
        const val FUEL_PRICE = 2
    }
}
