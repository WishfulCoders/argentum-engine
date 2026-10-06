package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Narset, Parter of Veils (WAR #61). The static is [com.wingedsheep.sdk.scripting.RestrictDrawsPerTurn];
 * its rules are covered in depth by the engine's `RestrictDrawsPerTurnTest`, so this pins the card's
 * own wiring: opponents only, one card a turn across several instructions, and the −2 dig.
 */
class NarsetParterOfVeilsScenarioTest : ScenarioTestBase() {

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    init {
        fun base() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("an opponent draws only one card this turn across two draw instructions") {
            val game = base()
                .withCardOnBattlefield(1, "Narset, Parter of Veils")
                .withCardsInHand(1, "Inspiration", 2)
                .withLandsOnBattlefield(1, "Island", 8)
                .apply { repeat(6) { withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            withClue("the second card of the first Inspiration is ignored") { game.handSize(2) shouldBe 1 }

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            withClue("no further draws this turn") { game.handSize(2) shouldBe 1 }
            game.librarySize(2) shouldBe 5
        }

        test("Narset's controller is not restricted") {
            val game = base()
                .withCardOnBattlefield(1, "Narset, Parter of Veils")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .apply { repeat(4) { withCardInLibrary(1, "Forest") } }
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 2
        }

        test("−2: reveal a noncreature, nonland card from the top four; the rest go to the bottom") {
            val game = base()
                .withCardOnBattlefield(1, "Narset, Parter of Veils")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .build()
            val narset = game.findPermanent("Narset, Parter of Veils")!!
            seedLoyalty(game, narset, 5)
            val shock = game.state.getLibrary(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Shock"
            }
            val ability = cardRegistry.getCard("Narset, Parter of Veils")!!.script.activatedAbilities.single()

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = narset, abilityId = ability.id))
                .error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(shock)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Shock") shouldBe true
            game.state.getEntity(narset)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
            withClue("the fifth card stays on top; the other three went under it") {
                val library = game.state.getLibrary(game.player1Id)
                library.size shouldBe 4
                game.state.getEntity(library.first())?.get<CardComponent>()?.name shouldBe "Mountain"
            }
        }
    }
}
