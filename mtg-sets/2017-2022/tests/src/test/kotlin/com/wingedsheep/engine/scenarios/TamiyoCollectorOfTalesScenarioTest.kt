package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tamiyo, Collector of Tales (WAR #220). The two "can't"s are pinned in depth by the engine's
 * `OpponentsCantMakeYouDiscardTest` and the Sigarda tests; this covers the card end to end.
 */
class TamiyoCollectorOfTalesScenarioTest : ScenarioTestBase() {

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Tamiyo, Collector of Tales")!!.script.activatedAbilities[index]

    init {
        fun base() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("an opponent's spell can't make you discard") {
            val game = base()
                .withCardOnBattlefield(2, "Tamiyo, Collector of Tales")
                .withCardInHand(1, "Mind Rot")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .build()

            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()
            game.handSize(2) shouldBe 2
        }

        test("an opponent's edict can't make you sacrifice") {
            val game = base()
                .withCardOnBattlefield(2, "Tamiyo, Collector of Tales")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Diabolic Edict")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpellTargetingPlayer(1, "Diabolic Edict", 2).error shouldBe null
            game.resolveStack()
            (game.findPermanent("Grizzly Bears") != null) shouldBe true
        }

        test("+1: name a card; matches from the top four go to hand, the rest to the graveyard") {
            val game = base()
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Hill Giant")
                .build()
            val tamiyo = game.findPermanent("Tamiyo, Collector of Tales")!!
            seedLoyalty(game, tamiyo, 5)

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = tamiyo, abilityId = ability(0).id))
                .error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            val shockIndex = decision.options.indexOf("Shock")
            withClue("Shock is offered as a nonland name") { (shockIndex >= 0) shouldBe true }
            game.submitDecision(OptionChosenResponse(decision.id, shockIndex)).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Shock").size shouldBe 2
            game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInGraveyard(1, "Forest").size shouldBe 1
            withClue("the fifth card is untouched") { game.librarySize(1) shouldBe 1 }
            game.state.getEntity(tamiyo)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 6
        }

        test("−3: return target card from your graveyard to your hand") {
            val game = base()
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardInGraveyard(1, "Hill Giant")
                .build()
            val tamiyo = game.findPermanent("Tamiyo, Collector of Tales")!!
            seedLoyalty(game, tamiyo, 5)
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = tamiyo, abilityId = ability(1).id,
                    targets = listOf(ChosenTarget.Card(giant, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Hill Giant") shouldBe true
        }
    }
}
