package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Redirect Lightning (TLA #151) — {R} Instant — Lesson.
 *
 *   As an additional cost to cast this spell, pay 5 life or pay {2}.
 *   Change the target of target spell or ability with a single target.
 *
 * Mode 0 pays 5 life, mode 1 pays {2}. Covers a spell (paying life) and an opponent's triggered
 * ability (paying mana).
 */
class RedirectLightningScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.castRedirectAt(stackObject: EntityId, mode: Int) {
            val card = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Redirect Lightning"
            }
            val targets = listOf(ChosenTarget.Spell(stackObject))
            execute(
                CastSpell(
                    player1Id, card, targets,
                    chosenModes = listOf(mode),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldBe null
        }

        context("Redirect Lightning") {

            test("paying 5 life, redirects a spell's single target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Redirect Lightning")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.castRedirectAt(bolt, mode = 0)
                withClue("the 5 life is paid on cast") { game.getLifeTotal(1) shouldBe 15 }

                game.resolveStack()
                game.selectCards(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("the Bolt hit its own caster instead") {
                    game.getLifeTotal(1) shouldBe 15
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("paying {2}, redirects a triggered ability's single target") {
                // Flametongue Kavu: "When this creature enters, it deals 4 damage to target creature."
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Redirect Lightning")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(2, "Flametongue Kavu")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Flametongue Kavu").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                val trigger = game.state.stack.last()
                // The Kavu's controller holds priority with the trigger on the stack; pass to us.
                game.passPriority().error shouldBe null
                game.castRedirectAt(trigger, mode = 1)

                game.resolveStack()
                val kavu = game.findPermanent("Flametongue Kavu")!!
                game.selectCards(listOf(kavu)).error shouldBe null
                game.resolveStack()

                withClue("no life was paid; the Kavu's trigger hit the Kavu instead of Hill Giant") {
                    game.getLifeTotal(1) shouldBe 20
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Flametongue Kavu") shouldBe false
                }
            }
        }
    }
}
