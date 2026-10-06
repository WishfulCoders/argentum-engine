package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Myr Battlesphere (SOM #180).
 *
 * - ETB: four 1/1 colorless Myr artifact creature tokens.
 * - Attack: tap X untapped Myr (any Myr, chosen on resolution); +X/+0 and X damage to "the player or
 *   planeswalker it's attacking" — `EffectTarget.AttackedPlayerOrPlaneswalker(Self)`, so an attack
 *   on a planeswalker burns the planeswalker, not its controller.
 */
class MyrBattlesphereScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(name: String) =
        findPermanent(name)?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) } ?: 0

    private fun battlesphereBoard(vararg opponent: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Myr Battlesphere", summoningSickness = false)
            .withCardOnBattlefield(1, "Silver Myr")
            .withCardOnBattlefield(1, "Gold Myr")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponent.forEach { builder.withCardOnBattlefield(2, it) }
        return builder.build()
    }

    init {
        test("enters with four 1/1 colorless Myr artifact creature tokens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Myr Battlesphere")
                .withLandsOnBattlefield(1, "Plains", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Myr Battlesphere").error shouldBe null
            game.resolveStack()
            val myr = game.findAllPermanents("Myr Token")
            myr.size shouldBe 4
            myr.forEach { id ->
                val projected = game.state.projectedState
                projected.getPower(id) shouldBe 1
                projected.isCreature(id) shouldBe true
                projected.hasType(id, "ARTIFACT") shouldBe true
            }
        }

        test("attacking a planeswalker: tapping two Myr burns the planeswalker for 2 and pumps +2/+0") {
            val game = battlesphereBoard("Ajani Goldmane")
            val start = game.loyalty("Ajani Goldmane")
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Myr Battlesphere" to "Ajani Goldmane"),
            ).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val silver = game.findPermanent("Silver Myr")!!
            val gold = game.findPermanent("Gold Myr")!!
            withClue("only untapped Myr are offered — the Bears aren't Myr") {
                decision.options shouldContainExactlyInAnyOrder listOf(silver, gold)
            }
            game.selectCards(listOf(silver, gold)).error shouldBe null

            game.state.getEntity(silver)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(gold)!!.has<TappedComponent>() shouldBe true
            game.state.projectedState.getPower(game.findPermanent("Myr Battlesphere")!!) shouldBe 6
            withClue("the X damage hits the planeswalker it's attacking, not its controller") {
                game.loyalty("Ajani Goldmane") shouldBe start - 2
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("attacking a player and tapping none: no pump, no damage") {
            val game = battlesphereBoard()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Myr Battlesphere" to 2)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList()).error shouldBe null

            game.state.projectedState.getPower(game.findPermanent("Myr Battlesphere")!!) shouldBe 4
            game.getLifeTotal(2) shouldBe 20
        }

        test("attacking a player and tapping one: the player takes 1") {
            val game = battlesphereBoard()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Myr Battlesphere" to 2)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findPermanent("Gold Myr")!!)).error shouldBe null
            game.getLifeTotal(2) shouldBe 19
        }
    }
}
