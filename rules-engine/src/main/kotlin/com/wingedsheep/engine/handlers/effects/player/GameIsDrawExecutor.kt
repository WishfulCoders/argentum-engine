package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GameIsDrawEffect
import kotlin.reflect.KClass

/**
 * Executor for [GameIsDrawEffect] — "the game is a draw" (CR 104.4c).
 *
 * Ends the game at once with no winner. Nobody loses, so "can't lose the game" and "your
 * opponents can't win the game" grants don't stop it. The settle step sees `gameOver` and drops
 * every pending trigger.
 */
class GameIsDrawExecutor : EffectExecutor<GameIsDrawEffect> {

    override val effectType: KClass<GameIsDrawEffect> = GameIsDrawEffect::class

    override fun execute(
        state: GameState,
        effect: GameIsDrawEffect,
        context: EffectContext
    ): EffectResult {
        if (state.gameOver) return EffectResult.success(state)
        return EffectResult.success(
            state.copy(gameOver = true, winnerId = null),
            listOf(GameEndedEvent(winnerId = null, reason = GameEndReason.DRAW))
        )
    }
}
