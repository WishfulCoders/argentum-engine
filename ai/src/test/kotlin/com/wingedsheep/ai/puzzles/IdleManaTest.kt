package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.spendIdleManaAtSorcerySpeed]: a card-neutral sorcery is cast in the last sorcery-speed window of
 * the turn, and only there, and not when the mana could still pay for an instant.
 *
 * Kept out of [PuzzleCatalog]: the catalog is scored against [AiProfile.PRODUCTION], which has the flag off.
 */
class IdleManaTest : ScenarioTestBase() {

    init {
        val runner = PuzzleRunner(cardRegistry) { scenario() }
        val idle = AiProfile.CURRENT.copy(id = "idle", useCardIntent = true, spendIdleManaAtSorcerySpeed = 1.0)

        fun sleightAt(step: Step, vararg extraHand: String) = { scenario: ScenarioBuilder ->
            var b = scenario.withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Sleight of Hand")
            for (card in extraHand) b = b.withCardInHand(1, card)
            b.build().advanceToPriority(1, step)
        }

        fun puzzle(id: String, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) =
            AiPuzzle(id, PuzzleCategory.PRIORITY_TIMING, id, aiSeat = 1, position = position, check = check)

        test("the last sorcery-speed window casts a card-neutral sorcery") {
            val result = runner.run(
                puzzle("idle-01", sleightAt(Step.POSTCOMBAT_MAIN)) { shouldCast("Sleight of Hand") }, idle,
            )
            withClue(result.move) { result.failure shouldBe null }
        }

        test("without the allowance the same position passes: a card for a card ties passing") {
            val result = runner.run(
                puzzle("idle-01-off", sleightAt(Step.POSTCOMBAT_MAIN)) { shouldPass() },
                idle.copy(spendIdleManaAtSorcerySpeed = 0.0),
            )
            withClue(result.move) { result.failure shouldBe null }
        }

        test("the precombat main phase is not the last window, so the allowance does not apply there") {
            val base = runner.run(puzzle("idle-02-base", sleightAt(Step.PRECOMBAT_MAIN)) {}, idle.copy(spendIdleManaAtSorcerySpeed = 0.0))
            val result = runner.run(puzzle("idle-02", sleightAt(Step.PRECOMBAT_MAIN)) {}, idle)
            result.move shouldBe base.move
        }

        test("an instant that could still use the mana turns the allowance off") {
            val result = runner.run(
                puzzle("idle-03", sleightAt(Step.POSTCOMBAT_MAIN, "Opt")) { shouldNotCast("Sleight of Hand") }, idle,
            )
            withClue(result.move) { result.failure shouldBe null }
        }
    }
}
