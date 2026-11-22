package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Lily Bowen, Raging Grandma — the upkeep trigger checks her power as it resolves: 16 or less
 * doubles her +1/+1 counters; above 16 strips all but one and gains life for each one removed.
 */
class LilyBowenRagingGrandmaScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun upkeepWith(counters: Int): Pair<TestGame, EntityId> {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Lily Bowen, Raging Grandma")
            .withLifeTotal(1, 20)
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()
        val lily = game.findPermanent("Lily Bowen, Raging Grandma")!!
        game.state = game.state.updateEntity(lily) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to counters)))
        }
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game to lily
    }

    init {
        test("power 8 doubles to sixteen +1/+1 counters") {
            val (game, lily) = upkeepWith(8)
            game.plusOnes(lily) shouldBe 16
            game.getLifeTotal(1) shouldBe 20
        }

        test("power exactly 16 still doubles") {
            val (game, lily) = upkeepWith(16)
            game.plusOnes(lily) shouldBe 32
            game.getLifeTotal(1) shouldBe 20
        }

        test("power 17 removes all but one counter and gains 1 life per counter removed") {
            val (game, lily) = upkeepWith(17)
            game.plusOnes(lily) shouldBe 1
            game.getLifeTotal(1) shouldBe 36
        }
    }
}
