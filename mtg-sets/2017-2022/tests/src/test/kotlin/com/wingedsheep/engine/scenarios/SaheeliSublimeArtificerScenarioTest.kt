package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Saheeli, Sublime Artificer (WAR #234, loyalty 5).
 *
 *   Whenever you cast a noncreature spell, create a 1/1 colorless Servo artifact creature token.
 *   −2: Target artifact you control becomes a copy of another target artifact or creature you
 *   control until end of turn, except it's an artifact in addition to its other types.
 */
class SaheeliSublimeArtificerScenarioTest : ScenarioTestBase() {

    private val saheeli = "Saheeli, Sublime Artificer"

    init {
        test("a noncreature spell makes a Servo; a creature spell does not") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, saheeli)
                .withCardInHand(1, "Divination")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun servos() = game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name?.startsWith("Servo") == true
            }

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            servos() shouldHaveSize 0

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            servos() shouldHaveSize 1
            game.state.projectedState.getPower(servos().single()) shouldBe 1
        }

        test("−2: the artifact copies the creature until end of turn and stays an artifact") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, saheeli)
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Serra Angel")
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .apply { repeat(3) { withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val walker = game.findPermanent(saheeli)!!
            game.state = game.state.updateEntity(walker) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
            }
            val ornithopter = game.findPermanent("Ornithopter")!!
            val angel = game.findPermanent("Serra Angel")!!

            val minusTwo = cardRegistry.getCard(saheeli)!!.script.activatedAbilities.single()
            game.execute(
                ActivateAbility(
                    game.player1Id, walker, minusTwo.id,
                    targets = listOf(ChosenTarget.Permanent(ornithopter), ChosenTarget.Permanent(angel))
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Ornithopter is a 4/4 flying Serra Angel that is still an artifact") {
                game.state.getEntity(ornithopter)!!.get<CardComponent>()!!.name shouldBe "Serra Angel"
                projected.getPower(ornithopter) shouldBe 4
                projected.getToughness(ornithopter) shouldBe 4
                projected.hasKeyword(ornithopter, Keyword.VIGILANCE) shouldBe true
                projected.hasType(ornithopter, CardType.ARTIFACT.name) shouldBe true
            }
            game.state.getEntity(walker)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the copy effect ended with the turn") {
                game.state.getEntity(ornithopter)!!.get<CardComponent>()!!.name shouldBe "Ornithopter"
                game.state.projectedState.getPower(ornithopter) shouldBe 0
            }
        }
    }
}
