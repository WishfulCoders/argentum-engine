package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.predicates.isModified
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A permanent that *becomes* an Aura (CR 613.1d, layer 4) with a gained enchant ability — the
 * Necromancy shape: "When this enchantment enters, if it's on the battlefield, it becomes an Aura
 * with 'enchant creature put onto the battlefield with [this].' Put target creature card from a
 * graveyard onto the battlefield under your control and attach this enchantment to it."
 *
 * Rules pinned here:
 *  - Until the ability resolves it is an ordinary enchantment: the Aura state-based actions don't
 *    touch it (CR 704.5m reads Auras only).
 *  - Once it is an Aura it follows the Aura rules (Necromancy ruling 2005-08-01): attached to the
 *    returned creature it stays; with nothing it can enchant (protection from black) it is an
 *    unattached Aura and goes to the graveyard (CR 303.4c / 704.5m), and its leaves trigger
 *    sacrifices the creature (Necromancy ruling 2008-04-01).
 *  - The creature it enchants counts as enchanted by an Aura its controller controls (modified,
 *    CR 700.9).
 *  - A target that left the graveyard makes the ability do nothing (CR 608.2b): the enchantment
 *    never becomes an Aura and stays on the battlefield.
 */
class PermanentBecomesAuraTest : ScenarioTestBase() {

    init {
        cardRegistry.register(card("Becomes Aura Test") {
            manaCost = "{2}{B}"
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.self.enters()
                interveningIf = Conditions.SourceInZone(Zone.BATTLEFIELD)
                target(TargetFilter.CreatureInGraveyard)
                effect = Effects.Pipeline {
                    run(Effects.AddSubtype("Aura", EffectTarget.Self, Duration.Permanent))
                    val returned = gather(CardSource.ChosenTargets)
                    move(returned, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                    run(Effects.EnchantPutOntoBattlefield(returned))
                    run(
                        Effects.CreateDelayedTrigger(
                            trigger = Triggers.self.leaves(),
                            watchedTarget = EffectTarget.Self,
                            fireOnce = true,
                            expiry = DelayedTriggerExpiry.Never,
                            carryCollections = listOf(returned.key),
                            effect = Effects.SacrificeTarget(returned.asTarget, sacrificedByItsController = true)
                        )
                    )
                }
            }
        })

        fun board(creature: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Becomes Aura Test")
            .withCardInHand(1, "Disenchant")
            .withCardInGraveyard(2, creature)
            .withCardOnBattlefield(2, "Tormod's Crypt")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** Cast it and let it resolve; its enters trigger is put on the stack targeting [card]. */
        fun TestGame.castTargeting(card: EntityId) {
            castSpell(1, "Becomes Aura Test").error shouldBe null
            passPriority()
            passPriority()
            if (state.pendingDecision != null) selectTargets(listOf(card)).error shouldBe null
            state.stack.size shouldBe 1
        }

        test("an ordinary enchantment until the ability resolves, then an Aura attached to the creature it returned") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castTargeting(giant)

            val enchantment = game.findPermanent("Becomes Aura Test")!!
            game.state.projectedState.hasSubtype(enchantment, "Aura") shouldBe false
            game.state.getEntity(enchantment)?.get<AttachedToComponent>() shouldBe null

            game.resolveStack()
            game.isOnBattlefield("Becomes Aura Test") shouldBe true
            game.state.projectedState.hasSubtype(enchantment, "Aura") shouldBe true
            game.state.getEntity(enchantment)?.get<GainedEnchantRestrictionComponent>() shouldNotBe null
            game.state.getEntity(enchantment)?.get<AttachedToComponent>()?.targetId shouldBe giant
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            // Enchanted by an Aura its controller controls (CR 700.9).
            isModified(game.state, giant) { game.state.projectedState.getController(it) } shouldBe true

            // Still attached after state-based actions are checked again.
            game.checkStateBasedActions()
            game.isOnBattlefield("Becomes Aura Test") shouldBe true
        }

        test("when it leaves the battlefield the creature's controller sacrifices it") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castTargeting(giant)
            game.resolveStack()

            game.castSpell(1, "Disenchant", game.findPermanent("Becomes Aura Test")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Becomes Aura Test") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("with nothing it can enchant it is an unattached Aura: graveyard, and the creature is sacrificed") {
            val game = board("White Knight") // protection from black
            val knight = game.state.getGraveyard(game.player2Id).single()
            game.castTargeting(knight)
            game.resolveStack()

            game.isInGraveyard(1, "Becomes Aura Test") shouldBe true
            game.isOnBattlefield("White Knight") shouldBe false
            game.isInGraveyard(2, "White Knight") shouldBe true
        }

        test("the target leaving the graveyard in response: nothing happens and it stays an ordinary enchantment") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castTargeting(giant)

            game.passPriority() // player 1 passes with the enters trigger on the stack
            game.execute(ActivateAbility(
                playerId = game.player2Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player2Id))
            )).error shouldBe null
            game.resolveStack()

            val enchantment = game.findPermanent("Becomes Aura Test")!!
            game.isInExile(2, "Hill Giant") shouldBe true
            game.state.projectedState.hasSubtype(enchantment, "Aura") shouldBe false
            game.checkStateBasedActions()
            game.isOnBattlefield("Becomes Aura Test") shouldBe true
        }
    }
}
