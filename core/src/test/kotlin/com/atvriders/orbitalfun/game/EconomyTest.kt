package com.atvriders.orbitalfun.game

import com.badlogic.gdx.math.Vector2
import com.atvriders.orbitalfun.physics.Body
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EconomyTest {

    @Test
    fun `exploration pays only for reaching new farthest distance`() {
        val econ = Economy()
        val first = econ.updateExplorationReward(100f)
        assertTrue(first > 0)
        val moneyAfterFirst = econ.money

        // Coming back closer pays nothing.
        assertEquals(0, econ.updateExplorationReward(80f))
        assertEquals(moneyAfterFirst, econ.money)

        // Pushing farther pays again.
        val third = econ.updateExplorationReward(150f)
        assertTrue(third > 0)
        assertEquals(moneyAfterFirst + third, econ.money)
    }

    @Test
    fun `scanning then selling maps adds money`() {
        val scanner = Scanner()
        val target = Body("Far", 10f, 5f, Vector2(300f, 0f))
        val found = scanner.scan(Vector2(290f, 0f), listOf(target), scannerRange = 50f, origin = Vector2(0f, 0f))
        assertEquals(1, found)
        assertTrue(scanner.unsoldValue > 0)

        // Scanning again finds nothing new.
        assertEquals(0, scanner.scan(Vector2(290f, 0f), listOf(target), 50f, Vector2(0f, 0f)))

        val econ = Economy()
        val earned = econ.sellMaps(scanner)
        assertEquals(earned, econ.money)
        assertEquals(0, scanner.unsoldValue)
    }

    @Test
    fun `buying an upgrade debits money and raises the level`() {
        val econ = Economy(money = 1000)
        val upgrades = Upgrades()
        val cost = upgrades.cost(Upgrades.Stat.THRUST)
        assertTrue(econ.tryBuyUpgrade(upgrades, Upgrades.Stat.THRUST))
        assertEquals(1, upgrades.thrustLevel)
        assertEquals(1000 - cost, econ.money)

        // Can't afford with an empty wallet.
        val broke = Economy(money = 0)
        assertFalse(broke.tryBuyUpgrade(Upgrades(), Upgrades.Stat.THRUST))
    }

    @Test
    fun `buying fuel is limited by money and tank space`() {
        val econ = Economy(money = 1000)
        val ship = Spacecraft(Vector2(), Vector2())
        ship.fuel = 0f
        val bought = econ.buyFuel(25f, ship, capacity = 120f)
        assertEquals(25f, bought, 0.01f)
        assertEquals(25f, ship.fuel, 0.01f)
        assertEquals(1000 - 25 * Economy.FUEL_PRICE, econ.money)

        // Tank space caps the purchase.
        val topUp = econ.buyFuel(1000f, ship, capacity = 120f)
        assertEquals(95f, topUp, 0.01f)
        assertEquals(120f, ship.fuel, 0.01f)
    }
}
