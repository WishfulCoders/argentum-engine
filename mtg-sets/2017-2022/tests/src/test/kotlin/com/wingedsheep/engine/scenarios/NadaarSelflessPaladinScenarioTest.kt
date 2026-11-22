package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.player.DungeonProgressComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nadaar, Selfless Paladin (AFR #27): entering ventures into the dungeon (the player chooses which),
 * and the anthem for *other* creatures you control is on only once you've completed a dungeon —
 * whether or not Nadaar was around for it (ruling).
 */
class NadaarSelflessPaladinScenarioTest : ScenarioTestBase() {

    init {
        test("entering ventures into the dungeon of your choice") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nadaar, Selfless Paladin")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nadaar, Selfless Paladin").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Dungeon of the Mad Mage"))).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Dungeon of the Mad Mage"
            withClue("Yawning Portal: you gain 1 life") { game.getLifeTotal(1) shouldBe 21 }
        }

        test("other creatures you control get +1/+1 only once you've completed a dungeon") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nadaar, Selfless Paladin")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val nadaar = game.findPermanent("Nadaar, Selfless Paladin")!!
            val bears = game.findAllPermanents("Grizzly Bears")
            val mine = bears.single { game.state.projectedState.getController(it) == game.player1Id }
            val theirs = bears.single { it != mine }

            game.state.projectedState.getPower(mine) shouldBe 2

            game.state = game.state.updateEntity(game.player1Id) {
                it.with(DungeonProgressComponent(completed = listOf("Lost Mine of Phandelver")))
            }
            val projected = game.state.projectedState
            projected.getPower(mine) shouldBe 3
            projected.getToughness(mine) shouldBe 3
            withClue("Not Nadaar itself, and not an opponent's creature") {
                projected.getPower(nadaar) shouldBe 3
                projected.getPower(theirs) shouldBe 2
            }
        }
    }
}
