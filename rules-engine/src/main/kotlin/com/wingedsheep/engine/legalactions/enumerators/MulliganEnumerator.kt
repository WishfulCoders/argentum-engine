package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.BottomCards
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * The opening-hand decisions (CR 103.5) — the only actions a player has before the first turn.
 *
 * A player who hasn't kept may keep or mulligan. One who kept after mulliganing then puts that many
 * cards on the bottom: every choice is offered as a complete [BottomCards], one per distinct set of
 * cards, so cards with the same definition (two Forests) aren't offered twice. An opening hand is
 * small, so the choices are few — 35 at most for a seven-card hand.
 */
class MulliganEnumerator : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>() ?: return emptyList()

        if (!mullState.hasKept) {
            val bottom = mullState.cardsToBottom
            return listOfNotNull(
                LegalAction(
                    action = KeepHand(playerId), actionType = "KeepHand",
                    description = if (bottom == 0) "Keep hand" else "Keep hand (bottom $bottom)",
                ),
                LegalAction(action = TakeMulligan(playerId), actionType = "TakeMulligan", description = "Mulligan")
                    .takeIf { mullState.canMulligan },
            )
        }

        val count = mullState.cardsToBottom
        if (count == 0) return emptyList()
        val hand = state.getHand(playerId)
        val name = { id: EntityId -> state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown" }
        val groups = hand.groupBy { state.getEntity(it)?.get<CardComponent>()?.cardDefinitionId ?: it.value }.values.toList()
        return bottomChoices(groups, count).map { cardIds ->
            LegalAction(
                action = BottomCards(playerId, cardIds), actionType = "BottomCards",
                description = "Put ${cardIds.joinToString(", ") { name(it) }} on the bottom",
                // Not targets — just which cards this choice bottoms, for a host that reads ids.
                validTargets = cardIds,
            )
        }
    }

    /** Every way to take [count] cards from [groups] of interchangeable cards, taking each group's first ones. */
    private fun bottomChoices(groups: List<List<EntityId>>, count: Int, from: Int = 0): List<List<EntityId>> {
        if (count == 0) return listOf(emptyList())
        if (from == groups.size) return emptyList()
        val group = groups[from]
        return (minOf(count, group.size) downTo 0).flatMap { taken ->
            bottomChoices(groups, count - taken, from + 1).map { group.take(taken) + it }
        }
    }
}
