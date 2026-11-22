package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * My Precious // Allure of Power.
 *
 * "Equipped creature has hexproof and can't be blocked." — same bug as Cloak of Mists and
 * Whispersilk Cloak: the evasion half was `flags(AbilityFlag.CANT_BE_BLOCKED)`, which lands on the
 * Equipment rather than on the creature it equips, while the hexproof half was a proper
 * `GrantKeyword` static.
 *
 * Allure of Power — "As an additional cost to cast this spell, sacrifice a creature." The
 * Adventure's own additional cost was dropped on a face cast (#3196): castable with no creature,
 * and the creature it should have eaten stayed on the battlefield.
 */
class MyPreciousScenarioTest : ScenarioTestBase() {

    init {
        context("the equipped creature cannot be blocked") {

            test("no creature may block the equipped attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardAttachedTo(1, "My Precious", "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("My Precious must make the creature it equips unblockable") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
                }
            }
        }

        context("Allure of Power owes its sacrifice") {

            fun allureGame(withCreature: Boolean) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "My Precious")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .apply { if (withCreature) withCardOnBattlefield(1, "Grizzly Bears") }
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("with no creature to sacrifice the Adventure is neither affordable nor castable") {
                val game = allureGame(withCreature = false)
                val precious = game.findCardsInHand(1, "My Precious").single()

                withClue("Allure of Power is only offered grayed out without a creature to sacrifice") {
                    game.getLegalActions(1).none {
                        it.isAffordable &&
                            (it.action as? CastSpell)?.let { cs -> cs.cardId == precious && cs.faceIndex == 0 } == true
                    } shouldBe true
                }
                withClue("a submitted face cast must be rejected for its unpaid sacrifice") {
                    game.execute(CastSpell(game.player1Id, precious, faceIndex = 0)).error shouldNotBe null
                }
                game.isInHand(1, "My Precious") shouldBe true
            }

            test("the offer carries the sacrifice picker and casting consumes the creature") {
                val game = allureGame(withCreature = true)
                val precious = game.findCardsInHand(1, "My Precious").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                val offer = game.getLegalActions(1).single {
                    (it.action as? CastSpell)?.let { cs -> cs.cardId == precious && cs.faceIndex == 0 } == true
                }
                offer.additionalCostInfo?.costType shouldBe "SacrificePermanent"
                offer.additionalCostInfo?.validSacrificeTargets.orEmpty() shouldContainExactly listOf(bears)

                withClue("the sacrifice is mandatory — a face cast without it is rejected") {
                    game.execute(CastSpell(game.player1Id, precious, faceIndex = 0)).error shouldNotBe null
                }
                game.execute(
                    CastSpell(
                        game.player1Id, precious, faceIndex = 0,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                val handBefore = game.handSize(1)
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 2
                game.isInExile(1, "My Precious") shouldBe true
            }

            test("casting the Equipment does not inherit the Adventure's sacrifice") {
                val game = allureGame(withCreature = true)
                val precious = game.findCardsInHand(1, "My Precious").single()

                game.execute(CastSpell(game.player1Id, precious)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("My Precious") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
