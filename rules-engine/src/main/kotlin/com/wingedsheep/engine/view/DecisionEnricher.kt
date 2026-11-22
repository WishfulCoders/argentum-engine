package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * What a seat is told about the pending decision while another seat is deciding it. The game
 * server sends it as `OpponentDecisionStatus`; it lives here so that tests checking what a seat
 * learns read the same projection the server sends.
 */
data class OpponentDecisionStatusView(
    /** The seat that is actually deciding. */
    val playerId: EntityId,
    val decisionType: String,
    val displayText: String,
    val sourceName: String?,
    /** The spell/ability source entity, so the client can show its card from its own
     *  (already viewer-masked) game state. */
    val sourceId: EntityId?,
    /** The source's art, for when the viewer's game state has no entry for it — a spell
     *  being cast from its controller's hand is announced (its name is already shown) but
     *  isn't in the viewer's masked state yet. Null when the viewer may not see the source. */
    val sourceImageUri: String?,
)

/** Projects a pending decision for one seat: the masked decision its decider is sent, or the status every other seat is sent. */
class DecisionEnricher(private val cardRegistry: CardRegistry) {
    private val visibility = Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)

    /** Hides every face-down identity the viewer may not see; the one per-viewer masking point. */
    private val masker = DecisionMasker(visibility)

    /**
     * The art to display for [entityId], or null when [viewerId] may not see what it is.
     *
     * Reads the entity's own [CardComponent.imageUri], which `CardEntityFactory` stamps from the
     * printing the player actually put in their deck. Re-deriving it from the canonical
     * [com.wingedsheep.sdk.model.CardDefinition] metadata instead (as this used to) shows the
     * *original* printing's art for every reprint, so a card in a search/reveal prompt didn't match
     * the same card in hand or on the battlefield — both of which read the component. The definition
     * lookup remains only as a fallback for entities with no image stamped.
     */
    private fun imageUriFor(state: GameState, entityId: EntityId, viewerId: EntityId): String? {
        if (visibility.isCardIdentityHiddenFrom(state, entityId, viewerId)) return null
        val cardComponent = state.getEntity(entityId)?.get<CardComponent>() ?: return null
        return cardComponent.imageUri
            ?: cardRegistry.getCard(cardComponent.cardDefinitionId)?.metadata?.imageUri
    }

    private fun Map<EntityId, SearchCardInfo>.withImages(state: GameState, viewerId: EntityId) =
        mapValues { (entityId, cardInfo) -> cardInfo.copy(imageUri = imageUriFor(state, entityId, viewerId)) }

    fun enrich(decision: PendingDecision, state: GameState, viewerId: EntityId): PendingDecision {
        return when (val masked = masker.maskFor(decision, state, viewerId)) {
            is SearchLibraryDecision -> masked.copy(cards = masked.cards.withImages(state, viewerId))
            is ReorderLibraryDecision -> masked.copy(cardInfo = masked.cardInfo.withImages(state, viewerId))
            is SelectCardsDecision -> masked.copy(cardInfo = masked.cardInfo?.withImages(state, viewerId))
            is OrderObjectsDecision -> masked.copy(cardInfo = masked.cardInfo?.withImages(state, viewerId))
            // A source that has left the game keeps the art it was asked with.
            is OrderTriggersDecision -> masked.copy(triggers = masked.triggers.map {
                it.copy(imageUri = imageUriFor(state, it.sourceId, viewerId) ?: it.imageUri)
            })
            is SplitPilesDecision -> masked.copy(cardInfo = masked.cardInfo?.withImages(state, viewerId))
            // Other decision types don't have card info to enrich
            else -> masked
        }
    }

    fun opponentDecisionStatus(
        decision: PendingDecision,
        state: GameState,
        viewerId: EntityId,
    ): OpponentDecisionStatusView {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is PlayCardDecision -> "Playing a card"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is BatchYesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> decision.orderingTitle ?: "Ordering blockers"
            is OrderTriggersDecision -> "Ordering triggers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is CombatResolutionDecision -> "Assigning combat damage"
            is ChooseOptionDecision -> "Making a choice"
            is ChooseReplacementDecision -> "Changing text"
            is BudgetModalDecision -> "Choosing modes"
            is FlipOntoBattlefieldDecision -> "Taking aim"
            is SelectManaSourcesDecision -> "Selecting mana sources"
        }
        val sourceId = decision.context.sourceId
        // A card in a hidden hand is unknown to this viewer, and its id alone could name it.
        val privateSourceHidden = decision.context.sourceIsPrivate && masker.isSourceHiddenFrom(decision, state, viewerId)
        return OpponentDecisionStatusView(
            playerId = decision.playerId,
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = masker.maskFor(decision, state, viewerId).context.sourceName,
            sourceId = sourceId.takeUnless { privateSourceHidden },
            sourceImageUri = sourceId?.takeUnless { privateSourceHidden }?.let { imageUriFor(state, it, viewerId) }
        )
    }
}
