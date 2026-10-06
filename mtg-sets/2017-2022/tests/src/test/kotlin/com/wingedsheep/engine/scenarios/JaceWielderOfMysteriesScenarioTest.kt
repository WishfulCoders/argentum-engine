package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
 * Jace, Wielder of Mysteries (WAR #54, loyalty 4).
 *
 *   If you would draw a card while your library has no cards in it, you win the game instead.
 *   +1: Target player mills two cards. Draw a card.
 *   −8: Draw seven cards. Then if your library has no cards in it, you win the game.
 */
class JaceWielderOfMysteriesScenarioTest : ScenarioTestBase() {

    private val jace = "Jace, Wielder of Mysteries"

    private fun TestGame.activate(index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(jace)!!.script.activatedAbilities[index]
        execute(ActivateAbility(player1Id, findPermanent(jace)!!, ability.id, targets)).error shouldBe null
    }

    private fun TestGame.setLoyalty(id: EntityId, amount: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount)) }
    }

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, jace)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+1: the target player mills two, then you draw") {
            val game = base()
                .withCardInLibrary(1, "Island")
                .apply { repeat(3) { withCardInLibrary(2, "Island") } }
                .build()
            val id = game.findPermanent(jace)!!
            game.setLoyalty(id, 4)

            game.activate(0, listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            game.graveyardSize(2) shouldBe 2
            game.handSize(1) shouldBe 1
            game.loyalty(id) shouldBe 5
        }

        test("+1 with an empty library: the draw is replaced and you win") {
            val game = base()
                .apply { repeat(3) { withCardInLibrary(2, "Island") } }
                .build()
            game.setLoyalty(game.findPermanent(jace)!!, 4)

            game.activate(0, listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player1Id
        }

        test("−8 from exactly eight loyalty: Jace is gone, the draws run out, and the check wins anyway") {
            val game = base()
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .build()
            game.setLoyalty(game.findPermanent(jace)!!, 8)

            game.activate(1)
            withClue("zero loyalty: Jace is put into the graveyard before the ability resolves") {
                game.isInGraveyard(1, jace) shouldBe true
            }
            game.resolveStack()

            withClue("three real draws, then the library is empty — a win, not a draw-loss") {
                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }
        }
    }
}
