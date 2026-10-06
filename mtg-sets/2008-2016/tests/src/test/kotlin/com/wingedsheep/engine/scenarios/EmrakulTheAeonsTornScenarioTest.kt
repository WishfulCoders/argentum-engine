package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Emrakul, the Aeons Torn (ROE #4) — {15} Legendary Creature — Eldrazi 15/15.
 *
 * Pins the cast trigger (extra turn, resolving above the uncounterable spell), protection from
 * spells that are one or more colors as printed on a real card, the hand-lowered annihilator 6,
 * and the graveyard-from-anywhere shuffle.
 */
class EmrakulTheAeonsTornScenarioTest : ScenarioTestBase() {
    private val emrakul = "Emrakul, the Aeons Torn"

    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("casting it takes an extra turn; the trigger sits above Emrakul and a counterspell can't stop it") {
            val game = base().withCardInHand(1, emrakul)
                .withLandsOnBattlefield(1, "Plains", 15)
                .withCardInHand(2, "Counterspell")
                .withLandsOnBattlefield(2, "Island", 2).build()
            game.castSpell(1, emrakul).error shouldBe null
            withClue("Emrakul plus its cast trigger") { game.state.stack.size shouldBe 2 }
            game.passPriority().error shouldBe null
            withClue("a counterspell may target it (ruling), it just does nothing") {
                game.castSpellTargetingStackSpell(2, "Counterspell", emrakul).error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield(emrakul) shouldBe true
            withClue("P1 takes the extra turn, so P2 skips its next one") {
                game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
            }
        }

        test("a colored spell can't target it") {
            val game = base().withCardOnBattlefield(2, emrakul)
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1).build()
            val target = game.findPermanent(emrakul)!!
            game.castSpell(1, "Lightning Bolt", target).error shouldNotBe null
        }

        test("annihilator 6: the defending player sacrifices six permanents of their choice") {
            val game = base().withCardOnBattlefield(1, emrakul, summoningSickness = false)
                .withLandsOnBattlefield(2, "Forest", 7)
                .withCardInLibrary(2, "Forest").build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(emrakul to 2)).error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player2Id
            val forests = game.findAllPermanents("Forest")
            game.selectCards(forests.take(6)).error shouldBe null
            game.findAllPermanents("Forest").size shouldBe 1
        }

        test("put into a graveyard from anywhere — discarded — its owner shuffles their graveyard into their library") {
            val game = base().withCardInHand(1, emrakul)
                .withCardInHand(1, "Mind Rot")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 3).build()
            game.castSpellTargetingPlayer(1, "Mind Rot", 1).error shouldBe null
            game.resolveStack()
            game.graveyardSize(1) shouldBe 0
            game.findCardsInLibrary(1, emrakul).size shouldBe 1
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInLibrary(1, "Mind Rot").size shouldBe 1
        }

        test("its owner's graveyard is shuffled, not the graveyard of the player who killed it") {
            // Wrath of God doesn't target and deals no damage, so protection doesn't stop it.
            val game = base().withCardOnBattlefield(2, emrakul)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInHand(1, "Wrath of God")
                .withLandsOnBattlefield(1, "Plains", 4).build()
            game.castSpell(1, "Wrath of God").error shouldBe null
            game.resolveStack()
            game.findCardsInLibrary(2, emrakul).size shouldBe 1
            game.findCardsInLibrary(2, "Grizzly Bears").size shouldBe 1
            game.graveyardSize(2) shouldBe 0
            withClue("the Wrath caster's graveyard is untouched") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Wrath of God") shouldBe true
            }
        }
    }
}
