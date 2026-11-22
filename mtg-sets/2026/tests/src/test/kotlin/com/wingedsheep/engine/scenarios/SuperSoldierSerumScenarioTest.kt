package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.msh.cards.SuperSoldierSerum
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Super-Soldier Serum (MSH) — "Enchanted creature gets +2/+2, has first strike and vigilance, and is
 * a legendary Soldier in addition to its other types."
 *
 * Playtesting reported the Soldier type never showing up on the enchanted creature. This asserts the
 * grant all the way out to the DTO the client actually renders, not just the projection, since the
 * type line the player sees is built from `projectedValues` in ClientStateTransformer.
 */
class SuperSoldierSerumScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(SuperSoldierSerum)
        registerCard(Bonesplitter)
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the enchanted creature becomes a legendary Soldier and gets +2/+2") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3 Centaur Warrior

        val aura = d.putCardInHand(d.player1, "Super-Soldier Serum")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, aura, listOf(bear)).outcome shouldBe Outcome.Done
        repeat(6) { if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        val card = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(d.state, d.player1)
            .cards
            .getValue(bear)

        withClue("subtypes=${card.subtypes} typeLine=${card.typeLine}") {
            card.subtypes.contains("Soldier") shouldBe true
            card.typeLine.contains("Soldier") shouldBe true
            card.typeLine.contains("Legendary") shouldBe true
        }
        card.power shouldBe 5
        card.toughness shouldBe 5

        // The battlefield renders the printed image and the preview only prints the type line for
        // tokens, so the grant is only visible to the player through this field.
        card.grantedSubtypes shouldBe setOf("Soldier")
        card.legendaryByEffect shouldBe true

        // grantedSubtypes exists for *static* grants like this Aura. Subtypes added by a floating
        // effect are excluded, because those already carry their own "+X" type-change badge and
        // would otherwise be reported twice in the preview.
        card.activeEffects.none { it.effectId == "type_added" } shouldBe true
    }

    /** Casts the Serum from [player]'s hand onto [host] and resolves it. */
    fun GameTestDriver.enchant(player: EntityId, host: EntityId) {
        val aura = putCardInHand(player, "Super-Soldier Serum")
        giveMana(player, Color.WHITE, 2)
        castSpell(player, aura, listOf(host)).outcome shouldBe Outcome.Done
        bothPass()
    }

    /** Answers the trigger's "any number of target Equipment" prompt with [equipment], then resolves it. */
    fun GameTestDriver.attachOnTrigger(player: EntityId, equipment: EntityId) {
        withClue("the Aura's trigger asks its controller for Equipment") {
            pendingDecision?.playerId shouldBe player
        }
        submitTargetSelection(player, listOf(equipment))
        bothPass()
    }

    fun GameTestDriver.attachedTo(entity: EntityId): EntityId? = state.getEntity(entity)?.get<AttachedToComponent>()?.targetId

    test("whenever enchanted creature attacks, the Aura's controller attaches their Equipment to it") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        val sword = d.putPermanentOnBattlefield(d.player1, "Bonesplitter")
        d.enchant(d.player1, courser)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2)
        d.attachOnTrigger(d.player1, sword)

        d.attachedTo(sword) shouldBe courser
    }

    test("whenever enchanted creature blocks, the trigger fires from the Aura too") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(attacker)
        val blocker = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val sword = d.putPermanentOnBattlefield(d.player2, "Bonesplitter")
        // The Serum is the defending player's, so put it straight onto the battlefield attached
        // rather than casting it on the attacker's turn.
        val aura = d.putPermanentOnBattlefield(d.player2, "Super-Soldier Serum")
        d.addComponent(aura, AttachedToComponent(blocker))
        d.addComponent(blocker, AttachmentsComponent(listOf(aura)))

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2)
        d.bothPass()
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker)))
        d.attachOnTrigger(d.player2, sword)

        d.attachedTo(sword) shouldBe blocker
    }

    test("the trigger is the Aura's: its controller attaches their own Equipment to an opponent's creature") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.removeSummoningSickness(theirs)
        val mySword = d.putPermanentOnBattlefield(d.player1, "Bonesplitter")
        d.enchant(d.player1, theirs)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, listOf(theirs), d.player1)
        d.attachOnTrigger(d.player1, mySword)

        withClue("player1's Bonesplitter is now on player2's creature") {
            d.attachedTo(mySword) shouldBe theirs
        }
    }
})
