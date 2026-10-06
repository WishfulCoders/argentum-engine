package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deathrite Shaman (RTR #213).
 *
 * The {T} ability targets a land card in a graveyard, so it is not a mana ability: it goes on the
 * stack, and the mana arrives when it resolves.
 */
class DeathriteShamanScenarioTest : ScenarioTestBase() {
    init {
        test("{T}: exiles a land card from any graveyard and adds one mana of the chosen color, via the stack") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withCardInGraveyard(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val shaman = game.findPermanent("Deathrite Shaman")!!
            val forest = game.findCardsInGraveyard(2, "Forest").single()
            val ability = cardRegistry.getCard("Deathrite Shaman")!!.activatedAbilities[0]

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = shaman,
                    abilityId = ability.id,
                    targets = listOf(ChosenTarget.Card(forest, game.player2Id, Zone.GRAVEYARD)),
                )
            ).error shouldBe null

            withClue("not a mana ability: it waits on the stack") { game.state.stack.size shouldBe 1 }

            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            val decision = game.getPendingDecision() as ChooseColorDecision
            game.submitDecision(ColorChosenResponse(decision.id, Color.BLUE)).error shouldBe null

            withClue("the land card is exiled") { game.isInExile(2, "Forest") shouldBe true }
            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            withClue("one blue mana added") { pool.getAmount(Color.BLUE) shouldBe 1 }
        }

        test("{B}, {T}: exiles an instant from a graveyard and each opponent loses 2 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInGraveyard(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val shaman = game.findPermanent("Deathrite Shaman")!!
            val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
            val ability = cardRegistry.getCard("Deathrite Shaman")!!.activatedAbilities[1]

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = shaman,
                    abilityId = ability.id,
                    targets = listOf(ChosenTarget.Card(bolt, game.player2Id, Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
