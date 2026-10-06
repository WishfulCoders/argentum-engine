package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.RestrictDrawsPerTurn
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The per-turn draw caps of [RestrictDrawsPerTurn] ("Each opponent can't draw more than one card
 * each turn" — Narset, Parter of Veils; Leovold, Emissary of Trest).
 *
 * A cap is a "can't" effect (CR 101.2, CR 614.17), so it is consulted **before** any replacement
 * effect is offered the draw: a draw it forbids can't be replaced (CR 614.17c) and simply doesn't
 * happen. [DrawLoop] asks [remainingAllowance] at the top of every individual draw (CR 121.2), and
 * the announcement step, "draw up to N" and optional "you may draw" gates ask it before offering a
 * draw at all (CR 121.3).
 *
 * The count compared against the cap is [CardsDrawnThisTurnComponent] — cards actually drawn this
 * turn, so a draw that was replaced (dredge) or that failed on an empty library didn't use any of the
 * allowance, while draws made before the capping permanent entered do (the Narset ruling).
 *
 * Abilities are collected the way every rule-level static is ([com.wingedsheep.engine.core]'s untap
 * limits): battlefield permanents that are face up and haven't lost all abilities, with text changes,
 * unlocked Room faces, [ConditionalStaticAbility] and [CompositeStaticAbility] honoured, plus
 * abilities granted to permanents or players (emblems) while their grant lasts. Control is read from
 * projected state. "Opponent" is team-aware ([GameState.isOpponentOf]).
 */
object DrawLimits {

    /**
     * How many more cards [playerId] may draw this turn, or `null` when no cap binds them. Never
     * negative — `0` means every further draw this turn is forbidden.
     */
    fun remainingAllowance(
        state: GameState,
        cardRegistry: CardRegistry,
        conditions: ConditionEvaluator,
        playerId: EntityId,
    ): Int? {
        val cap = capFor(state, cardRegistry, conditions, playerId) ?: return null
        val drawn = state.getEntity(playerId)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
        return (cap - drawn).coerceAtLeast(0)
    }

    /** The smallest [RestrictDrawsPerTurn.maxPerTurn] binding [playerId], or `null` if none does. */
    fun capFor(
        state: GameState,
        cardRegistry: CardRegistry,
        conditions: ConditionEvaluator,
        playerId: EntityId,
    ): Int? {
        // Draws are frequent, so a permanent or grant with no cap anywhere in it is skipped before
        // any condition is evaluated or text change applied (the `mayCap` pre-filter below).
        val projected = state.projectedState
        var cap: Int? = null

        fun collect(ability: StaticAbility, source: EntityId, controller: EntityId) {
            when (ability) {
                is ConditionalStaticAbility -> {
                    if (conditions.evaluate(state, ability.condition, EffectContext(sourceId = source, controllerId = controller))) {
                        collect(ability.ability, source, controller)
                    }
                }
                is CompositeStaticAbility -> ability.abilities.forEach { collect(it, source, controller) }
                is RestrictDrawsPerTurn -> {
                    if (binds(state, ability.affected, controller, playerId)) {
                        cap = cap?.let { minOf(it, ability.maxPerTurn) } ?: ability.maxPerTurn
                    }
                }
                else -> Unit
            }
        }

        val battlefield = state.getBattlefield()
        for (source in battlefield) {
            val entity = state.getEntity(source) ?: continue
            if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(source)) continue
            val definition = entity.get<CardComponent>()?.let { cardRegistry.getCard(it) } ?: continue
            val statics = RoomFaceStatics.activeStaticAbilities(entity, definition)
            if (statics.none(::mayCap)) continue
            val controller = projected.getController(source) ?: continue
            val text = TextChanges.of(state, source)
            for (ability in statics) {
                collect(if (text == null) ability else ability.applyTextReplacement(text), source, controller)
            }
        }
        for (grant in state.grantedStaticAbilities) {
            if (!mayCap(grant.ability)) continue
            val source = grant.entityId
            val player = source in state.turnOrder
            if (!player && (source !in battlefield || projected.hasLostAllAbilities(source))) continue
            if (!GrantDurationGate.holds(state, source, grant.sourceId, grant.duration)) continue
            val controller = if (player) source else projected.getController(source) ?: continue
            collect(grant.ability, source, controller)
        }
        return cap
    }

    /** Whether [ability] is, or wraps, a [RestrictDrawsPerTurn] — the pre-filter for [capFor]. */
    private fun mayCap(ability: StaticAbility): Boolean = when (ability) {
        is RestrictDrawsPerTurn -> true
        is ConditionalStaticAbility -> mayCap(ability.ability)
        is CompositeStaticAbility -> ability.abilities.any(::mayCap)
        else -> false
    }

    /** Whether a cap whose [affected] group is read relative to [controller] binds [drawer]. */
    private fun binds(state: GameState, affected: Player, controller: EntityId, drawer: EntityId): Boolean =
        when (affected) {
            Player.You -> drawer == controller
            Player.EachOpponent, Player.AnOpponent -> state.isOpponentOf(drawer, controller)
            Player.Each, Player.Any -> true
            // Target-bound references have no meaning for a continuous static.
            else -> false
        }
}
