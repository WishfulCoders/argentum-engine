package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dragon's Rage Channeler (MH2 #121) — {R} Creature — Human Shaman 1/1.
 *
 *   Whenever you cast a noncreature spell, surveil 1.
 *   Delirium — As long as there are four or more card types among cards in your graveyard, this
 *   creature gets +2/+2, has flying, and attacks each combat if able.
 */
class DragonsRageChannelerScenarioTest : ScenarioTestBase() {

    init {
        context("Dragon's Rage Channeler") {

            test("surveiling a land into a three-type graveyard turns on delirium") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragon's Rage Channeler", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Divination")
                    .withCardInLibrary(1, "Mountain") // surveil fodder (top card)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val drc = game.findPermanent("Dragon's Rage Channeler")!!

                game.state.projectedState.getPower(drc) shouldBe 1
                game.state.projectedState.hasKeyword(drc, Keyword.FLYING) shouldBe false

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(decision.options) // Mountain into the graveyard
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack() // Lightning Bolt resolves → instant joins the graveyard

                withClue("creature, sorcery, land, instant = delirium") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                    game.state.projectedState.getPower(drc) shouldBe 3
                    game.state.projectedState.getToughness(drc) shouldBe 3
                    game.state.projectedState.hasKeyword(drc, Keyword.FLYING) shouldBe true
                }
            }

            test("with delirium it must attack; without it, it may stay home") {
                val delirious = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragon's Rage Channeler", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Divination")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                withClue("an empty attack is illegal while DRC must attack") {
                    delirious.declareAttackers(emptyMap()).error shouldNotBe null
                }
                delirious.declareAttackers(mapOf("Dragon's Rage Channeler" to 2)).error shouldBe null

                val calm = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragon's Rage Channeler", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                calm.declareAttackers(emptyMap()).error shouldBe null
            }

            test("a creature spell does not trigger surveil") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragon's Rage Channeler", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
