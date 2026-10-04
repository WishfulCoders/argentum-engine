package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Skyclave Apparition (ZNR #39) — {1}{W}{W} Creature — Kor Spirit 2/2.
 *
 * When this creature enters, exile up to one target nonland, nontoken permanent you don't control
 * with mana value 4 or less.
 * When this creature leaves the battlefield, the exiled card's owner creates an X/X blue Illusion
 * creature token, where X is the mana value of the exiled card.
 *
 * Pins: the exiled card stays exiled when the Apparition leaves (no return), and the leaves
 * trigger hands the exiled card's owner an X/X blue Illusion sized by its mana value.
 */
class SkyclaveApparitionScenarioTest : ScenarioTestBase() {

    init {
        context("Skyclave Apparition") {

            test("exiles a mana value 4 permanent; leaving gives its owner a 4/4 blue Illusion") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Skyclave Apparition")
                    .withCardInHand(1, "End Hostilities") // {3}{W}{W} wipe to kill the Apparition
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardOnBattlefield(2, "Hill Giant") // {3}{R} → mana value 4
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Skyclave Apparition").error shouldBe null
                game.resolveStack() // Apparition enters → ETB asks for its target
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInExile(2, "Hill Giant") shouldBe true

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Skyclave Apparition") shouldBe false
                // Never returned.
                game.isInExile(2, "Hill Giant") shouldBe true

                val tokenId = game.findPermanent("Illusion Token")
                tokenId.shouldNotBeNull()
                val token = game.state.getEntity(tokenId)!!
                val tokenCard = token.get<CardComponent>()!!
                tokenCard.colors shouldBe setOf(Color.BLUE)
                tokenCard.baseStats?.basePower shouldBe 4
                tokenCard.baseStats?.baseToughness shouldBe 4
                token.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }

            test("with nothing exiled, leaving creates no token") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Skyclave Apparition")
                    .withCardInHand(1, "End Hostilities")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardOnBattlefield(2, "Serra Angel") // mana value 5 — not a legal target
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Skyclave Apparition").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.skipTargets()
                game.resolveStack()

                game.isOnBattlefield("Serra Angel") shouldBe true

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Skyclave Apparition") shouldBe false
                game.findPermanent("Illusion Token") shouldBe null
            }
        }
    }
}
