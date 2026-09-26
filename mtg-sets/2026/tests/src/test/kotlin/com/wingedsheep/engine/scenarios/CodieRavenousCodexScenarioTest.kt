package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fra.cards.CodieRavenousCodex
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Codie, Ravenous Codex (FRA #168) — {3} Legendary Artifact Creature — Book Construct 1/4.
 *
 * "Whenever you cast a prepared spell, copy it. You may choose new targets for the copy.
 *  {W}{U}{B}{R}{G}, {T}: Each creature you control becomes prepared."
 *
 * Encouraging Aviator is the preparation creature: it doesn't enter prepared, and its prepare
 * spell is Jump ({U} instant, "Target creature gains flying until end of turn").
 */
class CodieRavenousCodexScenarioTest : ScenarioTestBase() {

    private val prepareAbility = CodieRavenousCodex.activatedAbilities.single().id

    private fun TestGame.isPrepared(id: EntityId) = state.getEntity(id)?.get<PreparedComponent>() != null

    private fun TestGame.jumpCopy(): EntityId? = state.getExile(player1Id).firstOrNull { id ->
        val e = state.getEntity(id)
        e?.get<CardComponent>()?.name == "Encouraging Aviator" && e.get<PreparedSpellCopyComponent>() != null
    }

    private fun TestGame.autoPay() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun TestGame.activatePrepareAll() {
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Codie, Ravenous Codex")!!,
                abilityId = prepareAbility,
            )
        ).error shouldBe null
        autoPay()
        resolveStack()
    }

    private fun builder(): ScenarioBuilder {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Codie, Ravenous Codex", summoningSickness = false)
            .withCardOnBattlefield(1, "Encouraging Aviator", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Island", 2)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { b = b.withCardInLibrary(1, "Forest") }
        repeat(5) { b = b.withCardInLibrary(2, "Forest") }
        return b
    }

    init {
        test("{W}{U}{B}{R}{G}, {T}: each creature you control with a prepare spell becomes prepared") {
            val game = builder().build()
            val aviator = game.findPermanent("Encouraging Aviator")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val codie = game.findPermanent("Codie, Ravenous Codex")!!
            game.isPrepared(aviator) shouldBe false

            game.activatePrepareAll()

            withClue("the preparation creature becomes prepared and its Jump copy waits in exile") {
                game.isPrepared(aviator) shouldBe true
                game.jumpCopy() shouldNotBe null
            }
            withClue("creatures without a prepare spell can't become prepared") {
                game.isPrepared(bears) shouldBe false
                game.isPrepared(codie) shouldBe false
            }
        }

        test("casting a prepared spell copies it, and the copy may take a new target") {
            val game = builder().build()
            val aviator = game.findPermanent("Encouraging Aviator")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.activatePrepareAll()
            val jump = game.jumpCopy()!!

            game.execute(
                CastSpell(game.player1Id, jump, targets = listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)
            ).error shouldBe null
            game.autoPay()

            withClue("Codie's trigger goes on the stack above the prepared spell") {
                game.state.stack.size shouldBe 2
            }

            // Resolve the trigger; answer the copy's "choose new targets" by pointing it at the Aviator.
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                when (val decision = game.getPendingDecision()) {
                    is YesNoDecision -> game.answerYesNo(true)
                    is ChooseTargetsDecision -> game.selectTargets(listOf(aviator))
                    null -> game.resolveStack()
                    else -> error("unexpected decision $decision")
                }
            }

            withClue("the original Jump gives the Bears flying") {
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }
            withClue("the retargeted copy gives the Aviator flying") {
                game.state.projectedState.hasKeyword(aviator, Keyword.FLYING) shouldBe true
            }
            withClue("casting the prepare spell unprepared the Aviator") {
                game.isPrepared(aviator) shouldBe false
            }
        }

        test("casting a preparation creature or an ordinary spell is not a prepared spell") {
            val game = builder()
                .withCardInHand(1, "Encouraging Aviator")
                .withCardInHand(1, "Shock")
                .build()

            game.castSpell(1, "Encouraging Aviator").error shouldBe null
            game.autoPay()
            withClue("the creature half on the stack alone — no Codie trigger") {
                game.state.stack.size shouldBe 1
            }
            game.resolveStack()

            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.autoPay()
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
