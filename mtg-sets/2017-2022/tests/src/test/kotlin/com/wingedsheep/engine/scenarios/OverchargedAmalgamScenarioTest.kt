package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Overcharged Amalgam (VOW #71) — {2}{U}{U} Creature — Zombie Horror, 3/3.
 *
 *   Flash
 *   Flying
 *   Exploit (When this creature enters, you may sacrifice a creature.)
 *   When this creature exploits a creature, counter target spell, activated ability, or
 *   triggered ability.
 *
 * Flashed in response; covers countering a spell and an activated ability, and declining the
 * exploit leaving the stack alone.
 */
class OverchargedAmalgamScenarioTest : ScenarioTestBase() {

    init {
        /** Resolve the Amalgam and its exploit trigger, sacrifice Grizzly Bears, counter [victim]. */
        fun TestGame.exploitAndCounter(victim: EntityId) {
            castSpell(1, "Overcharged Amalgam").error shouldBe null
            resolveStack() // Amalgam resolves, exploit trigger resolves -> "sacrifice?" prompt
            answerYesNo(true).error shouldBe null
            selectCards(listOf(findPermanent("Grizzly Bears")!!)).error shouldBe null
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            selectTargets(listOf(victim)).error shouldBe null
            resolveStack()
        }

        context("Overcharged Amalgam") {

            test("exploiting counters a spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overcharged Amalgam")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.exploitAndCounter(bolt)

                withClue("the Bolt was countered; the Bears were sacrificed") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Overcharged Amalgam") shouldBe true
                }
            }

            test("exploiting counters an activated ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overcharged Amalgam")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
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
                val ping = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.exploitAndCounter(ping)

                withClue("the ping was countered") {
                    game.getLifeTotal(1) shouldBe 20
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("declining the exploit leaves the spell to resolve") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overcharged Amalgam")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpell(1, "Overcharged Amalgam").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
