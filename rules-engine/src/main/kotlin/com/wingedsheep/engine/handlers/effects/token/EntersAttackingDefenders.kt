package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EntersAttackingDefenderContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * The defenders of tokens created by a [CreateTokenEffect] with
 * [CreateTokenEffect.attackingEach] — "for each opponent, create … tapped and attacking that player
 * or a planeswalker they control" (Adeline, Resplendent Cathar).
 *
 * CR 508.4: a creature put onto the battlefield attacking attacks what its controller chooses "as
 * it enters the battlefield (unless the effect that put it onto the battlefield specifies what it's
 * attacking)". These effects specify *a side* — that player or one of their planeswalkers — and
 * leave the pick within it to the token's controller (Adeline's ruling: "You choose whether each
 * token is attacking that opponent or a planeswalker they control as those tokens enter").
 *
 * Every token of the one simultaneous creation is assigned before any of them is created, so the
 * picks are collected first, one decision per token whose side offers more than one choice, and
 * the effect is re-run with the answers so far (carried in [CHOICES_KEY]) until none is missing.
 * A side with only its player to attack — the usual board — needs no decision.
 */
internal object EntersAttackingDefenders {

    /** Pipeline slot carrying the picks already made, in token order, across the re-runs. */
    const val CHOICES_KEY = "resolution.entersAttackingDefenders"

    /** One creation batch: [count] tokens for [controllerId], attacking [side]'s player or planeswalkers. */
    data class Batch(val controllerId: EntityId, val side: EntityId, val defenders: List<EntityId?>)

    sealed interface Plan {
        data class Ready(val batches: List<Batch>) : Plan
        data class Paused(val result: EffectResult) : Plan
    }

    /**
     * What the tokens may attack on [side]'s behalf: that player, then each planeswalker they
     * control (projected control, CR 508.4a's "controlled by a defending player"). Nothing when
     * [side] is the token controller's own side or has left the game — such a token enters but
     * isn't attacking (CR 508.4a).
     */
    fun options(state: GameState, controllerId: EntityId, side: EntityId): List<EntityId> {
        if (side !in state.getOpponents(controllerId)) return emptyList()
        val projected = state.projectedState
        val planeswalkers = state.getBattlefield().filter {
            projected.isPlaneswalker(it) && projected.getController(it) == side
        }
        return listOf(side) + planeswalkers
    }

    /**
     * Assign a defender to every token, or pause for the next pick. [countFor] is the final
     * (post-replacement) number of tokens one batch creates for a controller.
     */
    fun plan(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext,
        controllerIds: List<EntityId>,
        sides: List<EntityId>,
        countFor: (EntityId) -> Int,
    ): Plan {
        val made = context.pipeline.storedCollections[CHOICES_KEY].orEmpty()
        var next = 0
        val batches = mutableListOf<Batch>()
        for (controllerId in controllerIds) {
            val count = countFor(controllerId)
            for (side in sides) {
                val options = options(state, controllerId, side)
                val defenders = mutableListOf<EntityId?>()
                repeat(count) {
                    defenders += when {
                        options.isEmpty() -> null
                        options.size == 1 -> options.single()
                        next < made.size -> made[next++].takeIf { it in options } ?: side
                        else -> return Plan.Paused(ask(state, effect, context, controllerId, side, options, made))
                    }
                }
                batches += Batch(controllerId, side, defenders)
            }
        }
        return Plan.Ready(batches)
    }

    private fun ask(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext,
        controllerId: EntityId,
        side: EntityId,
        options: List<EntityId>,
        made: List<EntityId>,
    ): EffectResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val tokenName = effect.name ?: "${effect.creatureTypes.joinToString(" ")} token"
        val sideName = state.getEntity(side)?.get<PlayerComponent>()?.name ?: "that player"
        val description = "$sideName or a planeswalker they control"
        val decision = { decisionId: String ->
            ChooseTargetsDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = "Choose what the $tokenName attacks",
                context = DecisionContext(
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION,
                ),
                targetRequirements = listOf(
                    TargetRequirementInfo(index = 0, description = description, minTargets = 1, maxTargets = 1)
                ),
                legalTargets = mapOf(0 to options),
            )
        }
        val continuation = EntersAttackingDefenderContinuation(
            originalEffect = effect,
            effectContext = context,
            chosen = made,
            options = options,
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}
