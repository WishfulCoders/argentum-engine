package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiDecisionInsight
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * What the evaluator cannot see about mana, measured rather than argued.
 *
 * `BoardPresence` prices a land `LAND_UNTAPPED` = 0.6 or `LAND_TAPPED` = 0.3 and nothing else about
 * it; `Tempo` counts lands and ignores tapped state; `RawBoardFeatures` carries
 * `landCountDifference` and `untappedLandDifference`. **No term anywhere reads a colour.** So a play
 * whose whole point is fixing pays the 0.3 (weight 1.5 → **0.45**) and is credited with nothing, and
 * a greedy evaluator declines it in every position.
 *
 * These are readings off the real decision path — the scores the AI actually assigned, captured
 * through [com.wingedsheep.ai.insight.AiInsightSink] — not a re-derivation. They pin today's wrong
 * behaviour on purpose, the way `PuzzleSuiteTest.KNOWN_FAILURES` does: when the evaluator learns
 * about colour, this test fails and says so. Its puzzle-suite counterparts are `activate-07` and
 * `sequencing-09`; the write-up is mtg-draft-ai `docs/46`.
 */
class FixingBlindnessTest : ScenarioTestBase() {

    init {
        // ── The activation: cracking a fetch land ──

        test("cracking Evolving Wilds loses to passing by the tapped-land charge, in every position") {
            // The positions differ in everything the evaluator could plausibly key on: what is in
            // hand, whether it is castable, how many lands are out, how many cards are stranded.
            val positions = mapOf(
                "a green card stranded in hand" to Position(lands = 2, hand = listOf("Grizzly Bears")),
                "nothing in hand at all" to Position(lands = 2, hand = emptyList()),
                "a castable card in hand" to Position(lands = 2, hand = listOf("Goblin Piker")),
                "mana screwed" to Position(lands = 1, hand = listOf("Grizzly Bears")),
                "flooded" to Position(lands = 5, hand = listOf("Grizzly Bears")),
                "two cards stranded" to Position(lands = 2, hand = listOf("Grizzly Bears", "Craw Wurm")),
            )
            for ((label, position) in positions) {
                withClue(label) {
                    val insight = position.decide()
                    val pass = insight.options.single { it.baseline }
                    val crack = insight.options.single { it.actionType == "ActivateAbility" }

                    // Not merely "worse" — worse by a constant, which is what makes it a blind spot
                    // rather than a judgement the position could have talked it out of.
                    (crack.score!! - pass.score!!) shouldBe (-TAPPED_LAND_CHARGE plusOrMinus 1e-9)
                    insight.chosenLabel shouldBe "Pass priority"
                }
            }
        }

        test("the basic it would fetch is chosen by library order, not by the colour it needs") {
            // Grizzly Bears is {1}{G} and there is no green source in play, so the Forest is the
            // only card in the library that changes anything. `DecisionResponder.contextualCardScore`
            // scores a land purely by how many lands are already out, so every basic ties and the
            // stable sort takes the first — which makes the fetch's whole purpose a coin flip on
            // deck order.
            for (library in listOf(listOf("Swamp", "Forest"), listOf("Forest", "Swamp"))) {
                withClue("library order $library") {
                    Position(lands = 2, hand = listOf("Grizzly Bears"), library = library)
                        .fetchedBasic() shouldBe library.first()
                }
            }
        }

        test("an optional search with exactly one legal card takes it, on every profile") {
            // A separate defect, found while building the position above: `respondSelectCards`
            // routed "search for **up to** one" (min 0, max 1) through its `max < options.size`
            // branch, which is false when the library holds exactly one legal card, and fell to
            // the `else` — which takes `min`, i.e. zero. The AI declined its own search and had
            // sacrificed the land for nothing, which made the activation score −2.10 rather than
            // −0.45. Fixed on every profile rather than behind a flag: declining a free search is
            // not a tuning choice.
            for (profile in listOf(AiProfile.PRODUCTION, FIXING)) {
                withClue(profile.id) {
                    Position(
                        lands = 2, hand = listOf("Grizzly Bears"),
                        library = listOf("Forest"), profile = profile,
                    ).fetchedBasic() shouldBe "Forest"
                }
            }
        }

        // ── The land drop: the same charge, one zone over ──

        test("it plays the basic over the tapland that is its only source of the colour it needs") {
            val insight = Position(
                lands = 2,
                hand = listOf("Grizzly Bears", "Shivan Oasis", "Mountain"),
            ).decide()
            val basic = insight.options.single { it.isPlayOf("Mountain") }
            val tapland = insight.options.single { it.isPlayOf("Shivan Oasis") }

            // Shivan Oasis is "{T}: Add {R} or {G}", enters tapped. No Mountain will ever cast a
            // {1}{G} creature, so this is not the curve trade-off `sequencing-07`/`-08` pin — there
            // is no setting of the constant that gets this right, because the constant is about
            // tapped-ness and the question is about colour.
            (tapland.score!! - basic.score!!) shouldBe (-TAPPED_LAND_CHARGE plusOrMinus 1e-9)
            insight.chosenLabel shouldBe "Play Mountain"
        }

        test("where the two lands tie, which one casts your hand is decided by hand order") {
            // Deathcap Glade is a slow land — with two other lands out it enters *untapped*, so the
            // one constant that tells lands apart does not apply and the scores are exactly equal.
            // The AI then takes whichever came first in hand: right in one ordering, wrong in the
            // other, for no reason it could state.
            val lands = listOf("Deathcap Glade", "Mountain")
            val chosen = lands.map { first ->
                val insight = Position(
                    lands = 2,
                    hand = listOf("Grizzly Bears", first) + lands.filter { it != first },
                ).decide()
                withClue("$first first") {
                    insight.options.single { it.isPlayOf("Deathcap Glade") }.score!! shouldBe
                        (insight.options.single { it.isPlayOf("Mountain") }.score!! plusOrMinus 1e-9)
                }
                insight.chosenLabel
            }
            chosen shouldBe listOf("Play Deathcap Glade", "Play Mountain")
        }
        // ── With `fixing` on, all four are the other way round ──

        test("`fixing`: it cracks the fetch land, and by more when the fetch actually fixes") {
            // The constant is gone. Cracking is worth the tapped basic it brings — the land it ate
            // was worth nothing as a permanent — plus, when the basic supplies a colour the hand
            // needs and the board cannot make, the `UNMET_COLOUR` charge it lifts.
            val land = TAPPED_LAND_CHARGE                       // 0.3 x weight 1.5
            val colour = 0.75 * 1.5                             // UNMET_COLOUR x weight 1.5
            val positions = mapOf(
                "a green card stranded in hand" to (listOf("Grizzly Bears") to land + colour),
                "two cards stranded" to (listOf("Grizzly Bears", "Craw Wurm") to land + colour),
                "mana screwed" to (listOf("Grizzly Bears") to land + colour),
                "nothing in hand at all" to (emptyList<String>() to land),
                "a castable card in hand" to (listOf("Goblin Piker") to land),
            )
            for ((label, expectation) in positions) {
                val (hand, gain) = expectation
                withClue(label) {
                    val insight = Position(lands = 2, hand = hand, profile = FIXING).decide()
                    val pass = insight.options.single { it.baseline }
                    val crack = insight.options.single { it.actionType == "ActivateAbility" }
                    (crack.score!! - pass.score!!) shouldBe (gain plusOrMinus 1e-9)
                    insight.chosenLabel shouldContain "Sacrifice"
                }
            }
        }

        test("`fixing`: it fetches the colour its hand needs, whatever the library order") {
            for (library in listOf(listOf("Swamp", "Forest"), listOf("Forest", "Swamp"))) {
                withClue("library order $library") {
                    Position(
                        lands = 2, hand = listOf("Grizzly Bears"),
                        library = library, profile = FIXING,
                    ).fetchedBasic() shouldBe "Forest"
                }
            }
        }

        test("`fixing`: with no colour missing, it takes the colour the deck leans on") {
            // Two Mountains out, and a hand of {2}{R} plus Shivan Dragon's {4}{R}{R}: red is
            // already available, so the "fixes something held" clause is silent for both options.
            // The Swamp is a colour this board cannot make — and nothing wants, which is why the
            // missing-colour clause must not fire for it. Three red pips do want a third Mountain.
            Position(
                lands = 2, hand = listOf("Goblin Piker", "Shivan Dragon"),
                library = listOf("Swamp", "Mountain"), profile = FIXING,
            ).fetchedBasic() shouldBe "Mountain"
        }

        test("`fixing`: it plays the tapland that is its only source of the colour it needs") {
            val insight = Position(
                lands = 2,
                hand = listOf("Grizzly Bears", "Shivan Oasis", "Mountain"),
                profile = FIXING,
                fetchLand = false,
            ).decide()
            val basic = insight.options.single { it.isPlayOf("Mountain") }
            val tapland = insight.options.single { it.isPlayOf("Shivan Oasis") }

            // The Oasis removes the unmet-green charge and the Mountain does not, so it wins by
            // `UNMET_COLOUR` 0.75 less the 0.3 it gives up for entering tapped, at weight 1.5.
            (tapland.score!! - basic.score!!) shouldBe (0.675 plusOrMinus 1e-9)
            insight.chosenLabel shouldBe "Play Shivan Oasis"
        }

        test("`fixing`: a single-colour position is untouched, so sequencing-07/-08 cannot move") {
            // Nothing in hand needs a colour the board cannot make, so the charge is zero for
            // every candidate and the land drop is decided by exactly what decided it before.
            val insight = Position(
                lands = 2,
                hand = listOf("Goblin Piker", "Shivan Oasis", "Mountain"),
                profile = FIXING,
                fetchLand = false,
            ).decide()
            val basic = insight.options.single { it.isPlayOf("Mountain") }
            val tapland = insight.options.single { it.isPlayOf("Shivan Oasis") }
            (tapland.score!! - basic.score!!) shouldBe (-TAPPED_LAND_CHARGE plusOrMinus 1e-9)
        }

        test("`seqc`: the tapland refund fires when the card it would unlock is the wrong colour") {
            // Two Mountains, one of them tapped, and Grizzly Bears ({1}{G}) in hand. Untapping the
            // second Mountain brings mana value 2 within reach, so upstream's test says "a card
            // would become castable" and withholds the refund — but no arrangement of Mountains
            // casts a green spell, so the tapped land cost this turn nothing at all. The spare
            // Mountain in hand is only there to give the position a second legal action, without
            // which the Strategist short-circuits and records no scores to read.
            val seqc = AiProfile.PRODUCTION.copy(id = "seqc", sequenceLandsByCastability = true)
            val seq = AiProfile.PRODUCTION.copy(id = "seq", sequenceLandsByUsableMana = true)

            fun refundShowsIn(profile: AiProfile): Double {
                val insight = Position(
                    lands = 1, hand = listOf("Grizzly Bears", "Mountain"), profile = profile,
                    fetchLand = false, extraTapped = "Mountain",
                ).decide()
                return insight.options.single { it.baseline }.score!!
            }

            // The refund is worth `tapped x IDLE_MANA_REFUND` = 0.3, at weight 1.5.
            (refundShowsIn(seqc) - refundShowsIn(seq)) shouldBe (0.45 plusOrMinus 1e-9)
        }

        test("`fixing`: a land in hand already covers its colour, so the fetch looks elsewhere") {
            // Holding a Forest, the green card in hand is not what the fetch is for; the Swamp is
            // the colour this board genuinely cannot reach.
            Position(
                lands = 2, hand = listOf("Grizzly Bears", "Forest", "Bog Imp"),
                library = listOf("Forest", "Swamp"), profile = FIXING,
            ).fetchedBasic() shouldBe "Swamp"
        }
    }

