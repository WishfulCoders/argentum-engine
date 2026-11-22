package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kirk, Enterprising Captain (TRC #198) — {3}{R} 4/3 menace.
 *
 *   Whenever Kirk attacks while creatures you control have total power 8 or greater, exile the
 *   top card of your library. You may play that card this turn.
 *
 * Proves the trigger fires at 8+ total power, doesn't fire below it, and — because "while" gates
 * the trigger rather than being an intervening-if — still resolves if power drops in response.
 */
class KirkEnterprisingCaptainScenarioTest : ScenarioTestBase() {

    private fun exiledNames(game: TestGame): List<String?> =
        game.state.getExile(game.player1Id).map { game.state.getEntity(it)?.get<CardComponent>()?.name }

    private fun mayPlayExiled(game: TestGame): Boolean {
        val exiled = game.state.getExile(game.player1Id)
        return exiled.isNotEmpty() && game.state.mayPlayPermissions.any {
            it.controllerId == game.player1Id && exiled.first() in it.cardIds
        }
    }

    init {
        context("Kirk, Enterprising Captain") {
            test("attacking with 8+ total power exiles the top card and lets you play it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirk, Enterprising Captain", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kirk, Enterprising Captain" to 2)).error shouldBe null
                game.resolveStack()

                withClue("4 + 3 + 2 = 9 power: the top card is exiled") {
                    exiledNames(game) shouldBe listOf("Mountain")
                }
                withClue("Player1 may play the exiled card this turn") {
                    mayPlayExiled(game) shouldBe true
                }
            }

            test("attacking below 8 total power does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirk, Enterprising Captain", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kirk, Enterprising Captain" to 2)).error shouldBe null

                withClue("4 + 3 = 7 power: nothing goes on the stack") {
                    game.state.stack.isEmpty() shouldBe true
                }
                game.resolveStack()
                exiledNames(game) shouldBe emptyList()
            }

            test("losing power in response doesn't stop the trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirk, Enterprising Captain", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kirk, Enterprising Captain" to 2)).error shouldBe null
                game.state.stack.size shouldBe 1

                // Player1 passes with the trigger on the stack; Player2 bolts the Hill Giant,
                // dropping total power to 6 before the trigger resolves.
                game.passPriority().error shouldBe null
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant died in response") {
                    game.findPermanent("Hill Giant") shouldBe null
                }
                withClue("the trigger still resolves: the condition was only checked on attack") {
                    exiledNames(game) shouldBe listOf("Mountain")
                    mayPlayExiled(game) shouldBe true
                }
            }
        }
    }
}
