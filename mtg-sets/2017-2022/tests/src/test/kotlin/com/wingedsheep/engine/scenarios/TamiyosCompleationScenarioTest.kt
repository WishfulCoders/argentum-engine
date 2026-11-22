package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tamiyo's Compleation (NEO #83) — {3}{U} Enchantment — Aura.
 *
 * "Flash
 *  Enchant artifact, creature, or planeswalker
 *  When this Aura enters, tap enchanted permanent. If it's an Equipment, unattach it.
 *  Enchanted permanent loses all abilities and doesn't untap during its controller's untap step."
 *
 * The Equipment rider is the composition worth proving: the enter trigger re-reads the enchanted
 * permanent, taps it, and unattaches it from the creature it equips.
 */
class TamiyosCompleationScenarioTest : ScenarioTestBase() {

    init {
        context("Tamiyo's Compleation") {

            test("on an attached Equipment: taps it and unattaches it from the equipped creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Tamiyo's Compleation")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Bonesplitter", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                withClue("control: Bonesplitter gives the Bears +2/+0") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                }

                game.castSpell(1, "Tamiyo's Compleation", bonesplitter).error shouldBe null
                game.resolveStack()

                val aura = game.findPermanent("Tamiyo's Compleation")!!
                withClue("the Aura is attached to the Equipment") {
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()?.targetId shouldBe bonesplitter
                }
                withClue("the Equipment was tapped") {
                    game.state.getEntity(bonesplitter)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the Equipment was unattached from the Bears") {
                    game.state.getEntity(bonesplitter)!!.get<AttachedToComponent>() shouldBe null
                    (game.state.getEntity(bears)!!.get<AttachmentsComponent>()?.attachedIds ?: emptyList())
                        .contains(bonesplitter) shouldBe false
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("on a creature: taps it, it stays attached, and it doesn't untap") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Tamiyo's Compleation")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Tamiyo's Compleation", bears).error shouldBe null
                game.resolveStack()

                val aura = game.findPermanent("Tamiyo's Compleation")!!
                withClue("the Aura stays on the non-Equipment creature, which is tapped") {
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("it's Bob's turn and the Bears stayed tapped") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
