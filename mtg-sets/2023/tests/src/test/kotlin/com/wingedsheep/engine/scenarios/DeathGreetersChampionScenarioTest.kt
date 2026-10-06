package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DashedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Death-Greeter's Champion (MOC #30) — {2}{R} 2/1. Dash {3}{R}; backup 1; double strike.
 *
 * The composition worth proving: a dashed cast still fires the backup enters trigger, the other
 * creature gains double strike (not haste — dash is printed above backup), and the dash delayed
 * trigger returns the Champion at the next end step while the counter stays on the target.
 */
class DeathGreetersChampionScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Death-Greeter's Champion") {

            test("dashed: backup grants double strike to another creature, champion returns at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death-Greeter's Champion")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithAlternativeCost(1, "Death-Greeter's Champion").error shouldBe null
                game.resolveStack()
                withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                val champion = game.findPermanent("Death-Greeter's Champion")!!
                val projected = game.state.projectedState
                withClue("dashed champion has haste") {
                    game.state.getEntity(champion)?.has<DashedComponent>() shouldBe true
                    projected.hasKeyword(champion, Keyword.HASTE) shouldBe true
                }
                withClue("bears get the counter and double strike, but not haste") {
                    plusOnes(game, bears) shouldBe 1
                    projected.getPower(bears) shouldBe 3
                    projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("dash returns the champion to hand; the counter stays on the bears") {
                    game.isOnBattlefield("Death-Greeter's Champion") shouldBe false
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Death-Greeter's Champion"
                    } shouldBe true
                    plusOnes(game, bears) shouldBe 1
                }
            }

            test("normal cast, backup on itself: a 3/2 double striker without haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death-Greeter's Champion")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Death-Greeter's Champion").error shouldBe null
                game.resolveStack()
                val champion = game.findPermanent("Death-Greeter's Champion")!!
                game.selectTargets(listOf(champion)).error shouldBe null
                game.resolveStack()

                plusOnes(game, champion) shouldBe 1
                val projected = game.state.projectedState
                projected.getPower(champion) shouldBe 3
                projected.hasKeyword(champion, Keyword.DOUBLE_STRIKE) shouldBe true
                projected.hasKeyword(champion, Keyword.HASTE) shouldBe false
            }
        }
    }
}
