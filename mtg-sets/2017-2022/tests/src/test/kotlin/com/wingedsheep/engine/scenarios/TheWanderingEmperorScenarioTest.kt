package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.neo.cards.TheWanderingEmperor
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Wandering Emperor (NEO #42) — flash; as long as she entered this turn, her loyalty abilities
 * can be activated any time you could cast an instant (still once per turn, ruling 2022-02-18).
 *
 * Flashes her in on the opponent's turn, exiles their tapped creature with −2, then shows the
 * once-per-turn limit holds and that on her controller's next turn she is back to sorcery timing.
 */
class TheWanderingEmperorScenarioTest : ScenarioTestBase() {
    init {
        fun abilityId(change: Int) = TheWanderingEmperor.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "The Wandering Emperor")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.resolveAndTakePriority() {
            resolveStack()
            if (state.priorityPlayerId == player2Id) passPriority()
            state.priorityPlayerId shouldBe player1Id
        }

        test("flashed in on the opponent's turn, −2 exiles their tapped creature at instant speed") {
            val game = board()
            game.castSpell(1, "The Wandering Emperor").error shouldBe null
            game.resolveAndTakePriority()
            val emperor = game.findPermanent("The Wandering Emperor")!!
            withClue("her loyalty abilities are offered on the opponent's turn") {
                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == emperor } shouldBe true
            }

            withClue("an untapped creature is not a legal −2 target") {
                game.execute(
                    ActivateAbility(
                        game.player1Id, emperor, abilityId(-2),
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                    )
                ).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    game.player1Id, emperor, abilityId(-2),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                )
            ).error shouldBe null
            game.resolveAndTakePriority()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 22

            withClue("still only one loyalty ability per turn") {
                game.execute(ActivateAbility(game.player1Id, emperor, abilityId(-1))).error shouldNotBe null
            }
        }

        test("on her controller's next turn she has not entered this turn, so she is back to sorcery timing") {
            val game = board()
            game.castSpell(1, "The Wandering Emperor").error shouldBe null
            game.resolveAndTakePriority()
            val emperor = game.findPermanent("The Wandering Emperor")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.execute(ActivateAbility(game.player1Id, emperor, abilityId(-1))).error shouldNotBe null

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.execute(ActivateAbility(game.player1Id, emperor, abilityId(-1))).error shouldBe null
            game.resolveStack()
            game.findPermanents("Samurai Token").size shouldBe 1
        }
    }
}
