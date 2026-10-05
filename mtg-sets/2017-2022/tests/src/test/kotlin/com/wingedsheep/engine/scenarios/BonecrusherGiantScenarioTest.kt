package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.BonecrusherGiant
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bonecrusher Giant // Stomp (ELD #115).
 *
 *   Bonecrusher Giant {2}{R} 4/3: Whenever this creature becomes the target of a spell, this
 *   creature deals 2 damage to that spell's controller.
 *   Stomp {1}{R} Instant — Adventure: Damage can't be prevented this turn. Stomp deals 2 damage to
 *   any target.
 *
 * Pins that the punisher hits the controller of the targeting spell (not Bonecrusher's own
 * controller), and that Stomp deals its damage and sends the card on an adventure into exile.
 */
class BonecrusherGiantScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(BonecrusherGiant)

        test("an opponent's spell targeting the Giant costs that opponent 2 life") {
            val game = scenario()
                .withPlayers("Giant", "Burner")
                .withCardOnBattlefield(1, "Bonecrusher Giant")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Bonecrusher Giant")!!

            game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()

            withClue("the bolt's controller takes 2; the Giant's controller is untouched") {
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("the bolt still resolves after the trigger: 3 damage kills the 4/3") {
                game.isInGraveyard(1, "Bonecrusher Giant") shouldBe true
            }
        }

        test("Stomp deals 2 damage and the card goes on an adventure into exile") {
            val game = scenario()
                .withPlayers("Giant", "Defender")
                .withCardInHand(1, "Bonecrusher Giant")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.findCardsInHand(1, "Bonecrusher Giant").single()

            game.execute(
                CastSpell(
                    game.player1Id, card,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    faceIndex = 0,
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.isInExile(1, "Bonecrusher Giant") shouldBe true
        }
    }
}
