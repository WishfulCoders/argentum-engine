package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Disruption Protocol (NEO #51).
 *
 * {U}{U} Instant
 * As an additional cost to cast this spell, tap an untapped artifact you control or pay {1}.
 * Counter target spell.
 *
 * Exercises the tap leg of [com.wingedsheep.sdk.scripting.AdditionalCost.OrPay] — the first
 * printed card to put a tap-permanents cost on that leg — against the pay-{1} path.
 */
class DisruptionProtocolScenarioTest : ScenarioTestBase() {

    init {
        context("tap-an-artifact-or-pay-{1} additional cost") {

            test("tap path: taps an untapped artifact, costs only {U}{U}, and counters the spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Disruption Protocol")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Island", 2) // exactly {U}{U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val actions = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Disruption Protocol") }
                withClue("The tap path is offered while an untapped artifact is available") {
                    actions.any { it.description.endsWith("(Tap)") } shouldBe true
                }

                val protocolId = game.findCardsInHand(1, "Disruption Protocol").first()
                val bearsId = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val ornithopterId = game.findPermanent("Ornithopter")!!

                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        protocolId,
                        listOf(ChosenTarget.Spell(bearsId)),
                        additionalCostPayment = AdditionalCostPayment(tappedPermanents = listOf(ornithopterId)),
                    )
                )
                withClue("Disruption Protocol casts via the tap path with only two Islands: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("The artifact is tapped to pay the additional cost") {
                    game.state.getEntity(ornithopterId)?.has<TappedComponent>() shouldBe true
                }

                game.resolveStack()

                withClue("Grizzly Bears is countered") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("pay path: with no artifact, costs {1}{U}{U} and counters the spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Disruption Protocol")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val actions = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Disruption Protocol") }
                withClue("Disruption Protocol is castable via the pay path") {
                    actions.isNotEmpty() shouldBe true
                }
                withClue("No tap path is offered without an artifact") {
                    actions.none { it.description.endsWith("(Tap)") } shouldBe true
                }

                val result = game.castSpellTargetingStackSpell(1, "Disruption Protocol", "Grizzly Bears")
                withClue("Disruption Protocol casts via the pay path: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("All three Islands were tapped for {1}{U}{U}") {
                    game.findPermanents("Island").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }

                game.resolveStack()

                withClue("Grizzly Bears is countered") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("without an artifact, two Islands can't pay {1}{U}{U}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Disruption Protocol")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                withClue("No Disruption Protocol cast is offered") {
                    game.getLegalActions(1).none { it.description.startsWith("Cast Disruption Protocol") } shouldBe true
                }
                val result = game.castSpellTargetingStackSpell(1, "Disruption Protocol", "Grizzly Bears")
                withClue("Casting without the artifact and without {1} extra fails") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
