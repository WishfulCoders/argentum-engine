package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.JackdawSavior
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jackdaw Savior — {2}{W} Creature — Bird Cleric 3/1
 * Flying
 * Whenever this creature or another creature you control with flying dies, return another target
 * creature card with lesser mana value from your graveyard to the battlefield.
 *
 * The return is a real target, chosen as the dies trigger is put on the stack (CR 603.3d) and
 * re-checked on resolution (CR 608.2b):
 * - the legal pool is creature cards in your graveyard with mana value below the dead creature's;
 * - with nothing lesser in the graveyard the trigger has no target and nobody is asked;
 * - a target that leaves the graveyard in response makes the trigger do nothing — it does not
 *   pick a different card at resolution.
 */
class JackdawSaviorScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JackdawSavior))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** The opponent bolts Jackdaw Savior (3 damage kills the 3/1). */
    fun GameTestDriver.boltJackdaw(me: EntityId, jackdaw: EntityId) {
        val opponent = getOpponent(me)
        val bolt = putCardInHand(opponent, "Lightning Bolt")
        giveMana(opponent, Color.RED, 1)
        passPriority(me)
        castSpell(opponent, bolt, listOf(jackdaw)).error shouldBe null
        bothPass()
    }

    test("the target is chosen as the trigger goes on the stack, from lesser-mana-value creature cards") {
        val driver = setup()
        val me = driver.activePlayer!!
        val jackdaw = driver.putCreatureOnBattlefield(me, "Jackdaw Savior")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")      // MV 2
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")     // MV 1
        val giant = driver.putCardInGraveyard(me, "Hill Giant")         // MV 4 — not lesser than 3
        driver.putCardInGraveyard(me, "Lightning Bolt")                 // not a creature card

        driver.boltJackdaw(me, jackdaw)

        val decision = driver.pendingDecision
        withClue("a target decision is raised for the dies trigger: $decision") {
            (decision is ChooseTargetsDecision) shouldBe true
        }
        decision as ChooseTargetsDecision
        withClue("only creature cards with mana value less than 3 are legal targets") {
            decision.legalTargets[0]?.toSet() shouldBe setOf(bears, lions)
        }
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null
        withClue("the trigger sits on the stack with its target chosen") { driver.stackSize shouldBe 1 }

        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        withClue("the untargeted cards stay in the graveyard") {
            driver.getGraveyard(me).contains(lions) shouldBe true
            driver.getGraveyard(me).contains(giant) shouldBe true
        }
    }

    test("with no lesser-mana-value creature card the trigger has no target and does nothing") {
        val driver = setup()
        val me = driver.activePlayer!!
        val jackdaw = driver.putCreatureOnBattlefield(me, "Jackdaw Savior")
        val giant = driver.putCardInGraveyard(me, "Hill Giant")

        driver.boltJackdaw(me, jackdaw)

        withClue("nobody is asked to choose a target") { driver.pendingDecision shouldBe null }
        driver.stackSize shouldBe 0
        driver.getGraveyard(me).contains(giant) shouldBe true
    }

    test("a target that leaves the graveyard in response leaves the trigger with nothing to return") {
        val driver = setup()
        val me = driver.activePlayer!!
        val jackdaw = driver.putCreatureOnBattlefield(me, "Jackdaw Savior")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.boltJackdaw(me, jackdaw)
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null

        // The targeted card is exiled while the trigger waits on the stack.
        driver.replaceState(
            driver.state.moveToZone(bears, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE))
        )
        driver.bothPass()

        withClue("the exiled target stays exiled") { driver.getExile(me).contains(bears) shouldBe true }
        withClue("the trigger does not pick a replacement card on resolution") {
            driver.findPermanent(me, "Savannah Lions") shouldBe null
            driver.getGraveyard(me).contains(lions) shouldBe true
        }
    }

    test("another flier you control dying uses that creature's mana value as the cap") {
        val driver = setup()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Jackdaw Savior")
        // Storm Crow: {1}{U} 1/2 flier, MV 2 — so only MV 0–1 creature cards qualify.
        val crow = driver.putCreatureOnBattlefield(me, "Storm Crow")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")      // MV 2 — not lesser than 2
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")     // MV 1

        val opponent = driver.getOpponent(me)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(crow)).error shouldBe null
        driver.bothPass()

        // One legal target: it is either auto-chosen or offered alone.
        (driver.pendingDecision as? ChooseTargetsDecision)?.let { decision ->
            decision.legalTargets[0]?.toSet() shouldBe setOf(lions)
            driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(lions)))).error shouldBe null
        }
        driver.bothPass()

        driver.findPermanent(me, "Savannah Lions") shouldNotBe null
        driver.getGraveyard(me).contains(bears) shouldBe true
    }
})
