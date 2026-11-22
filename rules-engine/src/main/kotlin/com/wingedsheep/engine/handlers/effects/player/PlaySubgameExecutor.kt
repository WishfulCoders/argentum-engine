package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SubgameRequest
import com.wingedsheep.engine.core.TurnStartFollowUp
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.PlaySubgameEffect
import kotlin.reflect.KClass

/**
 * Executor for [PlaySubgameEffect] (CR 729, "players play a Magic subgame").
 *
 * The subgame is a whole separate game, so it can't be played from inside an effect executor.
 * Like [RestartGameExecutor] this executor only records the request on the state; the settle
 * boundary ([com.wingedsheep.engine.core.Settler]) starts the subgame through
 * [com.wingedsheep.engine.core.Subgames] once the effect has run. The resolving spell stays on the
 * stack meanwhile, and the rest of its text — [PlaySubgameEffect.afterSubgame] — runs in the main
 * game with this context when the subgame is over, before the spell finishes resolving (CR 729.5).
 */
class PlaySubgameExecutor : EffectExecutor<PlaySubgameEffect> {

    override val effectType: KClass<PlaySubgameEffect> = PlaySubgameEffect::class

    override fun execute(
        state: GameState,
        effect: PlaySubgameEffect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val followUp = effect.afterSubgame?.let { TurnStartFollowUp(effect = it, context = context) }
        return EffectResult.success(
            state.copy(pendingSubgame = SubgameRequest(sourceName, effect.nonWinners, followUp))
        )
    }
}
