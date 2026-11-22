package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.mergeOutputs
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import kotlin.reflect.KClass

/**
 * Executor for DrawUpToEffect.
 *
 * "Draw up to N cards" — the player chooses how many (0 to maxCards).
 * Creates a ChooseNumberDecision and pushes a DrawUpToContinuation.
 * The continuation handler draws the chosen number of cards via DrawCardsEffect
 * through the registry (so draw replacement effects work correctly).
 *
 * If [DrawUpToEffect.storeNotDrawnAs] is set, stores the number of cards NOT drawn
 * (maxCards - chosen) as a named numeric variable in the effect context, allowing
 * subsequent pipeline effects to reference it via DynamicAmount.VariableReference.
 */
class DrawUpToExecutor(
    private val drawPrimitive: DrawCardPrimitive,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<DrawUpToEffect> {

    override val effectType: KClass<DrawUpToEffect> = DrawUpToEffect::class

    override fun execute(
        state: GameState,
        effect: DrawUpToEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.resolvePlayerTarget(effect.target, state)
            ?: return EffectResult.error(state, "DrawUpTo: could not resolve player target")

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val librarySize = state.getZone(libraryZone).size
        // A per-turn draw cap (CR 121.2b) means the player can't choose to draw more than it allows.
        val drawsLeft = drawPrimitive.drawsLeftThisTurn(state, playerId) ?: Int.MAX_VALUE
        val actualMax = effect.maxCards.coerceAtMost(librarySize).coerceAtMost(drawsLeft)

        if (actualMax == 0) {
            // Can't draw any cards: every card is "not drawn". Nothing paused, so the number is
            // this step's own output, not a hand-off to the frame beneath.
            val storeAs = effect.storeNotDrawnAs
            return if (storeAs != null) {
                EffectResult(state, emptyList(), updatedStoredNumbers = mapOf(storeAs to effect.maxCards))
            } else {
                EffectResult.success(state)
            }
        }

        val continuation = DrawUpToContinuation(
            playerId = playerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            maxCards = actualMax,
            originalMaxCards = effect.maxCards,
            storeNotDrawnAs = effect.storeNotDrawnAs,
            programOrigin = context.programOrigin,
        )

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose how many cards to draw (0-$actualMax)",
            minValue = 0,
            maxValue = actualMax,
            phase = DecisionPhase.RESOLUTION,
            answer = continuation
        )

        return EffectResult.propagatePause(
            decisionResult.state,
            decisionResult.events
        )
    }

    companion object {
        /**
         * Inject a stored number into the next EffectProgram on the stack, following the same
         * injection pattern as SelectFromCollectionContinuation. For resumers, after a pause: a
         * step that finishes without pausing returns the number in its result instead.
         */
        fun injectStoredNumber(state: GameState, name: String, value: Int): EffectResult {
            val nextFrame = state.peekContinuation()
            val newState = if (nextFrame is EffectProgram) {
                val (_, stateAfterPop) = state.popContinuation()
                stateAfterPop.pushContinuation(
                    nextFrame.copy(effectContext = nextFrame.effectContext.mergeOutputs(
                        com.wingedsheep.engine.handlers.PipelineOutputs(numbers = mapOf(name to value)),
                        dedupSnapshots = true,
                    ))
                )
            } else {
                state
            }
            return EffectResult.success(newState)
        }
    }
}
