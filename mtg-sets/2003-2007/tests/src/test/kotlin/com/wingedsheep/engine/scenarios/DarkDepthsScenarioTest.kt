package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.csp.cards.DarkDepths
import com.wingedsheep.mtg.sets.definitions.zen.cards.VampireHexmage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dark Depths (CSP) — ten ice counters on entry, a {3} counter-removal ability, and a *state*
 * trigger (CR 603.8) that sacrifices it for Marit Lage once no ice counters remain, however they
 * went away. Vampire Hexmage stripping every counter at once is the canonical way it fires.
 */
class DarkDepthsScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DarkDepths, VampireHexmage))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    /** Play Dark Depths as the land drop so its enters-with-counters replacement applies. */
    fun playDepths(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Dark Depths")
        driver.playLand(player, card).error shouldBe null
        return driver.findPermanent(player, "Dark Depths")!!
    }

    fun iceCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.ICE) ?: 0

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null) {
            driver.bothPass()
        }
    }

    test("enters with ten ice counters, and {3} removes one without making Marit Lage") {
        val (driver, you) = newGame()
        val depths = playDepths(driver, you)
        iceCounters(driver, depths) shouldBe 10

        driver.giveColorlessMana(you, 3)
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = depths,
                abilityId = DarkDepths.activatedAbilities.first().id,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        resolveAll(driver)

        iceCounters(driver, depths) shouldBe 9
        driver.findPermanent(you, "Dark Depths").shouldNotBeNull()
        driver.findPermanent(you, "Marit Lage").shouldBeNull()
    }

    test("Vampire Hexmage removes every ice counter; the state trigger sacrifices it for Marit Lage") {
        val (driver, you) = newGame()
        val depths = playDepths(driver, you)
        val hexmage = driver.putCreatureOnBattlefield(you, "Vampire Hexmage")

        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = hexmage,
                abilityId = VampireHexmage.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(depths)),
            )
        ).error shouldBe null
        // Hexmage is sacrificed as the cost.
        driver.findPermanent(you, "Vampire Hexmage").shouldBeNull()

        resolveAll(driver)

        withClue("Dark Depths is sacrificed by its own state trigger") {
            driver.findPermanent(you, "Dark Depths").shouldBeNull()
            driver.getGraveyardCardNames(you).contains("Dark Depths") shouldBe true
        }
        val marit = driver.findPermanent(you, "Marit Lage")
        marit.shouldNotBeNull()
        val projected = projector.project(driver.state)
        projected.getPower(marit) shouldBe 20
        projected.getToughness(marit) shouldBe 20
        projected.hasKeyword(marit, Keyword.FLYING) shouldBe true
        projected.hasKeyword(marit, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.isLegendary(marit) shouldBe true
    }
})
