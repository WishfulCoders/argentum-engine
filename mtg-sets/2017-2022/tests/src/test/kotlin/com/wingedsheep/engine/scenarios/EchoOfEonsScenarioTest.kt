package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Echo of Eons {4}{U}{U} — Sorcery.
 *
 * "Each player shuffles their hand and graveyard into their library, then draws seven cards.
 *  Flashback {2}{U}"
 *
 * The composition worth proving is the flashback cast: the card leaves the graveyard before the
 * graveyard is shuffled away, so it isn't shuffled into the library, and it ends in exile.
 */
class EchoOfEonsScenarioTest : ScenarioTestBase() {

    init {
        test("flashed back from the graveyard, it wheels both players and is exiled") {
            val builder = scenario()
                .withPlayers()
                .withCardInGraveyard(1, "Echo of Eons")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(10) { builder.withCardInLibrary(1, "Forest"); builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            val echo = game.findCardsInGraveyard(1, "Echo of Eons").single()
            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = echo,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            )
            withClue("flashback cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.handSize(1) shouldBe 7
            game.handSize(2) shouldBe 7
            withClue("both graveyards were shuffled away") {
                game.graveyardSize(1) shouldBe 0
                game.graveyardSize(2) shouldBe 0
            }
            withClue("flashback exiled Echo of Eons instead of letting it be shuffled in or hit the yard") {
                game.isInExile(1, "Echo of Eons") shouldBe true
            }
            // 10 Forests + Bears + Bolt = 12, minus seven drawn.
            game.librarySize(1) shouldBe 5
            // 10 Forests + Bears = 11, minus seven drawn.
            game.librarySize(2) shouldBe 4
        }
    }
}
