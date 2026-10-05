package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.MagdaBrazenOutlaw
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Magda, Brazen Outlaw (KHM #142) — {1}{R} Legendary Creature — Dwarf Berserker 2/1.
 *
 *   Other Dwarves you control get +1/+0.
 *   Whenever a Dwarf you control becomes tapped, create a Treasure token.
 *   Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that card
 *   onto the battlefield, then shuffle.
 */
class MagdaBrazenOutlawScenarioTest : FunSpec({

    val dwarf = card("Test Magda Dwarf") {
        manaCost = "{R}"
        typeLine = "Creature — Dwarf Warrior"
        power = 1
        toughness = 1
    }

    val dragon = card("Test Magda Dragon") {
        manaCost = "{4}{R}{R}"
        typeLine = "Creature — Dragon"
        power = 6
        toughness = 6
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MagdaBrazenOutlaw, PredefinedTokens.Treasure, dwarf, dragon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.treasures(player: EntityId): List<EntityId> =
        getPermanents(player).filter { getCardName(it) == "Treasure" }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && !isPaused && guard++ < 20) bothPass()
    }

    test("other Dwarves get +1/+0; Magda herself does not") {
        val driver = newDriver()
        val me = driver.player1
        val magda = driver.putCreatureOnBattlefield(me, "Magda, Brazen Outlaw")
        val d = driver.putCreatureOnBattlefield(me, "Test Magda Dwarf")
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")

        driver.state.projectedState.getPower(d) shouldBe 2
        driver.state.projectedState.getToughness(d) shouldBe 1
        driver.state.projectedState.getPower(magda) shouldBe 2
        driver.state.projectedState.getPower(goblin) shouldBe 2
    }

    test("a Dwarf attacking (becoming tapped) makes a Treasure; a non-Dwarf does not") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(me, "Magda, Brazen Outlaw")
        val d = driver.putCreatureOnBattlefield(me, "Test Magda Dwarf")
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")
        driver.removeSummoningSickness(d)
        driver.removeSummoningSickness(goblin)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(d, goblin), opp)
        driver.drainStack()

        withClue("only the Dwarf's tap triggers Magda") {
            driver.treasures(me).size shouldBe 1
        }
    }

    test("Magda is a Dwarf too: her own tapping makes a Treasure") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val magda = driver.putCreatureOnBattlefield(me, "Magda, Brazen Outlaw")
        driver.removeSummoningSickness(magda)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(magda), opp)
        driver.drainStack()

        driver.treasures(me).size shouldBe 1
    }

    test("sacrifice five Treasures: a Dragon card goes from library onto the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val magda = driver.putCreatureOnBattlefield(me, "Magda, Brazen Outlaw")
        repeat(5) { driver.putPermanentOnBattlefield(me, "Treasure") }
        driver.putCardOnTopOfLibrary(me, "Test Magda Dragon")
        val treasures = driver.treasures(me)
        treasures.size shouldBe 5

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = magda,
                abilityId = MagdaBrazenOutlaw.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = treasures)
            )
        ).outcome shouldBe Outcome.Done
        driver.treasures(me).size shouldBe 0

        driver.drainStack()
        val decision = driver.pendingDecision as SelectCardsDecision
        withClue("only artifact or Dragon cards are findable (the library is otherwise Mountains)") {
            decision.options.map { driver.getCardName(it) } shouldBe listOf("Test Magda Dragon")
        }
        driver.submitCardSelection(me, decision.options)
        driver.drainStack()

        (driver.findPermanent(me, "Test Magda Dragon") != null) shouldBe true
    }

    test("four Treasures are not enough") {
        val driver = newDriver()
        val me = driver.player1
        val magda = driver.putCreatureOnBattlefield(me, "Magda, Brazen Outlaw")
        repeat(4) { driver.putPermanentOnBattlefield(me, "Treasure") }

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = magda,
                abilityId = MagdaBrazenOutlaw.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = driver.treasures(me))
            )
        ).outcome shouldNotBe Outcome.Done
    }
})
