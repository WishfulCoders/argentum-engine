package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifyUnlockCost
import com.wingedsheep.sdk.scripting.UnlockCostTarget

/**
 * Computes the effective cost of the **door-unlock** special action (CR 709.5e) after applying
 * battlefield [ModifyUnlockCost] static abilities.
 *
 * Unlocking is neither a spell nor the Plot action, so neither [CostCalculator] (which works on
 * `ModifySpellCost`) nor [PlotCostReducer] touches it; this is unlocking's dedicated, parallel
 * cost-reduction path. Both
 * [com.wingedsheep.engine.legalactions.enumerators.UnlockRoomDoorEnumerator] (affordability) and
 * [com.wingedsheep.engine.handlers.actions.room.UnlockRoomDoorHandler] (validate + pay) route the
 * unlock cost through here so the two stay in lockstep.
 *
 * Only [UnlockCostTarget.YouUnlock] is matched today (Inquisitive Glimmer: "Unlock costs you pay
 * cost {1} less"); a future "opponents' unlock costs" tax adds an [UnlockCostTarget] variant
 * without changing call sites. Only generic [CostModification] reductions/increases are meaningful
 * — the printed unlock cost is a flat mana cost.
 */
class UnlockCostReducer(
    private val cardRegistry: CardRegistry,
) {
    /**
     * The unlock cost [unlockerId] actually pays, after reductions. Floored at {0} generic;
     * colored pips are never reduced below the printed requirement.
     */
    fun effectiveUnlockCost(state: GameState, unlockerId: EntityId, baseCost: ManaCost): ManaCost {
        var totalReduction = 0
        var totalIncrease = 0
        for ((sourceId, ability) in scanBattlefield(state)) {
            if (ability.target != UnlockCostTarget.YouUnlock) continue
            if (state.projectedState.getController(sourceId) != unlockerId) continue
            when (val mod = ability.modification) {
                is CostModification.ReduceGeneric -> totalReduction += mod.amount
                is CostModification.IncreaseGeneric -> totalIncrease += mod.amount
                else -> { /* unlocking only supports flat generic adjustments */ }
            }
        }
        var cost = baseCost
        if (totalIncrease > 0) cost = increaseGeneric(cost, totalIncrease)
        if (totalReduction > 0) cost = cost.reduceGeneric(totalReduction)
        return cost
    }

    private fun increaseGeneric(cost: ManaCost, increase: Int): ManaCost {
        if (increase <= 0) return cost
        val colored = cost.symbols.filter { it !is ManaSymbol.Generic }
        val newGeneric = cost.genericAmount + increase
        val symbols = if (newGeneric > 0) {
            listOf(ManaSymbol.Generic(newGeneric)) + colored
        } else colored
        return ManaCost(symbols)
    }

    private fun scanBattlefield(state: GameState): List<Pair<EntityId, ModifyUnlockCost>> {
        val results = mutableListOf<Pair<EntityId, ModifyUnlockCost>>()
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel
                for (ability in def.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is ModifyUnlockCost) results += entityId to ability
                }
            }
        }
        return results
    }
}
