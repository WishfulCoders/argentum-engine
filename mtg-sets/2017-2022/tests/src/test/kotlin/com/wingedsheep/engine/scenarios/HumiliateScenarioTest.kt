package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Humiliate (STX #193).
 *
 * "{W}{B} Sorcery — Target opponent reveals their hand. You choose a nonland card from it. That
 * player discards that card. Put a +1/+1 counter on a creature you control."
 *
 * Pilfer's reveal-choose-discard, then an untargeted "a creature you control" chosen at
 * resolution, after the hand is revealed (2021-04-16 ruling). The second test pins the companion
 * ruling: with no creature you control, the spell still resolves and simply does nothing for the
 * counter half.
 */
class HumiliateScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Drain the stack, answering any select/target decision Humiliate raises. */
    private fun resolveHumiliate(game: TestGame, chosenDiscard: com.wingedsheep.sdk.model.EntityId?, counterTarget: com.wingedsheep.sdk.model.EntityId?) {
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            when (val decision = game.getPendingDecision()) {
                is SelectCardsDecision -> game.selectCards(if (chosenDiscard != null) listOf(chosenDiscard) else emptyList())
                is ChooseTargetsDecision -> game.selectTargets(if (counterTarget != null) listOf(counterTarget) else emptyList())
                null -> game.passPriority()
                else -> error("unexpected decision: $decision")
            }
        }
    }

    init {
        context("Humiliate") {

            test("the caster chooses a nonland card to discard, then puts a +1/+1 counter on their creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Humiliate")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findPermanent("Grizzly Bears")!!
                val theirGiant = game.findCardsInHand(2, "Hill Giant").single()

                val cast = game.castSpellTargetingPlayer(1, "Humiliate", 2)
                withClue("Casting Humiliate should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()

                resolveHumiliate(game, chosenDiscard = theirGiant, counterTarget = myBears)

                withClue("the chosen nonland card (Hill Giant) is discarded; the Plains stays in hand") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isInHand(2, "Plains") shouldBe true
                }
                withClue("a +1/+1 counter lands on the chosen creature") {
                    plusCounters(game, myBears) shouldBe 1
                }
            }

            test("with no creature you control, the spell still resolves and discards a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Humiliate")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirGiant = game.findCardsInHand(2, "Hill Giant").single()

                val cast = game.castSpellTargetingPlayer(1, "Humiliate", 2)
                withClue("Casting Humiliate should succeed even with no creature you control: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()

                resolveHumiliate(game, chosenDiscard = theirGiant, counterTarget = null)

                withClue("the discard still happens; there was simply no creature to put a counter on") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
