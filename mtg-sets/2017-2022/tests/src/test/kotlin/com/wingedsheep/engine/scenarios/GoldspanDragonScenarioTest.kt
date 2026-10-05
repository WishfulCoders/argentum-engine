package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.GoldspanDragon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goldspan Dragon (KHM #139).
 *
 *   Flying, haste
 *   Whenever this creature attacks or becomes the target of a spell, create a Treasure token.
 *   Treasures you control have "{T}, Sacrifice this artifact: Add two mana of any one color."
 *
 * Pins both halves of the one disjunctive trigger (attacking the turn it arrives, and an opposing
 * spell targeting it), and the granted two-mana Treasure ability.
 */
class GoldspanDragonScenarioTest : ScenarioTestBase() {

    private val grantedTreasureAbility = GoldspanDragon.staticAbilities
        .filterIsInstance<GrantActivatedAbility>()
        .single()
        .ability

    init {
        test("attacking the turn it arrives makes a Treasure") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Goldspan Dragon", summoningSickness = true)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("Haste lets it attack") {
                game.declareAttackers(mapOf("Goldspan Dragon" to 2)).error shouldBe null
            }
            game.resolveStack()
            withClue("The attack trigger made one Treasure") {
                game.findPermanents("Treasure").size shouldBe 1
            }
        }

        test("an opponent's spell targeting it makes a Treasure for its controller") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Goldspan Dragon")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Goldspan Dragon")!!
            game.castSpell(2, "Shock", dragon).error shouldBe null
            game.resolveStack()

            withClue("The 4/4 survives Shock") { game.isOnBattlefield("Goldspan Dragon") shouldBe true }
            val treasures = game.findPermanents("Treasure")
            withClue("One Treasure, controlled by the Dragon's controller") {
                treasures.size shouldBe 1
                game.state.getEntity(treasures.single())
                    ?.get<ControllerComponent>()
                    ?.playerId shouldBe game.player1Id
            }
        }

        test("Treasures you control tap and sacrifice for two mana of one color") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Goldspan Dragon")
                .withCardOnBattlefield(1, "Treasure")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val treasure = game.findPermanent("Treasure")!!
            withClue("The granted ability is offered on the Treasure") {
                game.getLegalActions(1).any {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == treasure && a.abilityId == grantedTreasureAbility.id
                } shouldBe true
            }

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = treasure,
                    abilityId = grantedTreasureAbility.id,
                    manaColorChoice = Color.RED,
                )
            )
            withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
            withClue("The Treasure was sacrificed for {R}{R}") {
                game.findPermanent("Treasure") shouldBe null
                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 2
            }
        }
    }
}
