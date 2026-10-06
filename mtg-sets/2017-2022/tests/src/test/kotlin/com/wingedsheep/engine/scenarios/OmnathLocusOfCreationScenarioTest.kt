package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Omnath, Locus of Creation — the landfall ability does something different on its first, second
 * and third resolution in a turn, and nothing on the fourth. Azusa and Rites of Flourishing give four
 * land drops in one main phase.
 */
class OmnathLocusOfCreationScenarioTest : ScenarioTestBase() {

    init {
        context("Omnath, Locus of Creation") {
            test("first landfall gains 4, second adds {R}{G}{W}{U}, third deals 4 to opponents and their planeswalkers, fourth does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Omnath, Locus of Creation")
                    .withCardOnBattlefield(1, "Azusa, Lost but Seeking")
                    .withCardOnBattlefield(1, "Rites of Flourishing")
                    .withCardsInHand(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Wrenn and Realmbreaker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val walker = game.findPermanent("Wrenn and Realmbreaker")!!
                game.state = game.state.updateEntity(walker) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
                }

                fun playForest() {
                    val forest = game.findCardsInHand(1, "Forest").first()
                    game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                    game.resolveStack()
                }

                playForest()
                withClue("first resolution: gain 4 life") { game.getLifeTotal(1) shouldBe 24 }

                playForest()
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                withClue("second resolution: {R}{G}{W}{U}, no more life") {
                    game.getLifeTotal(1) shouldBe 24
                    pool.red shouldBe 1
                    pool.green shouldBe 1
                    pool.white shouldBe 1
                    pool.blue shouldBe 1
                }

                playForest()
                withClue("third resolution: 4 damage to the opponent and their planeswalker") {
                    game.getLifeTotal(2) shouldBe 16
                    game.state.getEntity(walker)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 2
                    game.getLifeTotal(1) shouldBe 24
                }

                playForest()
                withClue("fourth resolution does nothing") {
                    game.getLifeTotal(1) shouldBe 24
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("entering draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Omnath, Locus of Creation")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Omnath, Locus of Creation").error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
