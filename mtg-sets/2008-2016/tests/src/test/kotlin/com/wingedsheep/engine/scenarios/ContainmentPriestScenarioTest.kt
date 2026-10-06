package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Containment Priest (C14 #5): flash; if a nontoken creature would enter and it wasn't cast, exile
 * it instead.
 */
class ContainmentPriestScenarioTest : ScenarioTestBase() {

    init {
        context("Containment Priest") {
            test("flashed in response to a reanimation spell, it exiles the returning creature") {
                val game = scenario()
                    .withPlayers("Reanimator", "Priest")
                    .withCardInHand(1, "Zombify")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Containment Priest")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(giant)).error shouldBe null
                game.execute(PassPriority(game.player1Id)).error shouldBe null
                withClue("flash: the Priest is cast with Zombify on the stack") {
                    game.castSpell(2, "Containment Priest").error shouldBe null
                }
                game.resolveStack()

                withClue("the Priest itself was cast, so it entered") { game.isOnBattlefield("Containment Priest") shouldBe true }
                withClue("Hill Giant would have entered without being cast — exiled instead") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInExile(1, "Hill Giant") shouldBe true
                }
            }

            test("a creature that is cast enters normally; a played Dryad Arbor is exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Containment Priest")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Dryad Arbor")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("cast creatures are unaffected") { game.isOnBattlefield("Grizzly Bears") shouldBe true }

                val arbor = game.findCardsInHand(1, "Dryad Arbor").single()
                game.execute(PlayLand(game.player1Id, arbor)).error shouldBe null
                withClue("Dryad Arbor is a creature that wasn't cast") {
                    game.isOnBattlefield("Dryad Arbor") shouldBe false
                    game.isInExile(1, "Dryad Arbor") shouldBe true
                }
            }
        }
    }
}
