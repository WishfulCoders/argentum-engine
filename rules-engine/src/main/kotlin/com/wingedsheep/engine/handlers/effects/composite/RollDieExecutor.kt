package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RollDieEffect
import kotlin.reflect.KClass

/**
 * Executor for [RollDieEffect] — the one place a die is rolled (CR 706).
 *
 * The natural result is drawn from the game's seeded RNG ([GameState.nextRandom]) as a uniform
 * integer in `1..sides` (CR 706.1a: N equally likely outcomes numbered 1 to N), so a game replayed
 * from its seed rolls the same numbers. The instruction's own modifier, if any, is added to give the
 * result (CR 706.2); it is evaluated against the post-roll state but no state changes in between, so
 * "subtract the number of cards in your hand" sees the hand as it is during the roll.
 *
 * The result is published under [RollDieEffect.storeResultAs] through
 * [EffectResult.updatedStoredNumbers], exactly as [FlipCoinsExecutor] publishes its heads tally, so
 * the results-table rows (`Patterns.Mechanic.rollDie`) and any later step read it as a stored
 * number. One [DieRolledEvent] is emitted per roll.
 */
class RollDieExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<RollDieEffect> {

    override val effectType: KClass<RollDieEffect> = RollDieEffect::class

    override fun execute(
        state: GameState,
        effect: RollDieEffect,
        context: EffectContext
    ): EffectResult {
        val (face, rolled) = state.nextRandom { nextInt(effect.sides) }
        val natural = face + 1
        val modifier = effect.modifier?.let { amountEvaluator.evaluate(rolled, it, context) } ?: 0
        val result = natural + modifier

        val sourceName = context.sourceId
            ?.let { rolled.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val event = DieRolledEvent(
            playerId = context.controllerId,
            sides = effect.sides,
            naturalResult = natural,
            result = result,
            sourceId = context.sourceId ?: context.controllerId,
            sourceName = sourceName
        )
        return EffectResult(
            state = rolled,
            events = listOf(event),
            updatedStoredNumbers = mapOf(effect.storeResultAs to result)
        )
    }
}
