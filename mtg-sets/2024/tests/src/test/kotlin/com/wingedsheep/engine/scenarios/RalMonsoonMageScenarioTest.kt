package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ral, Monsoon Mage // Ral, Leyline Prodigy (MH3 #247).
 *
 * Front: instants and sorceries cost {1} less; casting one during your turn flips a coin — lose and
 * Ral deals 1 damage to you, win and you may exile him and return him transformed.
 * Back: enters with an extra loyalty per instant/sorcery cast this turn; +1 discounts instants and
 * sorceries until your next turn; −2 divides 2 damage and draws if you control another blue permanent.
 *
 * Coin-flip tests read the emitted [CoinFlipEvent] rather than pinning a seed to an outcome.
 */
class RalMonsoonMageScenarioTest : ScenarioTestBase() {

    private val front = "Ral, Monsoon Mage"
    private val back = "Ral, Leyline Prodigy"

    private fun costOf(game: TestGame, cardName: String, player: EntityId = game.player1Id): Int {
        val calculator = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator)
        return calculator.calculateEffectiveCost(game.state, cardRegistry.requireCard(cardName), player).cmc
    }

    private fun faceName(game: TestGame, id: EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun abilityId(index: Int) = cardRegistry.requireCard(back).script.activatedAbilities[index].id

    /** Pay any mana prompt automatically and drain the stack, collecting every event. */
    private fun settle(game: TestGame, answerMay: Boolean, events: MutableList<GameEvent>) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is SelectManaSourcesDecision -> events += game.submitManaSourcesAutoPay().events
                is YesNoDecision -> events += game.answerYesNo(answerMay).events
                null -> if (game.state.stack.isNotEmpty()) {
                    events += game.resolveStack().flatMap { it.events }
                } else return
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun castShockAtOpponent(game: TestGame, answerMay: Boolean): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val cast = game.castSpellTargetingPlayer(1, "Shock", 2)
        cast.error shouldBe null
        events += cast.events
        settle(game, answerMay, events)
        return events
    }

    private fun frontOnMyTurn(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, front)
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun prodigyOnMyTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("front face: instant and sorcery spells you cast cost {1} less, other spells don't") {
            val game = frontOnMyTurn()
            costOf(game, "Divination") shouldBe 2 // {2}{U}
            costOf(game, "Think Twice") shouldBe 1 // {1}{U}
            costOf(game, "Grizzly Bears") shouldBe 2 // creature, untouched
            withClue("the opponent's spells aren't discounted") {
                costOf(game, "Divination", game.player2Id) shouldBe 3
            }
        }

        test("casting an instant on your turn flips a coin: lose deals 1 to you, win may transform him") {
            val seen = mutableSetOf<Boolean>()
            for (attempt in 1..40) {
                val game = frontOnMyTurn()
                val ral = game.findPermanent(front)!!
                val events = castShockAtOpponent(game, answerMay = true)
                val flips = events.filterIsInstance<CoinFlipEvent>()
                withClue("exactly one flip per instant cast") { flips.size shouldBe 1 }
                val won = flips.single().won
                seen += won
                withClue("Shock resolved either way") { game.getLifeTotal(2) shouldBe 18 }
                if (won) {
                    game.getLifeTotal(1) shouldBe 20
                    val prodigy = game.findPermanent(back)
                    withClue("won + yes: Ral returned transformed") { prodigy shouldNotBe null }
                    game.findPermanent(front) shouldBe null
                    withClue("printed loyalty 2 + 1 instant cast this turn") {
                        loyalty(game, prodigy!!) shouldBe 3
                    }
                    game.state.getEntity(prodigy!!)?.get<CardComponent>()?.ownerId shouldBe game.player1Id
                } else {
                    withClue("lost: Ral deals 1 damage to you and stays a creature") {
                        game.getLifeTotal(1) shouldBe 19
                        faceName(game, ral) shouldBe front
                    }
                }
                if (seen.size == 2) break
            }
            withClue("both outcomes of the flip were exercised") { seen shouldBe setOf(true, false) }
        }

        test("winning the flip and declining leaves Ral on his front face") {
            var checked = false
            for (attempt in 1..40) {
                val game = frontOnMyTurn()
                val ral = game.findPermanent(front)!!
                val events = castShockAtOpponent(game, answerMay = false)
                if (!events.filterIsInstance<CoinFlipEvent>().single().won) continue
                faceName(game, ral) shouldBe front
                game.findPermanent(back) shouldBe null
                game.getLifeTotal(1) shouldBe 20
                checked = true
                break
            }
            checked shouldBe true
        }

        test("the cast trigger doesn't fire during an opponent's turn") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val events = castShockAtOpponent(game, answerMay = true)
            events.filterIsInstance<CoinFlipEvent>().size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 18
            game.findPermanent(front) shouldNotBe null
        }

        test("+1: instants and sorceries cost {1} less through the opponent's turn, gone on your next turn") {
            val game = prodigyOnMyTurn()
            val ral = game.findPermanent(back)!!
            costOf(game, "Think Twice") shouldBe 2

            game.execute(ActivateAbility(game.player1Id, ral, abilityId(0))).error shouldBe null
            game.resolveStack()
            loyalty(game, ral) shouldBe 3
            costOf(game, "Think Twice") shouldBe 1
            costOf(game, "Grizzly Bears") shouldBe 2

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the discount lasts until your next turn, so it covers an instant now") {
                costOf(game, "Think Twice") shouldBe 1
            }
            costOf(game, "Think Twice", game.player2Id) shouldBe 2

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("expired as your next turn began") { costOf(game, "Think Twice") shouldBe 2 }
        }

        test("−2 splits 2 damage and doesn't draw when Ral is the only blue permanent") {
            val game = prodigyOnMyTurn {
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val ral = game.findPermanent(back)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(ral) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
            }
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(
                    game.player1Id, ral, abilityId(1),
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()
            // The division is asked for as the ability resolves (the composite "…then draw" shape,
            // shared with Electrolyze, isn't pre-divided at activation).
            if (game.getPendingDecision() is DistributeDecision) {
                game.submitDistribution(mapOf(bears to 1, game.player2Id to 1)).error shouldBe null
                game.resolveStack()
            }

            loyalty(game, ral) shouldBe 2
            game.getLifeTotal(2) shouldBe 19
            game.state.getEntity(bears)?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
                ?.amount shouldBe 1
            withClue("Ral himself is blue, but 'other than Ral' excludes him") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("−2 draws a card when you control another blue permanent") {
            val game = prodigyOnMyTurn {
                withCardOnBattlefield(1, "Wind Drake")
            }
            val ral = game.findPermanent(back)!!
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(
                    game.player1Id, ral, abilityId(1),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe handBefore + 1
        }
    }
}
