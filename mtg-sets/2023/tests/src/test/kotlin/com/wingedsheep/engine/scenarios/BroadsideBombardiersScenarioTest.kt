package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lcc.cards.BroadsideBombardiers
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Broadside Bombardiers (LCC #54) — {2}{R} Creature — Goblin Pirate 2/2.
 *
 *   Menace, haste
 *   Boast — Sacrifice another creature or artifact: This creature deals damage equal to 2 plus the
 *   sacrificed permanent's mana value to any target.
 *
 * Pins the wiring: the boast is offered only after the Bombardiers attacked, the damage reads the
 * sacrificed permanent's mana value through last-known information, and the once-each-turn clause
 * removes the ability after one activation.
 */
class BroadsideBombardiersScenarioTest : ScenarioTestBase() {

    private val relic = card("Test Bombard Relic") {
        manaCost = "{4}"
        typeLine = "Artifact"
    }

    private val trinket = card("Test Bombard Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private fun boastAbilityId() =
        cardRegistry.getCard("Broadside Bombardiers")!!.script.activatedAbilities[0].id

    private fun TestGame.boastOffered(): Boolean =
        getLegalActions(1).any { it.description.startsWith("Boast —") }

    private fun TestGame.boast(sacrificed: EntityId): ExecutionResult = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Broadside Bombardiers")!!,
            abilityId = boastAbilityId(),
            targets = listOf(ChosenTarget.Player(player2Id)),
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrificed)),
        )
    )

    private fun combatScenario(): TestGame = scenario()
        .withPlayers("Pirate", "Defender")
        .withCardOnBattlefield(1, "Broadside Bombardiers")
        .withCardOnBattlefield(1, "Test Bombard Relic")
        .withCardOnBattlefield(1, "Test Bombard Trinket")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        cardRegistry.register(BroadsideBombardiers)
        cardRegistry.register(relic)
        cardRegistry.register(trinket)

        test("the boast is not offered before the Bombardiers attack") {
            val game = combatScenario()
            game.boastOffered() shouldBe false
        }

        test("after attacking, sacrificing a four-drop artifact deals 2 + 4 damage, once each turn") {
            val game = combatScenario()
            game.declareAttackers(mapOf("Broadside Bombardiers" to 2)).error shouldBe null

            withClue("attacked this turn — the boast is now offered") {
                game.boastOffered() shouldBe true
            }

            val relicId = game.findPermanent("Test Bombard Relic")!!
            game.boast(relicId).error shouldBe null

            withClue("the sacrifice is a cost, paid on activation") {
                game.isInGraveyard(1, "Test Bombard Relic") shouldBe true
            }

            game.resolveStack()

            withClue("2 plus the relic's mana value of 4") {
                game.getLifeTotal(2) shouldBe 14
            }
            withClue("boast is once each turn, so a second activation is not offered") {
                game.boastOffered() shouldBe false
            }
        }
    }
}
