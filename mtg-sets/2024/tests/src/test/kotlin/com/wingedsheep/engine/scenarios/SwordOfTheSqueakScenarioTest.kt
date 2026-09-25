package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sword of the Squeak — {2} Artifact — Equipment.
 *   Equipped creature gets +1/+1 for each creature you control with base power or toughness 1.
 *   Whenever a Hamster, Mouse, Rat, or Squirrel you control enters, you may attach this
 *   Equipment to that creature.
 *   Equip {2}
 *
 * Pins "base power or toughness" as the layer-7b value: +1/+1 counters and pumps raise a 1/1's
 * power without changing its base, so it keeps counting; a 3/3 never counts.
 */
class SwordOfTheSqueakScenarioTest : ScenarioTestBase() {

    private fun TestGame.answerPendingYes() {
        while (true) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> answerYesNo(true)
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                else -> return
            }
            resolveStack()
        }
    }

    init {
        test("counts creatures with base power or toughness 1, pumped or not") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Centaur Courser") // 3/3 — never counts
                .withCardAttachedTo(1, "Sword of the Squeak", "Centaur Courser")
                .withCardOnBattlefield(1, "Savannah Lions") // 1/1 — counts
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sword = game.findPermanent("Sword of the Squeak")!!
            val courser = game.findPermanent("Centaur Courser")!!
            val lions = game.findPermanent("Savannah Lions")!!

            withClue("one base-1 creature: the equipped 3/3 is a 4/4") {
                game.state.projectedState.getPower(courser) shouldBe 4
                game.state.projectedState.getToughness(courser) shouldBe 4
            }

            // A +1/+1 counter and Giant Growth change the Lions' power, not its base power.
            game.state = game.state.updateEntity(lions) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
            }
            game.castSpell(1, "Giant Growth", targetId = lions).error shouldBe null
            game.resolveStack()
            withClue("the pumped 5/5 Lions still has base power 1, so the bonus holds") {
                game.state.projectedState.getPower(lions) shouldBe 5
                game.state.projectedState.getPower(courser) shouldBe 4
            }
        }

        test("an effect that sets base power and toughness changes who counts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardAttachedTo(1, "Sword of the Squeak", "Centaur Courser")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(1, "Force of Nature")
                .withCardsInHand(1, "Perfected Theory", 2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val courser = game.findPermanent("Centaur Courser")!!
            val lions = game.findPermanent("Savannah Lions")!!
            val force = game.findPermanent("Force of Nature")!!
            game.state.projectedState.getPower(courser) shouldBe 4

            // First mode: "base power and toughness 1/1" — the 8/8 now has base power 1 and counts.
            game.castSpellWithMode(1, "Perfected Theory", 0, force).error shouldBe null
            game.resolveStack()
            withClue("Lions and the shrunken Force of Nature both count: +2/+2") {
                game.state.projectedState.getPower(courser) shouldBe 5
            }

            // Second mode: "base power and toughness 4/5" — the Lions stop counting.
            game.castSpellWithMode(1, "Perfected Theory", 1, lions).error shouldBe null
            game.resolveStack()
            withClue("only Force of Nature is base 1/1 now: +1/+1") {
                game.state.projectedState.getPower(courser) shouldBe 4
            }
        }

        test("a Squirrel entering may take the Sword") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardAttachedTo(1, "Sword of the Squeak", "Centaur Courser")
                .withCardInHand(1, "Bakersbane Duo") // 2/2 Squirrel Raccoon
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sword = game.findPermanent("Sword of the Squeak")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Bakersbane Duo").error shouldBe null
            game.resolveStack()
            game.answerPendingYes()

            val duo = game.findPermanent("Bakersbane Duo")!!
            withClue("the Sword moved onto the entering Squirrel") {
                game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe duo
            }
            withClue("no base-1 creature: the 2/2 Duo gets +0/+0") {
                game.state.projectedState.getPower(duo) shouldBe 2
            }
        }
    }
}
