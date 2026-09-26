package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thought-Stalker Warlock (BLB): "choose target opponent. If THEY lost life this turn, …"
 *
 * Regression guard: the condition used to be Conditions.OpponentLostLifeThisTurn, which is
 * true when ANY opponent lost life — in multiplayer you could target opponent B (who lost
 * nothing) and still get the reveal-hand branch because opponent C bled earlier. The
 * condition must bind to the chosen target: PlayerLostLifeThisTurn(Player.ContextPlayer(0)).
 */
class ThoughtStalkerWarlockConditionTest : FunSpec({

    val vanilla = CardDefinition.creature(
        name = "Condition Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(com.wingedsheep.sdk.core.Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun initFourPlayerGame(): Pair<com.wingedsheep.engine.state.GameState, List<com.wingedsheep.sdk.model.EntityId>> {
        val registry = CardRegistry()
        registry.register(vanilla)
        val deck = Deck(cards = List(40) { "Condition Test Bear" })
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..4).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    test("condition binds to the chosen target opponent, not any opponent") {
        val (initial, players) = initFourPlayerGame()
        // players[3] lost life this turn; players[1] and players[2] did not.
        val state = initial.updateEntity(players[3]) { it.with(LifeLostThisTurnComponent) }

        val evaluator = PredicateEvaluator(cardRegistry = null).conditions
        val fixed = Conditions.PlayerLostLifeThisTurn(Player.ContextPlayer(0))

        fun contextTargeting(target: com.wingedsheep.sdk.model.EntityId) = EffectContext(
            sourceId = null,
            controllerId = players[0],
            targets = listOf(ChosenTarget.Player(target))
        )

        // Targeting the bleeding opponent → true; targeting an untouched opponent → false.
        evaluator.evaluate(state, fixed, contextTargeting(players[3])) shouldBe true
        evaluator.evaluate(state, fixed, contextTargeting(players[1])) shouldBe false

        // The old any-opponent condition is true regardless of the chosen target — the bug.
        val old = Conditions.OpponentLostLifeThisTurn
        evaluator.evaluate(state, old, contextTargeting(players[1])) shouldBe true
    }
})
