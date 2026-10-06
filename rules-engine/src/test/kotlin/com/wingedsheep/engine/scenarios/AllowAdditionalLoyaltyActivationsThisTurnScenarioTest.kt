package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.AllowAdditionalLoyaltyActivationsThisTurnEffect]
 * — "you may activate [this planeswalker]'s loyalty ability two more times this turn" (Comet,
 * Stellar Pup's 6 row), driven through an inline instant that grants it to a target planeswalker.
 *
 * Proves, against CR 606.3 (one loyalty activation per permanent per turn): a grant adds exactly
 * two to the allowance, before or after the first activation; it is additive — a second grant adds
 * two more — unlike the "rather than only once" grant; it stacks on Oath of Teferi and on a
 * "twice" grant (max of those, plus the bonus); it is per permanent; and it lapses at end of turn.
 * The validator and the legal-action enumerator agree at every step.
 */
class AllowAdditionalLoyaltyActivationsThisTurnScenarioTest : ScenarioTestBase() {

    private val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val otherWalker = card("Other Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Other"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val treat = card("Test Treat") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You may activate loyalty abilities of target planeswalker two more times this turn."
        spell {
            val t = target(TargetFilter.Planeswalker)
            effect = Effects.AllowAdditionalLoyaltyActivationsThisTurn(target = t)
        }
    }

    private val rush = card("Test Rush") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You may activate loyalty abilities of target planeswalker twice this turn rather than only once."
        spell {
            val t = target(TargetFilter.Planeswalker)
            effect = Effects.AllowLoyaltyActivationsThisTurn(target = t)
        }
    }

    private val oath = card("Test Oath") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "You may activate the loyalty abilities of planeswalkers you control twice each turn rather than only once."
        staticAbility { ability = ExtraLoyaltyActivation }
    }

    private fun board(vararg extra: String, treats: Int = 1, rushes: Int = 0): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Walker")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(treats) { b.withCardInHand(1, "Test Treat") }
        repeat(rushes) { b.withCardInHand(1, "Test Rush") }
        extra.forEach { b.withCardOnBattlefield(1, it) }
        return b.build()
    }

    private fun TestGame.activate(name: String = "Test Walker") = run {
        val source = findPermanent(name)!!
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        execute(ActivateAbility(player1Id, source, ability.id))
    }

    /** Activate and resolve; asserts the activation was legal. */
    private fun TestGame.activateAndResolve(name: String = "Test Walker") {
        withClue("activation of $name should be legal") { activate(name).error shouldBe null }
        resolveStack()
    }

    private fun TestGame.canActivateOffered(name: String = "Test Walker"): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    private fun TestGame.cast(spell: String, name: String = "Test Walker") {
        castSpell(1, spell, targetId = findPermanent(name)!!).error shouldBe null
        resolveStack()
    }

    /** Activates [n] times, each legal, then asserts the next one is refused by both paths. */
    private fun TestGame.exactly(n: Int, name: String = "Test Walker") {
        repeat(n) { activateAndResolve(name) }
        canActivateOffered(name) shouldBe false
        activate(name).error shouldNotBe null
    }

    init {
        listOf(walker, otherWalker, treat, rush, oath).forEach { cardRegistry.register(it) }

        test("a grant after the first activation allows exactly two more (Comet's 6)") {
            val game = board()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.cast("Test Treat")
            game.exactly(2)
            game.getLifeTotal(1) shouldBe 23
        }

        test("a grant before any activation makes three in all") {
            val game = board()
            game.cast("Test Treat")
            game.exactly(3)
        }

        test("two grants are additive — two more, then two more again") {
            val game = board(treats = 2)
            game.activateAndResolve()
            game.cast("Test Treat")
            game.activateAndResolve()
            game.cast("Test Treat")
            game.exactly(3)
        }

        test("stacks on Oath of Teferi: twice, plus two more") {
            val game = board("Test Oath")
            game.cast("Test Treat")
            game.exactly(4)
        }

        test("stacks on a 'twice rather than only once' grant") {
            val game = board(rushes = 1)
            game.cast("Test Rush")
            game.cast("Test Treat")
            game.exactly(4)
        }

        test("the grant is per permanent — another planeswalker stays at once") {
            val game = board("Other Walker")
            game.cast("Test Treat", "Test Walker")
            game.exactly(1, "Other Walker")
            game.exactly(3, "Test Walker")
        }

        test("the bonus lapses at end of turn") {
            val game = board()
            game.cast("Test Treat")
            game.state.getEntity(game.findPermanent("Test Walker")!!)
                ?.get<AbilityActivatedThisTurnComponent>().shouldNotBeNull()
                .loyaltyActivationBonus shouldBe 2
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.findPermanent("Test Walker")!!)
                ?.get<AbilityActivatedThisTurnComponent>() shouldBe null
        }
    }
}
