package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dungeons.Dungeons
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dungeon Crawler (AFR #99): "Whenever you complete a dungeon, you may return this card from your
 * graveyard to your hand" triggers from the graveyard when the dungeon-completion state-based action
 * removes the dungeon (CR 309.7, 704.5t). The venture comes from Dungeon Map's sorcery-speed ability.
 */
class DungeonCrawlerScenarioTest : ScenarioTestBase() {

    init {
        test("completing a dungeon returns it from the graveyard to hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Dungeon Crawler")
                .withCardOnBattlefield(1, "Dungeon Map")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // The venture marker is on Fungi Cavern, one arrow from Lost Mine's last room.
            var s = DungeonService.enter(game.state, game.player1Id, Dungeons.LostMineOfPhandelver, "setup").first
            s = DungeonService.moveTo(s, game.player1Id, 5, "setup").first
            game.state = s

            val map = game.findPermanent("Dungeon Map")!!
            val venture = cardRegistry.getCard("Dungeon Map")!!.activatedAbilities[1].id
            game.execute(ActivateAbility(game.player1Id, map, venture)).error shouldBe null
            game.resolveStack()

            withClue("Temple of Dumathoin resolved, so the dungeon completed and the trigger asks") {
                DungeonService.completedCount(game.state, game.player1Id) shouldBe 1
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Dungeon Crawler") shouldBe true
            game.isInGraveyard(1, "Dungeon Crawler") shouldBe false
        }
    }
}
