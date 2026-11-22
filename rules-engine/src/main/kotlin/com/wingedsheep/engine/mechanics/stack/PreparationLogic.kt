package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId

/**
 * Prepared (Secrets of Strixhaven) — shared "becoming prepared" logic (per CR 614/the official
 * prepare rulings).
 *
 * A creature with the [com.wingedsheep.sdk.model.CardLayout.PREPARE] layout *enters* prepared when
 * its creature face carries the [com.wingedsheep.sdk.core.Keyword.PREPARED] keyword
 * ([enterPreparedIfKeyworded]) — however it enters: cast and resolved (`PermanentEntry`), put onto
 * the battlefield from any zone (`ZoneTransitionService`), or created as a token copy
 * (`EntersWithReplacements.applyOnEntry`) (CR 722.3a). It can *become* prepared later via an effect such
 * as Leech Collector's "becomes prepared" trigger ([com.wingedsheep.engine.handlers.effects.permanent.BecomePreparedExecutor]).
 *
 * Either way the mechanics are identical: create a copy of the prepare spell (`cardFaces[0]`) in
 * the controller's exile, grant a permanent cast-from-exile permission for it, and link the two via
 * [PreparedComponent] / [PreparedSpellCopyComponent]. Casting that copy strips the
 * [PreparedComponent] (unprepares the creature); the copy ceases to exist on resolution
 * ([CopyOfComponent]). A creature that is already prepared does not re-prepare.
 */
object PreparationLogic {

    /**
     * "This creature enters prepared" (CR 722.3a): make [permanentId] prepared as it enters if
     * [cardDef] is a preparation card whose creature face has the
     * [com.wingedsheep.sdk.core.Keyword.PREPARED] keyword. PREPARE-layout creatures without the
     * keyword (Leech Collector, which only becomes prepared via a trigger) don't. Callers skip
     * face-down entries — a face-down permanent has no prepare spell.
     */
    fun enterPreparedIfKeyworded(
        state: GameState,
        permanentId: EntityId,
        cardDef: CardDefinition,
        controllerId: EntityId,
    ): GameState {
        if (cardDef.layout != CardLayout.PREPARE || Keyword.PREPARED !in cardDef.keywords) return state
        return makePrepared(state, permanentId, cardDef, controllerId)
    }

    /**
     * Make the permanent [permanentId] prepared. No-op if the permanent is missing, isn't a
     * PREPARE-layout card, or is already prepared (CR 614 — it can't become prepared twice; the
     * already-existing copy stands).
     */
    fun makePrepared(
        state: GameState,
        permanentId: EntityId,
        cardDef: CardDefinition,
        controllerId: EntityId,
    ): GameState {
        val sourceCard = state.getEntity(permanentId)?.get<CardComponent>() ?: return state
        // Already prepared: a second "becomes prepared" does nothing.
        if (state.getEntity(permanentId)?.get<PreparedComponent>() != null) return state
        val prepareFace = cardDef.cardFaces.firstOrNull() ?: return state

        var newState = state
        val (copyId, stateWithCopy) = newState.newEntity()
        newState = stateWithCopy
        newState = newState.updateEntity(copyId) { c ->
            c.with(
                CardComponent(
                    cardDefinitionId = sourceCard.cardDefinitionId,
                    name = sourceCard.name,
                    manaCost = prepareFace.manaCost,
                    typeLine = prepareFace.typeLine,
                    oracleText = prepareFace.oracleText,
                    colors = prepareFace.manaCost.colors,
                    ownerId = controllerId,
                    spellEffect = prepareFace.script.spellEffect,
                    imageUri = sourceCard.imageUri,
                )
            ).with(
                CopyOfComponent(
                    originalCardDefinitionId = sourceCard.cardDefinitionId,
                    copiedCardDefinitionId = sourceCard.cardDefinitionId,
                )
            ).with(
                PreparedSpellCopyComponent(sourceId = permanentId)
            )
        }
        newState = newState.addToZone(ZoneKey(controllerId, Zone.EXILE), copyId)

        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(copyId),
                controllerId = controllerId,
                sourceId = permanentId,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )

        newState = newState.updateEntity(permanentId) { c ->
            c.with(PreparedComponent(exileCopyId = copyId))
        }
        return newState
    }
}
