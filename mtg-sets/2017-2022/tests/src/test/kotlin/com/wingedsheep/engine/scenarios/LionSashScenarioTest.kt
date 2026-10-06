package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.neo.cards.LionSash
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lion Sash (NEO #26) — "{W}: Exile target card from a graveyard. If it was a permanent card, put a
 * +1/+1 counter on this permanent. Equipped creature gets +1/+1 for each +1/+1 counter on this
 * Equipment. Reconfigure {2}".
 */
class LionSashScenarioTest : ScenarioTestBase() {
    init {
        val exileId = LionSash.activatedAbilities[0].id
        val attachId = LionSash.activatedAbilities[1].id
        val unattachId = LionSash.activatedAbilities[2].id

        fun TestGame.sash(): EntityId = findPermanent("Lion Sash")!!
        fun TestGame.counters(): Int =
            state.getEntity(sash())?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.exileFromGraveyard(cardName: String, owner: Int) {
            val card = findCardsInGraveyard(owner, cardName).first()
            val ownerId = if (owner == 1) player1Id else player2Id
            val r = execute(
                ActivateAbility(
                    player1Id, sash(), exileId,
                    targets = listOf(ChosenTarget.Card(card, ownerId, com.wingedsheep.sdk.core.Zone.GRAVEYARD))
                )
            )
            withClue("${r.error}") { r.error shouldBe null }
            resolveStack()
        }

        fun board() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Lion Sash")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 6)
            .withCardInGraveyard(2, "Hill Giant")
            .withCardInGraveyard(2, "Lightning Bolt")
            .withCardInGraveyard(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("exiling a permanent card grows it; a nonpermanent card is exiled for nothing") {
            val game = board().build()
            game.exileFromGraveyard("Hill Giant", 2)
            game.isInExile(2, "Hill Giant") shouldBe true
            game.counters() shouldBe 1

            game.exileFromGraveyard("Lightning Bolt", 2)
            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.counters() shouldBe 1

            // A land is a permanent card too — from its controller's own graveyard.
            game.exileFromGraveyard("Forest", 1)
            game.counters() shouldBe 2
            game.state.projectedState.getPower(game.sash()) shouldBe 3
        }

        test("reconfigured onto a creature, its counters pump that creature and it stops being a creature") {
            val game = board().build()
            game.exileFromGraveyard("Hill Giant", 2)
            game.exileFromGraveyard("Forest", 1)
            val bears = game.findPermanent("Grizzly Bears")!!

            val r = game.execute(
                ActivateAbility(game.player1Id, game.sash(), attachId, targets = listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.isCreature(game.sash()) shouldBe false
            game.state.projectedState.hasSubtype(game.sash(), "Cat") shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4

            game.execute(ActivateAbility(game.player1Id, game.sash(), unattachId)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.isCreature(game.sash()) shouldBe true
            game.state.projectedState.getPower(game.sash()) shouldBe 3
            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
