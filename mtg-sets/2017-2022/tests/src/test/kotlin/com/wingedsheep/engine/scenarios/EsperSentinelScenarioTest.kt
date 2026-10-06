package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Esper Sentinel (MH2 #12) — "Whenever an opponent casts their first noncreature spell each turn,
 * draw a card unless that player pays {X}, where X is this creature's power."
 */
class EsperSentinelScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Sentinel Test Instant") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.GainLife(1) }
        })
        cardRegistry.register(card("Sentinel Test Creature") {
            manaCost = "{0}"
            typeLine = "Creature — Construct"
            power = 1
            toughness = 1
        })

        fun TestGame.hand1() = state.getHand(player1Id).size

        fun board(opponentLands: Int) = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Esper Sentinel")
            .withCardInHand(2, "Sentinel Test Instant")
            .withCardInHand(2, "Sentinel Test Instant")
            .withCardInHand(2, "Sentinel Test Creature")
            .withLandsOnBattlefield(2, "Plains", opponentLands)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("the opponent's first noncreature spell: decline and you draw; a second one doesn't trigger") {
            val game = board(2).build()
            val before = game.hand1()
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.state.stack.size shouldBe 2 // spell + trigger
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.playerId shouldBe game.player2Id
            decision.prompt shouldContain "{1}"
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.hand1() shouldBe before + 1

            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.hand1() shouldBe before + 1
        }

        test("paying {X} stops the draw") {
            val game = board(2).build()
            val before = game.hand1()
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            // Paying opens the mana-source window (CR 605.3a); auto-tap a Plains.
            game.submitManaSourcesAutoPay().error shouldBe null
            game.state.pendingDecision shouldBe null
            game.resolveStack()
            game.hand1() shouldBe before
        }

        test("a creature spell neither triggers it nor uses up the turn's first noncreature spell") {
            val game = board(2).build()
            val before = game.hand1()
            game.castSpell(2, "Sentinel Test Creature").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.hand1() shouldBe before + 1
        }

        test("an opponent who can't pay just lets you draw") {
            val game = board(0).build()
            val before = game.hand1()
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.hand1() shouldBe before + 1
        }

        test("a noncreature spell cast before it entered was already that turn's first") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Esper Sentinel")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(2, "Sentinel Test Instant")
                .withCardInHand(2, "Sentinel Test Instant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // P1 casts Esper Sentinel; P2 responds with a noncreature spell while it is still on the stack.
            game.castSpell(1, "Esper Sentinel").error shouldBe null
            game.passPriority().error shouldBe null
            game.state.priorityPlayerId shouldBe game.player2Id
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.state.stack.size shouldBe 2 // Sentinel isn't on the battlefield yet: no trigger
            game.resolveStack()
            game.isOnBattlefield("Esper Sentinel") shouldBe true

            // Same turn, P2's second noncreature spell isn't their first: no trigger (2021-06-18 ruling).
            game.passPriority().error shouldBe null
            game.state.priorityPlayerId shouldBe game.player2Id
            game.castSpell(2, "Sentinel Test Instant").error shouldBe null
            game.state.stack.size shouldBe 1
        }
    }
}