    // ── Harness ──

    private fun AiActionOption.isPlayOf(name: String) = label == "Play $name"

    private inner class Position(
        val lands: Int,
        val hand: List<String>,
        val library: List<String> = listOf("Forest", "Swamp"),
        val profile: AiProfile = AiProfile.PRODUCTION,
        /** Off for the land-drop positions: with a fetch on the battlefield, cracking is better. */
        val fetchLand: Boolean = true,
        /** A land that starts the position tapped, for the sequencing refund. */
        val extraTapped: String? = null,
    ) {
        private fun game(): ScenarioTestBase.TestGame {
            var builder: ScenarioTestBase.ScenarioBuilder = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Mountain", lands)
                .withLandsOnBattlefield(2, "Plains", 2)
            if (fetchLand) builder = builder.withCardOnBattlefield(1, "Evolving Wilds", tapped = false)
            if (extraTapped != null) builder = builder.withCardOnBattlefield(1, extraTapped, tapped = true)
            hand.forEach { builder = builder.withCardInHand(1, it) }
            library.forEach { builder = builder.withCardInLibrary(1, it) }
            // Deep enough that nothing here is playing a decking race, and deliberately not a
            // basic: a land-only library makes every simulated draw a land, which `CardAdvantage`
            // prices differently. Same reasoning as `PuzzleRunner.stockLibraries`.
            repeat(20) { builder = builder.withCardInLibrary(1, "Craw Wurm") }
            repeat(20) { builder = builder.withCardInLibrary(2, "Craw Wurm") }
            return builder
                .withRngSeed(SEED)
                .withTurnNumber(4)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()
        }

        /** The scores the AI actually assigned at this position's own priority window. */
        fun decide(): AiDecisionInsight {
            val game = game()
            val captured = mutableListOf<AiDecisionInsight>()
            AIPlayer.create(cardRegistry, game.player1Id, profile) { _, insight ->
                captured += insight
            }.chooseAction(game.state)
            return captured.last()
        }

        /** Which basic the AI takes once the activation has been made for it, or [NOTHING]. */
        fun fetchedBasic(): String {
            val game = game()
            val ai = AIPlayer.create(cardRegistry, game.player1Id, profile)
            // Not `single { it.action is ActivateAbility }`: full enumeration also offers every
            // Mountain's mana ability.
            val activation = LegalActionEnumerator.create(cardRegistry)
                .enumerate(game.state, game.player1Id)
                .single { action ->
                    val source = (action.action as? ActivateAbility)?.sourceId
                    source != null && nameOf(game.state, source) == "Evolving Wilds"
                }
            game.execute(activation.action).error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as SelectCardsDecision
            val response = ai.respondToDecision(game.state, decision) as CardsSelectedResponse
            return response.selectedCards.singleOrNull()?.let { nameOf(game.state, it) } ?: "nothing"
        }
    }

    private fun nameOf(state: GameState, id: EntityId): String =
        state.getEntity(id)?.get<CardComponent>()?.name ?: id.value

    private companion object {
        /** `EvaluationWeights.boardPresence` × (`LAND_UNTAPPED` − `LAND_TAPPED`) = 1.5 × 0.3. */
        const val TAPPED_LAND_CHARGE = 0.45

        /** Same date-stamp convention as `PuzzleRunner.PUZZLE_SEED` and `ArenaConfig.DEFAULT_SEED`. */
        const val SEED = 20260919L

        /** `docs/46`'s fix, on top of the same frozen baseline the readings above are taken on. */
        val FIXING = AiProfile.PRODUCTION.copy(
            id = "production-fixing",
            priceSacrificeLandsAsNoMana = true,
            choosesLandsByColour = true,
            chargesForUnavailableColours = true,
        )
    }
}
