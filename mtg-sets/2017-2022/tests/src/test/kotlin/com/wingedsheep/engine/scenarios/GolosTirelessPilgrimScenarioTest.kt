package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Golos, Tireless Pilgrim (M20 #226) — {2}{W}{U}{B}{R}{G}: exile the top three cards of your
 * library; you may play them this turn without paying their mana costs. "Play" includes a land,
 * which still uses the turn's land drop.
 */
class GolosTirelessPilgrimScenarioTest : ScenarioTestBase() {
    init {
        test("the exiled spell casts for free and the exiled land can be played") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Golos, Tireless Pilgrim")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ability = cardRegistry.getCard("Golos, Tireless Pilgrim")!!.script.activatedAbilities.single()
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Golos, Tireless Pilgrim")!!, ability.id)
            ).error shouldBe null
            game.resolveStack()

            withClue("the top three are exiled; the fourth stays in the library") {
                game.isInExile(1, "Hill Giant") shouldBe true
                game.isInExile(1, "Forest") shouldBe true
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe 1
            }

            withClue("all seven lands paid the ability, yet the Giant casts for free") {
                game.castSpellFromExile(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            val forest = game.state.getExile(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
            }
            withClue("the exiled Forest is played with the turn's land drop") {
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                game.isInExile(1, "Forest") shouldBe false
            }
        }
    }
}
