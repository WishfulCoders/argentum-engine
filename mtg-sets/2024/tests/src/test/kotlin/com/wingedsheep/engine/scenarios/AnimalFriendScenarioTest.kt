package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Animal Friend (PIP #72) — {1}{G} Aura.
 * "Enchanted creature has 'Whenever this creature attacks, create a 1/1 green Squirrel creature
 * token. Put a +1/+1 counter on that token for each Aura and Equipment attached to this creature
 * other than Animal Friend.'"
 */
class AnimalFriendScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.attackWithBears() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            resolveStack()
        }

        fun TestGame.squirrelCounters(): Int {
            val squirrels = findPermanents("Squirrel Token")
            squirrels shouldHaveSize 1
            return state.getEntity(squirrels.single())?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        }

        context("Animal Friend") {
            test("counts every other Aura and Equipment on the creature, but not itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Animal Friend", "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithBears()

                game.squirrelCounters() shouldBe 2
                val squirrel = game.findPermanent("Squirrel Token")!!
                game.state.projectedState.getPower(squirrel) shouldBe 3
                game.state.projectedState.getToughness(squirrel) shouldBe 3
            }

            test("with nothing else attached the Squirrel stays a 1/1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Animal Friend", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithBears()

                game.squirrelCounters() shouldBe 0
                val squirrel = game.findPermanent("Squirrel Token")!!
                game.state.projectedState.getPower(squirrel) shouldBe 1
                game.state.projectedState.getToughness(squirrel) shouldBe 1
            }
        }
    }
}
