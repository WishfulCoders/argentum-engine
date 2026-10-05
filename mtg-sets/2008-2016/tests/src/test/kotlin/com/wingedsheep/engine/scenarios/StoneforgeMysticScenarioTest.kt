package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Stoneforge Mystic (WWK #20) — {1}{W} Creature — Kor Artificer 1/2.
 *
 * When this creature enters, you may search your library for an Equipment card, reveal it, put it
 * into your hand, then shuffle.
 * {1}{W}, {T}: You may put an Equipment card from your hand onto the battlefield.
 */
class StoneforgeMysticScenarioTest : ScenarioTestBase() {

    init {
        context("Stoneforge Mystic") {

            test("entering searches for an Equipment and puts it into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stoneforge Mystic")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Bonesplitter")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stoneforge Mystic").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val bonesplitter = game.findCardsInLibrary(1, "Bonesplitter").single()
                withClue("only the Equipment is offered") {
                    decision.options shouldBe listOf(bonesplitter)
                }
                game.selectCards(listOf(bonesplitter))
                game.resolveStack()

                game.isInHand(1, "Bonesplitter") shouldBe true
                game.isOnBattlefield("Bonesplitter") shouldBe false
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }

            test("{1}{W}, {T}: puts an Equipment from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stoneforge Mystic", summoningSickness = false)
                    .withCardInHand(1, "Bonesplitter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Stoneforge Mystic")!!
                val abilityId = cardRegistry.getCard("Stoneforge Mystic")!!.activatedAbilities.first().id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = mystic, abilityId = abilityId)
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val bonesplitter = game.findCardsInHand(1, "Bonesplitter").single()
                withClue("only the Equipment in hand is offered") {
                    decision.options shouldBe listOf(bonesplitter)
                }
                game.selectCards(listOf(bonesplitter))
                game.resolveStack()

                game.isOnBattlefield("Bonesplitter") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
