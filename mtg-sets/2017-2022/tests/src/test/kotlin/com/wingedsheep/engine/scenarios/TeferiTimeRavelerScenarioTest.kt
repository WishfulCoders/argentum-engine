package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Teferi, Time Raveler (WAR #221). The sorcery-timing lock's rules (stack, steps, resolution, flash
 * precedence) are pinned by the engine's `CastOnlyAtSorceryTimingTest`; this covers the card's three
 * lines end to end.
 */
class TeferiTimeRavelerScenarioTest : ScenarioTestBase() {

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Teferi, Time Raveler")!!.script.activatedAbilities[index]

    private fun castable(game: TestGame, playerNumber: Int, name: String): Boolean {
        val cardId = game.findCardsInHand(playerNumber, name).single()
        return game.getLegalActions(playerNumber).any { (it.action as? CastSpell)?.cardId == cardId }
    }

    init {
        test("opponents can't cast an instant outside their own sorcery timing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Teferi, Time Raveler")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castable(game, 2, "Shock") shouldBe false
        }

        test("+1: until your next turn you may cast sorceries as though they had flash") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Teferi, Time Raveler")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .apply { repeat(3) { withCardInLibrary(1, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val teferi = game.findPermanent("Teferi, Time Raveler")!!
            seedLoyalty(game, teferi, 4)

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = teferi, abilityId = ability(0).id))
                .error shouldBe null
            game.resolveStack()
            game.state.getEntity(teferi)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5

            // On the opponent's turn, with priority, the sorcery is castable.
            game.state = game.state.copy(
                activePlayerId = game.player2Id,
                phase = Phase.COMBAT,
                step = Step.BEGIN_COMBAT,
                priorityPlayerId = game.player1Id,
            )
            withClue("Divination at instant speed on the opponent's turn") {
                castable(game, 1, "Divination") shouldBe true
            }
        }

        test("−3: return the target to its owner's hand and draw a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Teferi, Time Raveler")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val teferi = game.findPermanent("Teferi, Time Raveler")!!
            seedLoyalty(game, teferi, 4)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = teferi, abilityId = ability(1).id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
            game.state.getEntity(teferi)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 1
        }

        test("−3 with no target just draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Teferi, Time Raveler")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val teferi = game.findPermanent("Teferi, Time Raveler")!!
            seedLoyalty(game, teferi, 4)

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = teferi, abilityId = ability(1).id))
                .error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 1
        }
    }
}
