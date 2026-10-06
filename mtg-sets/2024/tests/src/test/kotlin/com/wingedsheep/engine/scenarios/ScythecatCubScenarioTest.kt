package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scythecat Cub (J25 #24) — "Landfall — Whenever a land you control enters, put a +1/+1 counter on
 * target creature you control. If this is the second time this ability has resolved this turn,
 * double the number of +1/+1 counters on that creature instead."
 *
 * Azusa, Lost but Seeking gives three land drops in one main phase, so one turn sees the first,
 * second and third resolution: +1, doubled (and no extra +1 — "instead"), then +1 again.
 */
class ScythecatCubScenarioTest : ScenarioTestBase() {

    init {
        fun plusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun addPlusOne(game: TestGame, id: EntityId, count: Int) {
            game.state = game.state.updateEntity(id) { container ->
                val existing = container.get<CountersComponent>() ?: CountersComponent()
                container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
            }
        }

        fun game() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Scythecat Cub")
            .withCardOnBattlefield(1, "Azusa, Lost but Seeking")
            .withCardsInHand(1, "Forest", 3)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** Play a Forest and resolve the landfall trigger aimed at [target]. */
        fun landfallOn(game: TestGame, target: EntityId) {
            val forest = game.findCardsInHand(1, "Forest").first()
            game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(target)).error shouldBe null
            }
            game.resolveStack()
        }

        test("first resolution adds a counter, the second doubles instead, the third adds one again") {
            val game = game()
            val cub = game.findPermanent("Scythecat Cub")!!
            addPlusOne(game, cub, 3)

            landfallOn(game, cub)
            withClue("first resolution: +1 (3 → 4)") { plusOne(game, cub) shouldBe 4 }

            landfallOn(game, cub)
            withClue("second resolution: doubled, with no extra +1 (4 → 8)") { plusOne(game, cub) shouldBe 8 }

            landfallOn(game, cub)
            withClue("third resolution: back to +1 (8 → 9)") { plusOne(game, cub) shouldBe 9 }
        }

        test("the second resolution doubles the counters on the creature it targets") {
            val game = game()
            val cub = game.findPermanent("Scythecat Cub")!!
            val azusa = game.findPermanent("Azusa, Lost but Seeking")!!
            addPlusOne(game, azusa, 2)

            landfallOn(game, cub)
            plusOne(game, cub) shouldBe 1

            landfallOn(game, azusa)
            withClue("Azusa's two counters double to four; the Cub is untouched") {
                plusOne(game, azusa) shouldBe 4
                plusOne(game, cub) shouldBe 1
            }
        }

        test("doubling a creature with no +1/+1 counters puts none on it") {
            val game = game()
            val cub = game.findPermanent("Scythecat Cub")!!
            val azusa = game.findPermanent("Azusa, Lost but Seeking")!!

            landfallOn(game, cub)
            landfallOn(game, azusa)

            plusOne(game, azusa) shouldBe 0
            plusOne(game, cub) shouldBe 1
        }
    }
}
