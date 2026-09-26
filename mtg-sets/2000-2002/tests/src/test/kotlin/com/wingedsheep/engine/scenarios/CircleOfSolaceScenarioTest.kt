package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Circle of Solace (ONS #13) — "As Circle of Solace enters, choose a creature type. {1}{W}: The next
 * time a creature of the chosen type would deal damage to you this turn, prevent that damage."
 *
 * The shield is a `Matching(Creature.withChosenSubtype())` source filter with `nextInstanceOnly`:
 * the chosen type is bound when the ability resolves, and the first damage instance from a matching
 * creature spends it. Two Prodigal Sorcerers (Human Wizards) ping the Circle's controller in turn,
 * so each instance is its own damage event.
 */
class CircleOfSolaceScenarioTest : ScenarioTestBase() {

    private val circleAbilityId by lazy { cardRegistry.getCard("Circle of Solace")!!.activatedAbilities[0].id }
    private val sorcererAbilityId by lazy { cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id }

    private fun TestGame.chooseType(type: String) {
        val circle = findPermanent("Circle of Solace")!!
        state = state.updateEntity(circle) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    private fun TestGame.activateCircle() {
        val circle = findPermanent("Circle of Solace")!!
        execute(ActivateAbility(playerId = player1Id, sourceId = circle, abilityId = circleAbilityId))
            .error shouldBe null
        resolveStack()
    }

    /** The opponent's Prodigal Sorcerer pings the Circle's controller; they may need priority handed over first. */
    private fun TestGame.ping(sorcerer: EntityId) {
        if (state.priorityPlayerId != player2Id) passPriority().error shouldBe null
        execute(
            ActivateAbility(
                playerId = player2Id,
                sourceId = sorcerer,
                abilityId = sorcererAbilityId,
                targets = listOf(ChosenTarget.Player(player1Id)),
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun newGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Circle of Solace")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
        .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Circle of Solace") {
            test("prevents the next damage from a creature of the chosen type, then is spent") {
                val game = newGame()
                game.chooseType("Wizard")
                game.activateCircle()

                withClue("the shield is visible beside its controller") {
                    game.getClientState(1).players.single { it.playerId == game.player1Id }
                        .activeEffects.any { it.effectId.startsWith("prevent_next_damage_from_") } shouldBe true
                }

                val (first, second) = game.findPermanents("Prodigal Sorcerer")
                val before = game.getLifeTotal(1)
                game.ping(first)
                withClue("the first Wizard's ping is prevented") {
                    game.getLifeTotal(1) shouldBe before
                }
                game.ping(second)
                withClue("the shield covered one instance only — the second ping connects") {
                    game.getLifeTotal(1) shouldBe before - 1
                }
            }

            test("a creature not of the chosen type is unaffected") {
                val game = newGame()
                game.chooseType("Goblin")
                game.activateCircle()

                val before = game.getLifeTotal(1)
                game.ping(game.findPermanents("Prodigal Sorcerer").first())
                withClue("a Wizard is not a Goblin, so its damage is not prevented") {
                    game.getLifeTotal(1) shouldBe before - 1
                }
            }
        }
    }
}
