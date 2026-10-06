package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yawgmoth's Will (USG #171): until end of turn you may play lands and cast spells from your
 * graveyard, and any card that would be put into your graveyard this turn is exiled instead — the
 * Will included, since its replacement is already in place as it finishes resolving (ruling).
 */
class YawgmothsWillScenarioTest : ScenarioTestBase() {

    init {
        context("Yawgmoth's Will") {
            test("play a land and cast a spell from the graveyard; everything bound for the graveyard is exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Yawgmoth's Will")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInGraveyard(1, "Dark Ritual")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val graveyardSwamp = game.findCardsInGraveyard(1, "Swamp").single()
                val ritual = game.findCardsInGraveyard(1, "Dark Ritual").single()
                withClue("before the Will nothing in the graveyard is playable") {
                    game.getLegalActions(1).none { (it.action as? PlayLand)?.cardId == graveyardSwamp } shouldBe true
                    game.getLegalActions(1).none { (it.action as? CastSpell)?.cardId == ritual } shouldBe true
                }

                game.castSpell(1, "Yawgmoth's Will").error shouldBe null
                game.resolveStack()
                withClue("the Will exiled itself instead of going to the graveyard") {
                    game.isInExile(1, "Yawgmoth's Will") shouldBe true
                    game.isInGraveyard(1, "Yawgmoth's Will") shouldBe false
                }

                withClue("the land is offered as a land play, never as a cast") {
                    game.getLegalActions(1).any { (it.action as? PlayLand)?.cardId == graveyardSwamp } shouldBe true
                    game.getLegalActions(1).none { (it.action as? CastSpell)?.cardId == graveyardSwamp } shouldBe true
                }
                game.execute(PlayLand(game.player1Id, graveyardSwamp)).error shouldBe null
                withClue("the graveyard Swamp was played") {
                    game.state.getBattlefield(game.player1Id).contains(graveyardSwamp) shouldBe true
                }

                game.castSpellFromGraveyard(1, "Dark Ritual").error shouldBe null
                game.resolveStack()
                withClue("Dark Ritual resolved, and was exiled instead of returning to the graveyard") {
                    game.isInExile(1, "Dark Ritual") shouldBe true
                    game.graveyardSize(1) shouldBe 0
                }
            }

            test("both halves end with the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Yawgmoth's Will")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Yawgmoth's Will").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // opponent's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // our next turn
                val graveyardSwamp = game.findCardsInGraveyard(1, "Swamp").single()
                withClue("the land permission is gone") {
                    game.getLegalActions(1).none { (it.action as? PlayLand)?.cardId == graveyardSwamp } shouldBe true
                }
            }
        }
    }
}
