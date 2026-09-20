package com.wingedsheep.gym.contract

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment

/** The action mask shared by the arena BC bridge and learner-seat gym observations. */
object PolicyActionBoundary {
    fun requirePolicyPayment(action: LegalAction, params: ActionParams) {
        val options = action.policyBlightTargetOptions
        require(params.blightTarget == null || params.blightTarget in options) {
            "blightTarget is not an engine-checked option for this action"
        }
        require(options.isEmpty() || params.blightTarget != null) {
            "Blight activation requires blightTarget"
        }
    }

    fun callable(action: LegalAction): Boolean =
        action.actionType !in setOf("CrewVehicle", "SaddleMount") &&
            additionalCostCallable(action) &&
            convokeCallable(action) &&
            !action.hasDelve &&
            !action.hasTapForGeneric &&
            !action.hasHarmonize &&
            !action.requiresManaColorChoice &&
            action.manaCostPerExtraTarget == null

    fun mask(actions: List<LegalAction>): List<LegalAction> = actions.map { action ->
        if (callable(action)) action else action.copy(affordable = false)
    }

    /** Share the same bounded, engine-checked convoke choices with arena and learner gym. */
    fun mask(actions: List<LegalAction>, state: GameState, simulator: GameSimulator): List<LegalAction> =
        mask(actions.map { action ->
            var prepared = action
            if (action.hasConvoke && !action.canPayWithoutConvoke) {
                prepared = prepared.copy(policyConvokePaymentOptions = convokeOptions(action, state, simulator))
            }
            if (action.additionalCostInfo?.costType == "Blight") {
                prepared = prepared.copy(policyBlightTargetOptions = blightOptions(action, state, simulator))
            }
            prepared
        })

    /** Only a simple, untargeted activation can be preflighted as a complete move here. */
    private fun blightOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): List<EntityId> {
        val activation = action.action as? ActivateAbility ?: return emptyList()
        val info = action.additionalCostInfo ?: return emptyList()
        if (!action.affordable || action.hasXCost || action.requiresTargets ||
            action.targetRequirements.orEmpty().isNotEmpty() ||
            action.hasConvoke || action.hasDelve || action.hasTapForGeneric || action.hasHarmonize ||
            action.requiresManaColorChoice || info.costType != "Blight" || info.blightAmount <= 0 ||
            info.validBlightTargets.size > MAX_BLIGHT_CANDIDATES
        ) return emptyList()
        return info.validBlightTargets.sortedBy { it.value }.filter { target ->
            simulator.accepts(state, activation.copy(costPayment =
                (activation.costPayment ?: AdditionalCostPayment()).copy(blightTargets = listOf(target))))
        }
    }

    private fun convokeCallable(action: LegalAction): Boolean = !action.hasConvoke ||
        (action.actionType == "CastSpell" && !action.hasXCost &&
            (action.canPayWithoutConvoke || action.policyConvokePaymentOptions.isNotEmpty()))

    /**
     * Search only ordinary, fixed-cost, untargeted casts. Each candidate is checked by the real
     * action processor, so a mana creature cannot both convoke and tap for mana, and unusual
     * payment restrictions cannot leak an unpayable action into the policy mask. The limits are
     * intentional: beyond them the cast stays masked until it has a richer payment head.
     */
    private fun convokeOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): List<Map<EntityId, ConvokePayment>> {
        val cast = action.action as? CastSpell ?: return emptyList()
        if (!action.affordable || action.actionType != "CastSpell" || action.hasXCost ||
            action.additionalCostInfo != null || action.hasDelve || action.hasTapForGeneric ||
            action.hasHarmonize || action.manaCostPerExtraTarget != null ||
            (action.requiresTargets && action.minTargets > 0) ||
            action.targetRequirements.orEmpty().any { it.minTargets > 0 }
        ) return emptyList()
        val cost = action.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() }
            ?: return emptyList()
        val creatures = action.convokeCreatures.orEmpty()
            .sortedWith(compareByDescending<com.wingedsheep.engine.legalactions.ConvokeCreatureData> {
                it.colors.count { color -> cost.symbols.any { symbol -> paysColored(symbol, color) } }
            }.thenBy { it.entityId.value })
            .take(MAX_CREATURES)
        if (creatures.isEmpty()) return emptyList()

        data class Candidate(
            val next: Int,
            val remaining: ManaCost,
            val payments: Map<EntityId, ConvokePayment>,
        )
        val queue = ArrayDeque<Candidate>()
        queue.add(Candidate(0, cost, emptyMap()))
        val options = mutableListOf<Map<EntityId, ConvokePayment>>()
        var checked = 0
        while (queue.isNotEmpty() && checked < MAX_PREFLIGHTS && options.size < MAX_OPTIONS) {
            val current = queue.removeFirst()
            for (index in current.next until creatures.size) {
                val creature = creatures[index]
                val choices = buildList<Color?> {
                    creature.colors.sortedBy { it.ordinal }.forEach { color ->
                        if (current.remaining.symbols.any { paysColored(it, color) }) add(color)
                    }
                    if (current.remaining.genericAmount > 0) add(null)
                }
                for (color in choices) {
                    val reduced = if (color == null) current.remaining.reduceGeneric(1)
                        else reduceColored(current.remaining, color)
                    if (reduced == current.remaining) continue
                    val payment = current.payments + (creature.entityId to ConvokePayment(color))
                    checked++
                    val completed = cast.copy(alternativePayment =
                        (cast.alternativePayment ?: AlternativePaymentChoice.NONE).copy(
                            convokedCreatures = payment))
                    if (simulator.accepts(state, completed)) options.add(payment)
                    if (payment.size < MAX_CREATURES && checked < MAX_PREFLIGHTS) {
                        queue.add(Candidate(index + 1, reduced, payment))
                    }
                    if (checked >= MAX_PREFLIGHTS || options.size >= MAX_OPTIONS) break
                }
                if (checked >= MAX_PREFLIGHTS || options.size >= MAX_OPTIONS) break
            }
        }
        return options
    }

    private fun paysColored(symbol: ManaSymbol, color: Color): Boolean = when (symbol) {
        is ManaSymbol.Colored -> symbol.color == color
        is ManaSymbol.Hybrid -> symbol.color1 == color || symbol.color2 == color
        is ManaSymbol.MonocolorHybrid -> symbol.color == color
        else -> false
    }

    private fun reduceColored(cost: ManaCost, color: Color): ManaCost {
        val symbols = cost.symbols.toMutableList()
        val index = symbols.indexOfFirst { it == ManaSymbol.Colored(color) }
            .takeIf { it >= 0 } ?: symbols.indexOfFirst { paysColored(it, color) }
        if (index >= 0) symbols.removeAt(index)
        return ManaCost(symbols)
    }

    private const val MAX_CREATURES = 7
    private const val MAX_PREFLIGHTS = 128
    private const val MAX_OPTIONS = 4
    private const val MAX_BLIGHT_CANDIDATES = 8

    private fun additionalCostCallable(action: LegalAction): Boolean {
        val info = action.additionalCostInfo ?: return true
        // SacrificeSelf needs no parameter only when the source is its sole payment; Blight
        // needs an engine-checked recipient before the policy may submit it.
        val activation = action.action as? ActivateAbility ?: return false
        return when (info.costType) {
            "Blight" -> action.policyBlightTargetOptions.isNotEmpty()
            "SacrificeSelf" -> info.sacrificeCount == 1 &&
                info.validSacrificeTargets == listOf(activation.sourceId) &&
                info.counterRemovalCreatures.isEmpty()
            else -> false
        }
    }
}
