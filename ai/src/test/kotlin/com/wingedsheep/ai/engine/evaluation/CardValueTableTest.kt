package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.training.ApprenticeArtifact
import com.wingedsheep.ai.training.ApprenticeArtifactLoader
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import kotlinx.serialization.json.Json

/**
 * The card term of the fitted correction (mtg-draft-ai `docs/40`).
 *
 * What is pinned here is the *contract with the fit*: which zones are summed, with which sign, under
 * which names — `fit_cards.py`'s `zone_counts` and `PreferenceWriter.cards` — because a mismatch
 * would not fail anything, it would quietly score positions the model was never fitted on.
 */
class CardValueTableTest : ScenarioTestBase() {

    init {
        test("the term is the signed sum of the five zones the fit was shown") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Goblin Piker")
                .withCardInHand(1, "Craw Wurm")
                .withCardInGraveyard(1, "Bog Imp")
                .withCardInGraveyard(2, "Grizzly Bears")
                .build()

            // battlefield: +bears -piker; graveyard: +imp -bears; hand: +wurm.
            table.score(game.state, game.state.projectedState, game.player1Id) shouldBe
                ((1.0 - 10.0) + (0.02 - 0.03) + 100.0 plusOrMinus 1e-9)
            // The same position from the other side is the negation on the differenced zones, and
            // that side's own hand rather than ours.
            table.score(game.state, game.state.projectedState, game.player2Id) shouldBe
                ((10.0 - 1.0) + (0.03 - 0.02) + 0.0 plusOrMinus 1e-9)
        }

        test("the opponent's hand and both libraries are never read") {
            // `PrefCards` does not record them, so a table that scored them would be scoring
            // information the model was never fitted on — and, in a real game, cheating. The same
            // card in our own hand is the control: it is worth 100, so a leak could not read as 0.
            fun score(hidden: Boolean): Double {
                var builder = scenario().withPlayers().withCardInHand(1, "Bog Imp")
                if (hidden) {
                    builder = builder.withCardInHand(2, "Craw Wurm")
                        .withCardInLibrary(1, "Craw Wurm")
                        .withCardInLibrary(2, "Craw Wurm")
                }
                val game = builder.build()
                return table.score(game.state, game.state.projectedState, game.player1Id)
            }
            score(hidden = true) shouldBe (score(hidden = false) plusOrMinus 1e-9)
            score(hidden = true) shouldBe (0.0 plusOrMinus 1e-9)   // Bog Imp is a graveyard card only

            // ...while the acting side's own hand is a level, which is what the fit put there.
            val mine = scenario().withPlayers().withCardInHand(1, "Craw Wurm").build()
            table.score(mine.state, mine.state.projectedState, mine.player1Id) shouldBe
                (100.0 plusOrMinus 1e-9)
        }

        test("copies count, and a card the fit never saw is worth nothing") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Shivan Dragon")
                .build()

            table.score(game.state, game.state.projectedState, game.player1Id) shouldBe (2.0 plusOrMinus 1e-9)
        }

        test("the correction carries the card term, and is still 0 once the game is over") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Grizzly Bears").build()
            val weights = RawEvaluationWeights(
                intercept = 0.0, weights = RawBoardFeatures.names.associateWith { 0.0 }, cards = table,
            )
            weights.isValid() shouldBe true
            val correction = weights.toCorrection(IntentCatalog.NONE)
            correction.evaluate(game.state, game.state.projectedState, game.player1Id) shouldBe
                (1.0 plusOrMinus 1e-9)

            val over = game.state.copy(gameOver = true, winnerId = game.player1Id)
            correction.evaluate(over, over.projectedState, game.player1Id) shouldBe 0.0
        }

        // ── The artifact ──

        test("a schema-2 artifact round-trips through the loader with its table") {
            val artifact = ApprenticeArtifact(
                modelId = "card-text",
                featureNames = RawBoardFeatures.names.toList(),
                sharedCoefficients = List(RawBoardFeatures.names.size) { 0.0 },
                cardZones = CardValueTable.ZONES,
                cardValues = mapOf("Grizzly Bears" to listOf(1.0, 0.0, -2.0)),
            )
            artifact.validationErrors() shouldBe emptyList()
            val text = Json.encodeToString(ApprenticeArtifact.serializer(), artifact)
            ApprenticeArtifactLoader.decodeOrNull(text) shouldBe artifact
            artifact.toEvaluationWeights(applyOverlay = false).cards shouldBe artifact.cardTable()
        }

        test("a schema-1 artifact still loads, and carries no table") {
            val text = """
                {"schemaVersion":1,"modelId":"rc2","setCode":null,
                 "featureNames":${Json.encodeToString(RawBoardFeatures.names.toList())},
                 "sharedCoefficients":${Json.encodeToString(List(RawBoardFeatures.names.size) { 0.0 })},
                 "setOverlayCoefficients":[],"intercept":0.0}
            """.trimIndent()
            val artifact = ApprenticeArtifactLoader.decodeOrNull(text)
            artifact?.cardTable() shouldBe null
            artifact?.toEvaluationWeights(applyOverlay = false)?.cards shouldBe null
        }

        test("a table whose rows do not match the zones is rejected rather than half-read") {
            val base = ApprenticeArtifact(
                modelId = "card-text",
                featureNames = RawBoardFeatures.names.toList(),
                sharedCoefficients = List(RawBoardFeatures.names.size) { 0.0 },
                cardZones = CardValueTable.ZONES,
                cardValues = mapOf("Grizzly Bears" to listOf(1.0, 0.0, -2.0)),
            )
            base.copy(cardValues = mapOf("Grizzly Bears" to listOf(1.0, 0.0)))
                .validationErrors() shouldContain "malformed card value"
            base.copy(cardValues = mapOf("Grizzly Bears" to listOf(1.0, 0.0, Double.NaN)))
                .validationErrors() shouldContain "malformed card value"
            base.copy(cardZones = listOf("battlefield", "hand", "graveyard"))
                .validationErrors() shouldContain "card zone schema mismatch"
            base.copy(schemaVersion = 1).validationErrors() shouldContain "card values need schema 2"
            base.copy(cardValues = emptyMap()).validationErrors() shouldContain "card zones without card values"
        }
    }

    /** Deliberately different magnitudes per zone, so a zone read in the wrong slot cannot pass. */
    private val table = CardValueTable(
        zones = CardValueTable.ZONES,
        values = mapOf(
            "Grizzly Bears" to listOf(1.0, 0.03, 0.0),
            "Goblin Piker" to listOf(10.0, 0.0, 0.0),
            "Bog Imp" to listOf(0.0, 0.02, 0.0),
            "Craw Wurm" to listOf(0.0, 0.0, 100.0),
        ),
    )
}
