package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.LoyaltyAbilitiesAtInstantSpeed
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for [LoyaltyAbilitiesAtInstantSpeed] — "you may activate this permanent's loyalty
 * abilities any time you could cast an instant" (The Wandering Emperor).
 *
 * Pins, against CR 606.3 and the Wandering Emperor ruling (2022-02-18): the permission lifts the
 * timing half of CR 606.3 (opponent's turn, non-main step, non-empty stack — CR 117.1a), but not the
 * once-per-turn half; it covers only the permanent that has it; a [Conditions.SourceEnteredThisTurn]
 * gate is read live (a walker that entered this turn — including by being flashed in on the opponent's
 * turn — qualifies, one that has been there since an earlier turn does not). The legal-action
 * enumerator and the activation validator agree at every step.
 */
class LoyaltyAbilitiesAtInstantSpeedScenarioTest : ScenarioTestBase() {

    private val alwaysWalker = card("Instant Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "You may activate this permanent's loyalty abilities any time you could cast an instant.\n0: You gain 1 life."
        staticAbility { ability = LoyaltyAbilitiesAtInstantSpeed }
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val emperor = card("Test Emperor") {
        manaCost = "{0}"
        typeLine = "Legendary Planeswalker — Emperor"
        startingLoyalty = 3
        oracleText = "Flash\nAs long as Test Emperor entered this turn, you may activate her loyalty abilities any time you could cast an instant.\n0: You gain 1 life."
        keywords(Keyword.FLASH)
        staticAbility {
            condition = Conditions.SourceEnteredThisTurn
            ability = LoyaltyAbilitiesAtInstantSpeed
        }
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val plainWalker = card("Plain Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Plain"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val quickSpell = card("Quick Spell") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    private fun TestGame.activate(name: String) = run {
        val source = findPermanent(name)!!
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        execute(ActivateAbility(player1Id, source, ability.id))
    }

    private fun TestGame.offered(name: String): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    /** Resolve the stack, then have the active opponent pass so player 1 holds priority again. */
    private fun TestGame.resolveAndTakePriority() {
        resolveStack()
        if (state.priorityPlayerId == player2Id) passPriority()
        state.priorityPlayerId shouldBe player1Id
    }

    /** The opponent's precombat main phase, player 1 holding priority. */
    private fun opponentsTurn(vararg walkers: String, enteredThisTurn: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply { walkers.forEach { withCardOnBattlefield(1, it, enteredThisTurn = enteredThisTurn) } }

    init {
        cardRegistry.register(alwaysWalker)
        cardRegistry.register(emperor)
        cardRegistry.register(plainWalker)
        cardRegistry.register(quickSpell)

        test("baseline CR 606.3: a planeswalker without the permission can't activate on an opponent's turn") {
            val game = opponentsTurn("Plain Walker").build()
            game.offered("Plain Walker") shouldBe false
            game.activate("Plain Walker").error shouldNotBe null
        }

        test("the permission allows activation on an opponent's turn, still only once per turn") {
            val game = opponentsTurn("Instant Walker", "Plain Walker").build()
            game.offered("Instant Walker") shouldBe true
            game.activate("Instant Walker").error shouldBe null
            game.resolveAndTakePriority()
            game.getLifeTotal(1) shouldBe 21

            withClue("CR 606.3's one-activation-per-turn limit still applies") {
                game.offered("Instant Walker") shouldBe false
                game.activate("Instant Walker").error shouldNotBe null
            }
            withClue("the permission covers only the permanent that has it") {
                game.offered("Plain Walker") shouldBe false
                game.activate("Plain Walker").error shouldNotBe null
            }
        }

        test("the permission allows activation in your own combat step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Instant Walker")
                .withCardOnBattlefield(1, "Plain Walker")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()
            game.offered("Plain Walker") shouldBe false
            game.offered("Instant Walker") shouldBe true
            game.activate("Instant Walker").error shouldBe null
        }

        test("the permission allows activation while a spell is on the stack (CR 117.1a)") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Instant Walker")
                .withCardOnBattlefield(1, "Plain Walker")
                .withCardInHand(1, "Quick Spell")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Quick Spell").error shouldBe null
            game.state.stack.isEmpty() shouldBe false
            game.state.priorityPlayerId shouldBe game.player1Id
            game.offered("Plain Walker") shouldBe false
            game.activate("Plain Walker").error shouldNotBe null
            game.offered("Instant Walker") shouldBe true
            game.activate("Instant Walker").error shouldBe null
        }

        test("entered-this-turn gate: a walker that has been there since an earlier turn gets no permission") {
            val game = opponentsTurn("Test Emperor", enteredThisTurn = false).build()
            game.offered("Test Emperor") shouldBe false
            game.activate("Test Emperor").error shouldNotBe null
        }

        test("entered-this-turn gate: a walker that entered this turn gets the permission") {
            val game = opponentsTurn("Test Emperor", enteredThisTurn = true).build()
            game.offered("Test Emperor") shouldBe true
            game.activate("Test Emperor").error shouldBe null
        }

        test("flashed in on the opponent's turn, it can activate that turn but not on the next turn outside a main phase") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Test Emperor")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Test Emperor").error shouldBe null
            game.resolveAndTakePriority()
            game.isOnBattlefield("Test Emperor") shouldBe true

            game.offered("Test Emperor") shouldBe true
            game.activate("Test Emperor").error shouldBe null
            game.resolveAndTakePriority()
            game.getLifeTotal(1) shouldBe 21

            // Player 1's next turn: she no longer entered "this turn", so in the upkeep (not a main
            // phase) her loyalty abilities are back to sorcery timing.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.priorityPlayerId shouldBe game.player1Id
            game.offered("Test Emperor") shouldBe false
            game.activate("Test Emperor").error shouldNotBe null
        }
    }
}
