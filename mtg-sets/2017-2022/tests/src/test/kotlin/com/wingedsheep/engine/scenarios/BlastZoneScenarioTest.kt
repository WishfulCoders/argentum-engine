package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.BlastZone
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Blast Zone (WAR #244) — Land.
 *
 *   This land enters with a charge counter on it.
 *   {T}: Add {C}.
 *   {X}{X}, {T}: Put X charge counters on this land.
 *   {3}, {T}, Sacrifice this land: Destroy each nonland permanent with mana value equal to the
 *   number of charge counters on this land.
 */
class BlastZoneScenarioTest : ScenarioTestBase() {

    private val addCharge = BlastZone.activatedAbilities[1].id
    private val sweep = BlastZone.activatedAbilities[2].id

    private fun charge(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    private fun setCharge(game: TestGame, id: EntityId, count: Int) {
        game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.CHARGE to count))) }
    }

    init {
        context("Blast Zone") {

            test("enters with one charge counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blast Zone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val inHand = game.findCardsInHand(1, "Blast Zone").first()

                game.execute(PlayLand(game.player1Id, inHand)).error shouldBe null

                val zone = game.findPermanent("Blast Zone")!!
                charge(game, zone) shouldBe 1
            }

            test("{X}{X}, {T}: pays twice X and adds X charge counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blast Zone")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val zone = game.findPermanent("Blast Zone")!!
                setCharge(game, zone, 1)

                game.execute(ActivateAbility(game.player1Id, zone, addCharge, xValue = 2)).error shouldBe null
                game.resolveStack()

                withClue("X = 2 adds two counters to the one it had") { charge(game, zone) shouldBe 3 }
                game.state.getEntity(zone)?.has<TappedComponent>() shouldBe true
                withClue("{X}{X} with X = 2 costs four mana") {
                    game.findAllPermanents("Forest").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 4
                }
            }

            test("sacrifice: destroys each nonland permanent whose mana value equals the counters it had") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blast Zone")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val zone = game.findPermanent("Blast Zone")!!
                setCharge(game, zone, 2)

                game.execute(ActivateAbility(game.player1Id, zone, sweep)).error shouldBe null
                withClue("The sacrifice is a cost, paid on activation") {
                    game.isInGraveyard(1, "Blast Zone") shouldBe true
                }
                game.resolveStack()

                withClue("Both mana value 2 creatures die, counted from the last-known charge counters") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 0
                }
                withClue("Mana value 1 artifact and the lands survive") {
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                    game.findAllPermanents("Forest").size shouldBe 3
                }
            }
        }
    }
}
