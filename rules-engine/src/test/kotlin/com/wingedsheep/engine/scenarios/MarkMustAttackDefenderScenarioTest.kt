package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.combat.MustAttackDefenderComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AttackRequirementWindow
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.MarkMustAttackDefenderEffect] — Gideon,
 * Battle-Forged's "+2: Up to one target creature an opponent controls attacks Gideon during its
 * controller's next turn if able", on an inline planeswalker.
 *
 * Pins, against CR 508.1d and the Gideon rulings (2015-06-22): during its controller's next turn the
 * creature must attack the planeswalker — attacking the player instead, or not attacking, is illegal,
 * and the enumerator lists it as a mandatory attacker; the requirement waits through the rest of the
 * activating player's turn and is gone after the turn it applied to; a tapped creature is not able to
 * attack, so the requirement imposes nothing; and if the planeswalker has left the battlefield the
 * creature may attack anyone or not at all.
 */
class MarkMustAttackDefenderScenarioTest : ScenarioTestBase() {

    private val walker = card("Taunting Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = "+2: Up to one target creature an opponent controls attacks Taunting Walker during its controller's next turn if able."
        loyaltyAbility(+2) {
            val creature = target(TargetFilter.CreatureOpponentControls, optional = true)
            effect = Effects.MarkMustAttackDefender(creature, EffectTarget.Self, AttackRequirementWindow.CONTROLLERS_NEXT_TURN)
        }
    }

    private val thisTurnWalker = card("Siren Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = "You may activate this permanent's loyalty abilities any time you could cast an instant.\n+1: Target creature attacks you this turn if able."
        staticAbility { ability = com.wingedsheep.sdk.scripting.LoyaltyAbilitiesAtInstantSpeed }
        loyaltyAbility(+1) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.MarkMustAttackDefender(creature, EffectTarget.Controller, AttackRequirementWindow.THIS_TURN)
        }
    }

    private fun board(creatureTapped: Boolean = false) = scenario()
        .withPlayers("Gideon", "Opponent")
        .withCardOnBattlefield(1, "Taunting Walker")
        .withCardOnBattlefield(2, "Centaur Courser", tapped = creatureTapped)
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activatePlusTwo() {
        val walkerId = findPermanent("Taunting Walker")!!
        val ability = cardRegistry.getCard("Taunting Walker")!!.script.activatedAbilities
            .single { it.cost is AbilityCost.Loyalty }
        execute(
            ActivateAbility(
                player1Id, walkerId, ability.id,
                targets = listOf(ChosenTarget.Permanent(findPermanent("Centaur Courser")!!))
            )
        ).error shouldBe null
        resolveStack()
    }

    /** From player 1's main phase to the opponent's declare-attackers step. */
    private fun TestGame.toOpponentsAttack() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        state.activePlayerId shouldBe player2Id
    }

    private fun TestGame.mandatoryAttackers() =
        getLegalActions(2).firstNotNullOfOrNull { info ->
            info.mandatoryAttackers?.takeIf { info.action is DeclareAttackers }
        }.orEmpty()

    init {
        cardRegistry.register(walker)
        cardRegistry.register(thisTurnWalker)

        test("during its controller's next turn the creature must attack the planeswalker") {
            val game = board()
            game.activatePlusTwo()
            val courser = game.findPermanent("Centaur Courser")!!
            withClue("the requirement waits for the creature controller's turn") {
                game.state.getEntity(courser)!!.get<MustAttackDefenderComponent>()!!
                    .requirements.single().activeOnTurn shouldBe null
            }

            game.toOpponentsAttack()
            game.mandatoryAttackers() shouldContain courser
            game.mandatoryAttackers() shouldNotContain game.findPermanent("Savannah Lions")!!

            withClue("not attacking breaks the requirement") {
                game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
            }
            withClue("attacking the player instead of the planeswalker breaks it") {
                game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldNotBe null
            }
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Centaur Courser" to "Taunting Walker")
            ).error shouldBe null
        }

        test("the requirement ends with the turn it applied to") {
            val game = board()
            game.activatePlusTwo()
            game.toOpponentsAttack()
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Centaur Courser" to "Taunting Walker")
            ).error shouldBe null
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(game.findPermanent("Centaur Courser")!!)!!
                .has<MustAttackDefenderComponent>() shouldBe false
        }

        test("a tapped creature isn't able to attack, so the requirement imposes nothing") {
            val game = board()
            game.activatePlusTwo()
            game.toOpponentsAttack()
            // Tap it as its controller's declare-attackers step begins.
            val courser = game.findPermanent("Centaur Courser")!!
            game.state = game.state.updateEntity(courser) {
                it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
            }
            game.mandatoryAttackers() shouldNotContain courser
            game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldBe null
        }

        test("if the planeswalker left the battlefield, the creature may attack anyone or nothing") {
            val game = board()
            game.activatePlusTwo()
            game.toOpponentsAttack()
            val walkerId = game.findPermanent("Taunting Walker")!!
            game.state = game.state
                .removeFromZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD),
                    walkerId
                )
                .addToZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD),
                    walkerId
                )
            game.mandatoryAttackers() shouldNotContain game.findPermanent("Centaur Courser")!!
            game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
        }

        test("THIS_TURN with the controller as defender is in force at once") {
            // Activated at instant speed on the opponent's turn: "target creature attacks you this
            // turn if able" binds the opponent's creature for the combat still to come.
            val game = scenario()
                .withPlayers("Siren", "Opponent")
                .withCardOnBattlefield(1, "Siren Walker")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val courser = game.findPermanent("Centaur Courser")!!
            val ability = cardRegistry.getCard("Siren Walker")!!.script.activatedAbilities
                .single { it.cost is AbilityCost.Loyalty }
            game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Siren Walker")!!, ability.id,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(courser)!!.get<MustAttackDefenderComponent>()!!
                .requirements.single().activeOnTurn shouldBe game.state.turnNumber

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player2Id
            game.mandatoryAttackers() shouldContain courser
            withClue("attacking the planeswalker instead of its controller breaks the requirement") {
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Centaur Courser" to "Siren Walker")
                ).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
        }
    }
}
