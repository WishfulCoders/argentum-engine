package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rav.cards.SeedsOfStrength
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Seeds of Strength (RAV #227) — three separate "Target creature gets +1/+1 until end of turn."
 * sentences. Each "target" is its own instance (CR 115.3), so one creature may be chosen for all
 * three and get +3/+3 (2005-10-01 ruling).
 */
class SeedsOfStrengthScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SeedsOfStrength)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castSeeds(targets: List<EntityId>) {
        val seeds = putCardInHand(player1, "Seeds of Strength")
        giveMana(player1, Color.GREEN, 1)
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, seeds, targets).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("the same creature can be chosen for all three targets and gets +3/+3") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.castSeeds(listOf(bears, bears, bears))

        d.state.projectedState.getPower(bears) shouldBe 5
        d.state.projectedState.getToughness(bears) shouldBe 5
    }

    test("the targets can be split: +2/+2 on one creature and +1/+1 on another") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.castSeeds(listOf(bears, other, bears))

        d.state.projectedState.getPower(bears) shouldBe 4
        d.state.projectedState.getPower(other) shouldBe 3
    }

    test("the offered cast doesn't tell the client to strip earlier picks from later targets") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val seeds = d.putCardInHand(d.player1, "Seeds of Strength")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.giveMana(d.player1, Color.WHITE, 1)

        val cast = d.legalActions(d.player1)
            .firstOrNull { (it.action as? CastSpell)?.cardId == seeds }
            .shouldNotBeNull()
        val requirements = cast.targetRequirements.shouldNotBeNull()

        requirements.size shouldBe 3
        requirements.forEach { req ->
            withClue("requirement ${req.index}") {
                req.mustDifferFromEarlier shouldBe false
                (bears in req.validTargets) shouldBe true
            }
        }
    }
})
