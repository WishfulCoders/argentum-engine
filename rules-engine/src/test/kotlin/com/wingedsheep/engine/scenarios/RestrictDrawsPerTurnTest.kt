package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictDrawsPerTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** "Each opponent can't draw more than one card each turn." — Narset, Parter of Veils' static. */
private val DrawWarden = card("Test Draw Warden") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Each opponent can't draw more than one card each turn."
    staticAbility { ability = RestrictDrawsPerTurn(maxPerTurn = 1, affected = Player.EachOpponent) }
    metadata { rarity = Rarity.RARE; collectorNumber = "T1" }
}

/** "Players can't draw cards." — the blanket `maxPerTurn = 0` form. */
private val DrawLock = card("Test Draw Lock") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Each player can't draw cards."
    staticAbility { ability = RestrictDrawsPerTurn(maxPerTurn = 0, affected = Player.Each) }
    metadata { rarity = Rarity.RARE; collectorNumber = "T2" }
}

private val MayDrawTwo = card("Test May Draw Two") {
    manaCost = "{U}"
    typeLine = "Sorcery"
    oracleText = "You may draw two cards."
    spell { effect = Effects.May(Effects.DrawCards(2)) }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T3" }
}

private val MayDrawOne = card("Test May Draw One") {
    manaCost = "{U}"
    typeLine = "Sorcery"
    oracleText = "You may draw a card."
    spell { effect = Effects.May(Effects.DrawCards(1)) }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T4" }
}

private val DrawUpToThree = card("Test Draw Up To Three") {
    manaCost = "{U}"
    typeLine = "Sorcery"
    oracleText = "Draw up to three cards."
    spell { effect = Effects.DrawUpTo(3) }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T5" }
}

/**
 * [RestrictDrawsPerTurn] — a per-turn draw cap, which is a "can't" effect (CR 101.2, CR 614.17)
 * rather than a replacement effect. Each test pins one rule or ruling of Narset, Parter of Veils /
 * Leovold, Emissary of Trest.
 */
class RestrictDrawsPerTurnTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(DrawWarden, DrawLock, MayDrawTwo, MayDrawOne, DrawUpToThree))

        fun base() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun drawnThisTurn(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId) =
            game.state.getEntity(playerId)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0

        test("CR 121.2: an opponent told to draw two draws only one; the rest is ignored") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 1
            game.librarySize(2) shouldBe 3
            drawnThisTurn(game, game.player2Id) shouldBe 1
        }

        test("the cap does not bind its controller") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .apply { repeat(4) { withCardInLibrary(1, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
        }

        test("draws made earlier in the turn count, even before the cap existed (Narset ruling)") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardsDrawnThisTurn(2, 1)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 0
            game.librarySize(2) shouldBe 4
        }

        test("CR 121.4: a forbidden draw is not an attempt to draw from an empty library") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardsDrawnThisTurn(2, 1)
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            withClue("Player 2's library is empty, but the draws never happened") {
                game.state.getEntity(game.player2Id)?.has<AttemptedDrawFromEmptyLibraryComponent>() shouldBe false
            }
        }

        test("CR 614.17c: a draw the cap forbids can't be replaced (no dredge offered)") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardsDrawnThisTurn(2, 1)
                .withCardInGraveyard(2, "Golgari Thug")
                .apply { repeat(6) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Golgari Thug") shouldBe true
            game.librarySize(2) shouldBe 6
        }

        test("a replaced draw never happened, so it doesn't use up the allowance (Narset ruling)") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInGraveyard(2, "Golgari Thug")
                .apply { repeat(6) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            // First draw: Player 2 dredges instead (the draw doesn't happen) ...
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            // ... so the second draw is their first real one this turn, and it happens.
            game.isInHand(2, "Golgari Thug") shouldBe true
            game.findCardsInGraveyard(2, "Forest").size shouldBe 4
            game.handSize(2) shouldBe 2
            drawnThisTurn(game, game.player2Id) shouldBe 1
        }

        test("CR 121.3: an optional draw the player couldn't fully perform can't be chosen") {
            val game = base()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(2, "Test May Draw Two")
                .withLandsOnBattlefield(2, "Island", 1)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpell(2, "Test May Draw Two").error shouldBe null
            game.resolveStack()

            withClue("\"you may draw two cards\" with one draw left is not offered at all") {
                game.state.pendingDecision shouldBe null
            }
            game.handSize(2) shouldBe 0
        }

        test("an optional draw within the allowance is still offered") {
            val game = base()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(2, "Test May Draw One")
                .withLandsOnBattlefield(2, "Island", 1)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpell(2, "Test May Draw One").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.handSize(2) shouldBe 1
        }

        test("\"draw up to N\" offers at most the remaining allowance") {
            val game = base()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardInHand(2, "Test Draw Up To Three")
                .withLandsOnBattlefield(2, "Island", 1)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpell(2, "Test Draw Up To Three").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseNumberDecision>()
            decision.maxValue shouldBe 1
        }

        test("the smallest of several caps applies, and maxPerTurn = 0 forbids every draw") {
            val game = base()
                .withCardOnBattlefield(1, "Test Draw Warden")
                .withCardOnBattlefield(2, "Test Draw Lock")
                .withCardsInHand(1, "Inspiration", 2)
                .withLandsOnBattlefield(1, "Island", 8)
                .apply { repeat(4) { withCardInLibrary(1, "Forest") } }
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
            game.resolveStack()
            withClue("the Each/0 lock binds the lock's controller's opponent and the Warden's controller alike") {
                game.librarySize(1) shouldBe 4
            }

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            withClue("0 is smaller than the Warden's 1") { game.librarySize(2) shouldBe 4 }
        }
    }
}
