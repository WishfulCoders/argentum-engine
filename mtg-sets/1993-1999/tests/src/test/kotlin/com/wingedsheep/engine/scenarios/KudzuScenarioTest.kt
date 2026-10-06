package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kudzu (LEA #204, {1}{G}{G}, Enchantment — Aura).
 *
 *   Enchant land
 *   When enchanted land becomes tapped, destroy it. That land's controller may attach this Aura to
 *   a land of their choice.
 *
 * Alice controls Kudzu on Bob's Forest and taps it with Icy Manipulator, so the three players that
 * matter are distinct: the Aura's controller (Alice), the destroyed land's controller (Bob, who makes
 * the choice), and the controller of the new host. Pinned here: Bob — not Alice — decides; the
 * destroyed Forest is not a candidate; Kudzu moves without changing control and survives the
 * unattached-Aura state-based action; declining leaves it to fall off into the graveyard.
 */
class KudzuScenarioTest : ScenarioTestBase() {

    init {
        context("Kudzu") {

            fun setup() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Icy Manipulator")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(2, "Forest")
                .withCardAttachedTo(1, "Kudzu", "Forest")
                .withCardOnBattlefield(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.tapForestWithIcy() {
                val icy = findPermanent("Icy Manipulator")!!
                val forest = findPermanent("Forest")!!
                val abilityId = cardRegistry.getCard("Icy Manipulator")!!.script.activatedAbilities[0].id
                execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = icy,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(state, forest))
                    )
                ).error shouldBe null
                resolveStack()
            }

            test("the destroyed land's controller moves Kudzu to a land of their choice") {
                val game = setup()
                val kudzu = game.findPermanent("Kudzu")!!
                val mountain = game.findPermanent("Mountain")!!
                val island = game.findPermanent("Island")!!

                game.tapForestWithIcy()

                withClue("the tapped land was destroyed") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob, the destroyed land's controller, makes the choice — not Kudzu's controller") {
                    decision.playerId shouldBe game.player2Id
                }
                withClue("any land on the battlefield, either player's; the destroyed Forest is gone") {
                    decision.options.toSet() shouldBe setOf(mountain, island)
                }

                game.selectCards(listOf(mountain)).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("Kudzu survived the unattached-Aura SBA on its new host") {
                    game.isOnBattlefield("Kudzu") shouldBe true
                    game.state.getEntity(kudzu)!!.get<AttachedToComponent>()!!.targetId shouldBe mountain
                }
                withClue("control of Kudzu does not change") {
                    game.state.getEntity(kudzu)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
                }
            }

            test("declining the move leaves Kudzu unattached, so it goes to the graveyard") {
                val game = setup()

                game.tapForestWithIcy()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                game.isInGraveyard(2, "Forest") shouldBe true
                game.isOnBattlefield("Kudzu") shouldBe false
                game.isInGraveyard(1, "Kudzu") shouldBe true
            }
        }
    }
}
