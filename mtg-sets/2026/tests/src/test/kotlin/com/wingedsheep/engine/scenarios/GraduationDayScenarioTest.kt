package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Graduation Day (SOS #16, {W} Enchantment).
 *
 *   Repartee — Whenever you cast an instant or sorcery spell that targets a creature,
 *   put a +1/+1 counter on target creature you control.
 *
 * "A creature" names no zone, so it means a creature permanent (CR 109.2). Raise Dead aimed at a
 * creature *card* in a graveyard must not trigger Repartee — the bug this file pins.
 */
class GraduationDayScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, entityId: EntityId): Int =
        game.state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Graduation Day") {

            test("an instant that targets a creature puts a +1/+1 counter on target creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Graduation Day")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                withClue("Repartee triggers and asks for its target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                counters(game, bears) shouldBe 1
            }

            test("a sorcery that targets a creature card in a graveyard does not trigger Repartee") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Graduation Day")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Hill Giant").error shouldBe null
                withClue("no Repartee trigger — a creature card is not a creature") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()

                withClue("Raise Dead returned Hill Giant") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                }
                counters(game, bears) shouldBe 0
            }
        }
    }
}
