package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.arn.cards.JeweledBird
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jeweled Bird {1} — Artifact:
 * "{T}: Ante this artifact. If you do, put all other cards you own from the ante into your
 * graveyard, then draw a card."
 *
 * The bird replaces your whole contribution to the ante (its ruling) — the opponent's anted
 * cards stay put.
 */
class JeweledBirdScenarioTest : ScenarioTestBase() {

    init {
        test("antes itself, sends your other ante cards to the graveyard, and draws") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .playingForAnte()
                .withCardOnBattlefield(1, "Jeweled Bird")
                .withCardInAnte(1, "Shivan Dragon")
                .withCardInAnte(1, "Serra Angel")
                .withCardInAnte(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bird = game.findPermanent("Jeweled Bird")!!
            game.execute(
                ActivateAbility(game.player1Id, bird, JeweledBird.activatedAbilities.single().id)
            ).error shouldBe null
            game.resolveStack()

            withClue("the bird is now your whole ante") {
                game.isInAnte(1, "Jeweled Bird") shouldBe true
                game.anteSize(1) shouldBe 1
            }
            withClue("your previous ante cards went to your graveyard") {
                game.isInGraveyard(1, "Shivan Dragon") shouldBe true
                game.isInGraveyard(1, "Serra Angel") shouldBe true
            }
            withClue("the opponent's ante is untouched") {
                game.isInAnte(2, "Hill Giant") shouldBe true
            }
            withClue("and you drew a card") {
                game.isInHand(1, "Forest") shouldBe true
            }
        }
    }
}
