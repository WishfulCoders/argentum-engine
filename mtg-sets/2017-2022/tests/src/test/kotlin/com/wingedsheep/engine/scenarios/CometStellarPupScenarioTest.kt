package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.DiceRolls
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Comet, Stellar Pup (UNF #166) — "0: Roll a six-sided die." with four result rows whose loyalty
 * symbols are part of the effect, not a cost.
 *
 * Covers each row: 1-2 puts on two counters and makes two hasty Squirrels; 3 removes one and
 * returns a card with mana value 2 or less (only those are offered), or just removes one when there
 * is none; 4-5 deals damage equal to his loyalty *before* the [−2], to a creature or player chosen
 * on resolution — not targeted, so a hexproof creature can be chosen; 6 puts one on and allows two
 * more activations, additively when he rolls 6 again.
 */
class CometStellarPupScenarioTest : ScenarioTestBase() {

    private fun board(build: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Comet, Stellar Pup")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(build)
        .build()

    private fun TestGame.loyalty(): Int =
        state.getEntity(findPermanent("Comet, Stellar Pup")!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    /** Activate Comet's 0 with the d6 forced to [natural]; stops at any decision. */
    private fun TestGame.roll(natural: Int) {
        val comet = findPermanent("Comet, Stellar Pup")!!
        val ability = cardRegistry.getCard("Comet, Stellar Pup")!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        withClue("Comet's loyalty ability should be activatable") {
            execute(ActivateAbility(player1Id, comet, ability.id)).error shouldBe null
        }
        state = state.copy(rng = DiceRolls.rngRolling(6, natural))
        resolveStack()
    }

    private fun TestGame.canActivate(): Boolean {
        val comet = findPermanent("Comet, Stellar Pup")!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == comet }
    }

    init {
        test("1 or 2: [+2], then two 1/1 green Squirrels that have haste this turn") {
            val game = board()
            game.roll(2)
            game.loyalty() shouldBe 7
            val squirrels = game.findPermanents("Squirrel Token")
            squirrels.size shouldBe 2
            squirrels.forEach { game.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.findPermanents("Squirrel Token").forEach {
                game.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe false
            }
        }

        test("3: [−1], then return a card with mana value 2 or less — only those are offered") {
            val game = board {
                withCardInGraveyard(1, "Grizzly Bears")
                withCardInGraveyard(1, "Lightning Bolt")
                withCardInGraveyard(1, "Hill Giant")
            }
            game.roll(3)
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            decision.options shouldContainExactlyInAnyOrder listOf(bears, bolt)
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.loyalty() shouldBe 4
        }

        test("3 with no card of mana value 2 or less: just [−1] (ruling)") {
            val game = board { withCardInGraveyard(1, "Hill Giant") }
            game.roll(3)
            game.hasPendingDecision() shouldBe false
            game.loyalty() shouldBe 4
            game.isInGraveyard(1, "Hill Giant") shouldBe true
        }

        test("4 or 5: damage equal to his loyalty to a chosen player, then [−2]") {
            val game = board { withCardOnBattlefield(2, "Grizzly Bears") }
            game.roll(4)
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!! shouldContain game.player2Id
            decision.legalTargets[0]!! shouldContain game.player1Id
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 15
            game.loyalty() shouldBe 3
        }

        test("4 or 5: the creature is chosen, not targeted — a hexproof creature can be hit") {
            val game = board { withCardOnBattlefield(2, "Gladecover Scout") }
            game.roll(5)
            val scout = game.findPermanent("Gladecover Scout")!!
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!! shouldContain scout
            game.selectTargets(listOf(scout)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Gladecover Scout") shouldBe false
            game.loyalty() shouldBe 3
        }

        test("6: [+1], and two more activations this turn — three in all") {
            val game = board()
            game.roll(6)
            game.loyalty() shouldBe 6
            game.canActivate() shouldBe true
            game.roll(1)
            game.canActivate() shouldBe true
            game.roll(1)
            game.loyalty() shouldBe 10
            game.canActivate() shouldBe false
        }

        test("6 again adds two more again") {
            val game = board()
            game.roll(6)
            game.roll(6)
            game.loyalty() shouldBe 7
            repeat(3) {
                game.canActivate() shouldBe true
                game.roll(1)
            }
            game.canActivate() shouldBe false
        }

        test("without a 6, once per turn (CR 606.3)") {
            val game = board()
            game.roll(1)
            game.canActivate() shouldBe false
        }
    }
}
