package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsOpponentsTurn
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "During an opponent's turn" (Unwelcome Sprite) must not fire on an ally's turn. In Team vs. Team
 * each teammate takes their own turn (CR 808.4), so an ally's turn is "not your turn" but is never an
 * opponent's turn.
 */
class IsOpponentsTurnConditionTest : FunSpec({

    val registry = CardRegistry().also { it.register(TestCards.all) }
    val conditions = PredicateEvaluator(registry).conditions

    fun boot(format: Format): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                format = format,
                players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    fun GameState.holds(condition: com.wingedsheep.sdk.scripting.conditions.Condition, controller: EntityId) =
        conditions.evaluate(this, condition, EffectContext(sourceId = null, controllerId = controller))

    test("Team vs. Team: an ally's turn is not an opponent's turn") {
        val (state, p) = boot(Format.TeamVsTeam())
        state.activePlayerId shouldBe p[0]
        state.holds(IsOpponentsTurn, p[0]) shouldBe false // own turn
        state.holds(IsOpponentsTurn, p[1]) shouldBe false // ally's turn
        state.holds(IsNotYourTurn, p[1]) shouldBe true // still "not your turn"
        state.holds(IsOpponentsTurn, p[2]) shouldBe true
        state.holds(IsOpponentsTurn, p[3]) shouldBe true
    }

    test("Two-Headed Giant: the shared team turn is never an opponent's turn for either head") {
        val (state, p) = boot(Format.TwoHeadedGiant())
        state.holds(IsOpponentsTurn, p[0]) shouldBe false
        state.holds(IsOpponentsTurn, p[1]) shouldBe false
        state.holds(IsOpponentsTurn, p[2]) shouldBe true
        state.holds(IsOpponentsTurn, p[3]) shouldBe true
    }
})
