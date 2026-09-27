package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Decisive Denial (STX #177).
 *
 * "{G}{U} Instant — Choose one — • Target creature you control fights target creature you don't
 * control. • Counter target noncreature spell unless its controller pays {3}."
 *
 * Mode 0 is Savage Smash's fight without a pump (both targets mandatory); mode 1 is Complicate's
 * counter-unless-pays over a *noncreature* spell. The counter-unless-pays half is tested both ways:
 * the controller has {3} available and pays (spell resolves) and has none and it is auto-countered.
 */
class DecisiveDenialScenarioTest : ScenarioTestBase() {

    private fun findCardInHand(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId, name: String) =
        game.state.getHand(playerId).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Decisive Denial") {

            test("mode 0: your creature fights a creature you don't control, no pump") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decisive Denial")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = findCardInHand(game, game.player1Id, "Decisive Denial")

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears)))
                    )
                )
                withClue("mode 0 cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears (2 toughness) dies to the Giant's unmodified 3 power") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Hill Giant (3 toughness) survives the Bears' 2 power — no pump was applied") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("mode 1: the noncreature spell resolves when its controller pays {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decisive Denial")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boltCast = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                withClue("Lightning Bolt cast should succeed: ${boltCast.error}") { boltCast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.passPriority() // player 2 passes so player 1 can respond

                val boltId = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                val denialId = findCardInHand(game, game.player1Id, "Decisive Denial")

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = denialId,
                        targets = listOf(ChosenTarget.Spell(boltId)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltId)))
                    )
                )
                withClue("mode 1 cast targeting the noncreature spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("player 2 (Bolt's controller) is offered the pay-or-lose-it choice") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Lightning Bolt still resolves after {3} is paid") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("mode 1: a spell auto-counters when its controller has no mana to pay {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decisive Denial")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boltCast = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                withClue("Lightning Bolt cast should succeed: ${boltCast.error}") { boltCast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.passPriority() // player 2 passes so player 1 can respond

                val boltId = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                val denialId = findCardInHand(game, game.player1Id, "Decisive Denial")

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = denialId,
                        targets = listOf(ChosenTarget.Spell(boltId)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltId)))
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("player 2 has no mana left to pay {3} — the Bolt is auto-countered") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
