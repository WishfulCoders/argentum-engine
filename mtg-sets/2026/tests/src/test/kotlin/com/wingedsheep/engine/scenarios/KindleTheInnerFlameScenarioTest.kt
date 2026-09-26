package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kindle the Inner Flame (ECL #147) — flashback costs {1}{R} plus beholding three Elementals, from
 * among Elementals you control and Elemental cards in your hand.
 */
class KindleTheInnerFlameScenarioTest : ScenarioTestBase() {

    init {
        fun builder(): ScenarioBuilder = scenario()
            .withPlayers("Player", "Opponent")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInGraveyard(1, "Kindle the Inner Flame")
            .withCardOnBattlefield(1, "Shimmercreep")
            .withCardInHand(1, "Stratosoarer")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Kindle the Inner Flame flashback") {
            test("the legal action offers the behold picker over Elementals on the battlefield and in hand") {
                val game = builder().withCardInHand(1, "Sunderflock").build()
                val onBattlefield = game.findPermanent("Shimmercreep").shouldNotBeNull()
                val inHand = game.findCardsInHand(1, "Stratosoarer") + game.findCardsInHand(1, "Sunderflock")

                val flashback = game.getLegalActions(1)
                    .firstOrNull { it.actionType == "CastWithFlashback" }
                    .shouldNotBeNull()
                flashback.isAffordable shouldBe true
                val info = flashback.additionalCostInfo.shouldNotBeNull()
                info.costType shouldBe "Behold"
                info.beholdCount shouldBe 3
                info.validBeholdTargets shouldContainExactlyInAnyOrder listOf(onBattlefield) + inHand
            }

            test("with only two Elementals to behold, flashback is not affordable") {
                val game = builder().build()

                val flashback = game.getLegalActions(1)
                    .firstOrNull { it.actionType == "CastWithFlashback" }
                    .shouldNotBeNull()
                flashback.isAffordable shouldBe false
            }
        }
    }
}
