package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Inventory Management (PIP #105) — "Split second. For each Aura and Equipment you control, you may
 * attach it to a creature you control."
 *
 * Each Aura/Equipment gets its own "may", and only creatures you control that it could legally be
 * attached to are offered (CR 701.3a) — never the one it's already on.
 */
class InventoryManagementScenarioTest : ScenarioTestBase() {

    private fun TestGame.hostOf(id: EntityId) = state.getEntity(id)!!.get<AttachedToComponent>()?.targetId

    /**
     * Answer every prompt the spell raises: [answers] says, per attachment, which host to move it to
     * (null = decline the "may"). Returns the legal hosts offered for each accepted attachment.
     */
    private fun TestGame.answer(answers: Map<EntityId, EntityId?>): Map<EntityId, List<EntityId>> {
        val offered = mutableMapOf<EntityId, List<EntityId>>()
        var current: EntityId? = null
        var guard = 0
        while (true) {
            val decision = getPendingDecision() ?: break
            check(guard++ < 10) { "too many decisions" }
            when (decision) {
                is YesNoDecision -> {
                    current = decision.context.subjectEntityId
                    withClue("each prompt names the Aura or Equipment it is about") {
                        (current in answers) shouldBe true
                    }
                    answerYesNo(answers[current] != null).error shouldBe null
                }
                is ChooseTargetsDecision -> {
                    val attachment = current!!
                    offered[attachment] = decision.legalTargets[0]!!
                    selectTargets(listOf(answers[attachment]!!)).error shouldBe null
                }
                else -> error("unexpected decision $decision")
            }
        }
        resolveStack()
        return offered
    }

    init {
        test("moves each Aura and Equipment to a creature you control it can legally be attached to") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                .withCardOnBattlefield(1, "Black Knight")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(1, "Inventory Management")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val holyStrength = game.findPermanent("Holy Strength")!!
            val bonesplitter = game.findPermanent("Bonesplitter")!!
            val knight = game.findPermanent("Black Knight")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Inventory Management").error shouldBe null
            game.resolveStack()
            val offered = game.answer(mapOf(holyStrength to giant, bonesplitter to knight))

            withClue("Holy Strength: not its current host, not pro-white Black Knight, not Bob's Wurm") {
                offered[holyStrength]!!.shouldContainExactlyInAnyOrder(giant)
            }
            withClue("Bonesplitter is colourless, so Black Knight can carry it") {
                offered[bonesplitter]!!.shouldContainExactlyInAnyOrder(knight, giant)
            }
            game.hostOf(holyStrength) shouldBe giant
            game.hostOf(bonesplitter) shouldBe knight
            game.state.projectedState.getPower(giant) shouldBe 4
            game.state.projectedState.getPower(knight) shouldBe 4
        }

        test("declining leaves an Aura in place, and an unattached Equipment can be attached") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Inventory Management")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val holyStrength = game.findPermanent("Holy Strength")!!
            val bonesplitter = game.findPermanent("Bonesplitter")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Inventory Management").error shouldBe null
            game.resolveStack()
            val offered = game.answer(mapOf(holyStrength to null, bonesplitter to bears))

            withClue("an unattached Equipment may go to any creature you control") {
                offered[bonesplitter]!!.shouldContainExactlyInAnyOrder(bears, giant)
            }
            game.hostOf(holyStrength) shouldBe bears
            game.hostOf(bonesplitter) shouldBe bears
            game.getPendingDecision() shouldBe null
        }
    }
}
