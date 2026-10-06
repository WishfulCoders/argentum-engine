package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ertai Resurrected (DMU #199) — {2}{U}{B} Legendary Creature — Phyrexian Human Wizard, 3/2.
 *
 *   Flash
 *   When Ertai Resurrected enters, choose up to one —
 *   • Counter target spell, activated ability, or triggered ability. Its controller draws a card.
 *   • Destroy another target creature or planeswalker. Its controller draws a card.
 *
 * Covers both modes and the decline; the activated-ability case proves "its controller" resolves
 * for an ability on the stack (which carries no ControllerComponent).
 */
class ErtaiResurrectedScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.chooseMode(containing: String?) {
            val decision = getPendingDecision() as? ChooseOptionDecision
                ?: error("Expected a ChooseOptionDecision; got ${getPendingDecision()}")
            val index = if (containing == null) {
                decision.options.indexOfFirst { !it.contains("Counter") && !it.contains("Destroy") }
            } else {
                decision.options.indexOfFirst { it.contains(containing) }
            }
            withClue("option '$containing' offered among ${decision.options}") { (index >= 0) shouldBe true }
            submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
        }

        fun TestGame.stackObjectNamed(name: String): EntityId =
            state.stack.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        context("Ertai Resurrected") {

            test("counter mode counters a spell and its controller draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ertai Resurrected")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.stackObjectNamed("Lightning Bolt")
                game.passPriority().error shouldBe null
                game.castSpell(1, "Ertai Resurrected").error shouldBe null
                val opponentHand = game.handSize(2)
                val yourHand = game.handSize(1)

                game.resolveStack()
                game.chooseMode("Counter")
                game.selectTargets(listOf(bolt)).error shouldBe null
                game.resolveStack()

                withClue("the Bolt was countered") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("the Bolt's controller drew a card; Ertai's controller did not") {
                    game.handSize(2) shouldBe opponentHand + 1
                    game.handSize(1) shouldBe yourHand
                }
            }

            test("counter mode counters an activated ability and its controller draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ertai Resurrected")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = sorcerer,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                    )
                ).error shouldBe null
                val ability = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.castSpell(1, "Ertai Resurrected").error shouldBe null
                val opponentHand = game.handSize(2)

                game.resolveStack()
                game.chooseMode("Counter")
                game.selectTargets(listOf(ability)).error shouldBe null
                game.resolveStack()

                withClue("the ping was countered") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("the ability's controller drew a card") {
                    game.handSize(2) shouldBe opponentHand + 1
                }
            }

            test("destroy mode destroys another creature and its controller draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ertai Resurrected")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ertai Resurrected").error shouldBe null
                game.resolveStack()
                val opponentHand = game.handSize(2)
                val yourHand = game.handSize(1)

                game.chooseMode("Destroy")
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears was destroyed; Ertai stays") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Ertai Resurrected") shouldBe true
                }
                withClue("the destroyed creature's controller drew a card") {
                    game.handSize(2) shouldBe opponentHand + 1
                    game.handSize(1) shouldBe yourHand
                }
            }

            test("choosing no mode does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ertai Resurrected")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ertai Resurrected").error shouldBe null
                game.resolveStack()
                val opponentHand = game.handSize(2)

                game.chooseMode(null)
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.handSize(2) shouldBe opponentHand
            }
        }
    }
}
