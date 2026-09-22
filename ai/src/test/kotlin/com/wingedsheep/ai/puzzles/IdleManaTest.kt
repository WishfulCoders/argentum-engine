package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.spendIdleManaAtSorcerySpeed]: a card-neutral sorcery is cast in the last sorcery-speed window of
 * the turn, and only there, and not with the mana a held instant needs; and [AiProfile.spendIdleManaInTheirEndStep],
 * its instant-speed mirror in the opponent's end step.
 *
 * Kept out of [PuzzleCatalog]: the catalog is scored against [AiProfile.PRODUCTION], which has the flag off.
 */
class IdleManaTest : ScenarioTestBase() {

    init {
        val runner = PuzzleRunner(cardRegistry) { scenario() }
        val idle = AiProfile.CURRENT.copy(id = "idle", useCardIntent = true, spendIdleManaAtSorcerySpeed = 1.0)

        fun sleightAt(step: Step, vararg extraHand: String, islands: Int = 2) = { scenario: ScenarioBuilder ->
            var b = scenario.withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", islands)
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

        test("the mana a held instant needs is not spent") {
            val result = runner.run(
                puzzle("idle-03", sleightAt(Step.POSTCOMBAT_MAIN, "Opt", islands = 1)) { shouldNotCast("Sleight of Hand") },
                idle,
            )
            withClue(result.move) { result.failure shouldBe null }
        }

        // AiProfile.spendIdleManaInTheirEndStep: timing-05's position, with the upstream cantrip bonus off so the
        // allowance is the only thing that can cast the Opt.
        val eot = AiProfile.CURRENT.copy(id = "eot", useCardIntent = true, spendIdleManaInTheirEndStep = 3.0)

        fun theirEndStep(vararg hand: String) = { scenario: ScenarioBuilder ->
            var b = scenario.withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
            for (card in hand) b = b.withCardInHand(1, card)
            b.build().advanceToPriority(1, Step.END)
        }

        test("the opponent's end step casts a card-neutral instant, and passes without the allowance") {
            val on = runner.run(puzzle("eot-01", theirEndStep("Opt")) { shouldCast("Opt") }, eot)
            withClue(on.move) { on.failure shouldBe null }
            val off = runner.run(
                puzzle("eot-01-off", theirEndStep("Opt")) { shouldPass() }, eot.copy(spendIdleManaInTheirEndStep = 0.0),
            )
            withClue(off.move) { off.failure shouldBe null }
        }

        test("a pump the hold policy floors in the end step stays in hand") {
            val result = runner.run(puzzle("eot-02", theirEndStep("Giant Growth")) { shouldNotCast("Giant Growth") }, eot)
            withClue(result.move) { result.failure shouldBe null }
        }

        // The 2026-09-22 play session: the pilot held Midnight Tilling for the whole game, through turns with six
        // lands open and nothing else to cast. Mill four and return one is card-neutral, so it tied passing and lost.
        // The pilot's own token stack (less the fitted correction, which needs its artifact).
        val pilot = com.wingedsheep.ai.engine.profileFromTokens("raceclock+timing+determinize+fixing+grants+locked")

        fun tillingInTheirEndStep() = { scenario: ScenarioBuilder ->
            var b = scenario.withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Midnight Tilling")
            for (card in listOf("Forest", "Lys Alana Informant", "Forest", "Moonglove Extractor", "Forest", "Forest")) {
                b = b.withCardInLibrary(1, card)
            }
            b.build().advanceToPriority(1, Step.END)
        }

        test("the pilot casts Midnight Tilling in the opponent's end step with idle and eot, and holds it without") {
            val withRules = com.wingedsheep.ai.engine.profileFromTokens("raceclock+timing+determinize+fixing+grants+locked+idle+eot")
            val on = runner.run(puzzle("eot-tilling", tillingInTheirEndStep()) { shouldCast("Midnight Tilling") }, withRules)
            withClue(on.move) { on.failure shouldBe null }
            val off = runner.run(puzzle("eot-tilling-off", tillingInTheirEndStep()) { shouldPass() }, pilot)
            withClue(off.move) { off.failure shouldBe null }
        }

        test("mana beyond what a held instant needs is spent") {
            val result = runner.run(
                puzzle("idle-04", sleightAt(Step.POSTCOMBAT_MAIN, "Opt", islands = 2)) { shouldCast("Sleight of Hand") },
                idle,
            )
            withClue(result.move) { result.failure shouldBe null }
        }
    }
}
