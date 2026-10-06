package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "As an additional cost to cast this spell, you may exile any number of [white] cards from your
 * hand. This spell costs {2} less to cast for each card exiled this way." — the march cycle
 * ([com.wingedsheep.sdk.scripting.AdditionalCost.ExileCardsForCostReduction]).
 *
 * - CR 601.2b/601.2h: an optional additional cost whose size the caster chooses; exiling none is
 *   legal, and only matching cards from the named zone qualify — never the spell itself.
 * - CR 601.2f: the reduction applies to the total cost, whose generic part includes the announced
 *   value of X (CR 107.3a), but never to coloured mana; exiling more cards than it can use is legal
 *   (march rulings: "can't reduce the mana it costs to less than {W}").
 * - X itself is unchanged by the reduction: the spell's effect, its target's mana-value cap and its
 *   mana value on the stack see the announced X.
 */
class ExileCardsForCostReductionTest : ScenarioTestBase() {

    private fun TestGame.handCard(name: String): EntityId = findCardsInHand(1, name).first()

    private fun TestGame.cast(
        name: String,
        x: Int?,
        target: EntityId?,
        exiled: List<EntityId> = emptyList(),
    ) = execute(
        CastSpell(
            player1Id, handCard(name),
            targets = listOfNotNull(target).map { ChosenTarget.Permanent(it) },
            xValue = x,
            additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        )
    )

    init {
        cardRegistry.register(card("Test March") {
            manaCost = "{X}{W}"
            typeLine = "Instant"
            additionalCost(Costs.additional.ExileCardsForCostReduction(GameObjectFilter.Any.withColor(Color.WHITE), 2))
            spell { effect = Effects.Exile(target(TargetFilter.Creature.manaValueAtMostX())) }
        })
        cardRegistry.register(card("Test Fixed March") {
            manaCost = "{4}{W}"
            typeLine = "Sorcery"
            additionalCost(Costs.additional.ExileCardsForCostReduction(GameObjectFilter.Any.withColor(Color.WHITE), 2))
            spell { effect = Effects.GainLife(3) }
        })

        fun board(plains: Int) = scenario().withPlayers()
            .withCardInHand(1, "Test March")
            .withCardInHand(1, "Test Fixed March")
            .withCardInHand(1, "Savannah Lions") // white
            .withCardInHand(1, "Savannah Lions") // white
            .withCardInHand(1, "Grizzly Bears") // green
            .withLandsOnBattlefield(1, "Plains", plains)
            .withCardOnBattlefield(2, "Hill Giant") // mana value 4
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("with nothing exiled it costs its full {X}{W}") {
            val game = board(4).build()
            val giant = game.findPermanent("Hill Giant")!!
            val r = game.cast("Test March", 4, giant)
            // {4}{W} = five mana; four Plains can't pay it.
            r.error shouldNotBe null
            val game2 = board(5).build()
            val r2 = game2.cast("Test March", 4, game2.findPermanent("Hill Giant")!!)
            withClue("${r2.error}") { r2.error shouldBe null }
            game2.resolveStack()
            game2.isInExile(2, "Hill Giant") shouldBe true
        }

        test("each exiled white card pays {2} of X; the X the spell sees is still the announced one") {
            val game = board(1).build()
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findCardsInHand(1, "Savannah Lions")
            // X = 4: {4}{W} less {4} = {W}, one Plains.
            val r = game.cast("Test March", 4, giant, exiled = lions)
            withClue("${r.error}") { r.error shouldBe null }
            game.findCardsInHand(1, "Savannah Lions").size shouldBe 0
            game.state.getExile(game.player1Id).count { id ->
                game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ==
                    "Savannah Lions"
            } shouldBe 2
            game.resolveStack()
            // Mana value 4 ≤ X = 4 at resolution too.
            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("the reduction can't touch the coloured {W}") {
            val game = board(0).build()
            val lions = game.findCardsInHand(1, "Savannah Lions")
            game.cast("Test Fixed March", null, null, exiled = lions).error shouldNotBe null
        }

        test("exiling more than the reduction can use is legal") {
            val game = board(1).build()
            // Three white cards ({6}) against {4} of generic: it costs {W}, no less.
            val exiled = game.findCardsInHand(1, "Savannah Lions") + game.handCard("Test March")
            val r = game.cast("Test Fixed March", null, null, exiled = exiled)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }

        test("only one card exiled takes only {2} off a fixed cost") {
            val game = board(2).build()
            val oneLion = game.findCardsInHand(1, "Savannah Lions").take(1)
            // {4}{W} less {2} = {2}{W}: two Plains aren't enough.
            game.cast("Test Fixed March", null, null, exiled = oneLion).error shouldNotBe null
            val game2 = board(3).build()
            val r = game2.cast("Test Fixed March", null, null, exiled = game2.findCardsInHand(1, "Savannah Lions").take(1))
            withClue("${r.error}") { r.error shouldBe null }
            game2.findCardsInHand(1, "Savannah Lions").size shouldBe 1
        }

        test("a nonwhite card, or the spell itself, can't be exiled to pay") {
            val game = board(5).build()
            val giant = game.findPermanent("Hill Giant")!!
            game.cast("Test March", 4, giant, exiled = game.findCardsInHand(1, "Grizzly Bears")).error shouldNotBe null
            game.cast("Test March", 4, giant, exiled = listOf(game.handCard("Test March"))).error shouldNotBe null
        }

        test("legal actions offer a zero-minimum hand picker and price X with the possible exiles") {
            val game = board(1).build()
            val cast = game.getLegalActions(1).single { info ->
                (info.action as? CastSpell)?.cardId == game.handCard("Test March")
            }
            val costInfo = cast.additionalCostInfo!!
            costInfo.costType shouldBe "ExileFromHand"
            costInfo.exileMinCount shouldBe 0
            costInfo.validExileTargets.toSet() shouldBe game.findCardsInHand(1, "Savannah Lions").toSet() +
                game.handCard("Test Fixed March")
            // One Plains pays {W}; three white cards could pay {6} of X.
            cast.maxAffordableX shouldBe 6
        }
    }
}
