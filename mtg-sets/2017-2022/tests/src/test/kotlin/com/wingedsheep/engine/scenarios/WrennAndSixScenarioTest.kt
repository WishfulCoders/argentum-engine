package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wrenn and Six (MH1 #217, {R}{G}, loyalty 3).
 *
 *   +1: Return up to one target land card from your graveyard to your hand.
 *   −1: Wrenn and Six deals 1 damage to any target.
 *   −7: You get an emblem with "Instant and sorcery cards in your graveyard have retrace."
 *
 * The emblem composes the emblem-owned graveyard-cast permission (Wrenn and Realmbreaker) with
 * Six's retrace discard cost, so the −7 test pins the composition: an instant is castable from
 * the graveyard only with a land card to discard, and a creature card is not.
 */
class WrennAndSixScenarioTest : ScenarioTestBase() {

    init {
        context("Wrenn and Six") {

            test("+1 returns a land card from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Six")
                    .withCardInGraveyard(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Six")!!
                setLoyalty(game, wrenn, 3)
                val forest = game.findCardsInGraveyard(1, "Forest").single()
                activate(game, wrenn, index = 0, targets = listOf(ChosenTarget.Card(forest, game.player1Id, Zone.GRAVEYARD)))
                game.resolveStack()

                game.isInHand(1, "Forest") shouldBe true
                loyalty(game, wrenn) shouldBe 4
            }

            test("−1 deals 1 damage to any target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Six")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Six")!!
                setLoyalty(game, wrenn, 3)
                activate(game, wrenn, index = 1, targets = listOf(ChosenTarget.Player(game.player2Id)))
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                loyalty(game, wrenn) shouldBe 2
            }

            test("the −7 emblem gives instants and sorceries in your graveyard retrace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Six")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Six")!!
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val forest = game.findCardsInHand(1, "Forest").single()

                withClue("before the emblem, Lightning Bolt can't be cast from the graveyard") {
                    graveyardCasts(game, bolt).size shouldBe 0
                }

                setLoyalty(game, wrenn, 7)
                activate(game, wrenn, index = 2)
                game.resolveStack()
                withClue("Wrenn died at 0 loyalty; the emblem outlives her") {
                    game.findPermanent("Wrenn and Six") shouldBe null
                }

                withClue("the emblem offers the instant, not the creature card") {
                    graveyardCasts(game, bears).size shouldBe 0
                    val cast = graveyardCasts(game, bolt).single()
                    cast.additionalCostInfo shouldNotBe null
                }

                val rider = (graveyardCasts(game, bolt).single().action as CastSpell).graveyardCastRider
                game.execute(
                    CastSpell(
                        game.player1Id, bolt,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                        graveyardCastRider = rider
                    )
                ).error shouldBe null
                withClue("retrace discarded the land card") { game.isInGraveyard(1, "Forest") shouldBe true }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("the retraced Bolt goes back to the graveyard, ready to be retraced again") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
            }

            test("without a land card to discard there is no retrace cast") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Six")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Six")!!
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                setLoyalty(game, wrenn, 7)
                activate(game, wrenn, index = 2)
                game.resolveStack()

                graveyardCasts(game, bolt).size shouldBe 0
            }
        }
    }

    private fun graveyardCasts(game: TestGame, cardId: EntityId) =
        game.getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard("Wrenn and Six")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
