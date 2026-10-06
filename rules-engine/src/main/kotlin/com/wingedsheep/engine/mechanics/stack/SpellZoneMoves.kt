package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.permanent.types.restoreDfcFrontFace
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Zone-move tails shared by a spell's resolution, fizzle and counter paths: shuffling a card into
 * its owner's library, and making a card that was just exiled from the stack *plotted*.
 */
internal object SpellZoneMoves {
    /**
     * Shuffle [ownerId]'s library after an Omen spell has been added to it on resolution
     * (Tarkir: Dragonstorm — "then shuffle this card into its owner's library"). Mirrors
     * [com.wingedsheep.engine.handlers.effects.library.ShuffleLibraryExecutor]: clears any
     * known top-of-library positions before shuffling, then advances the deterministic RNG.
     * The caller is responsible for emitting the [LibraryShuffledEvent].
     */
    fun shuffleOwnerLibrary(state: GameState, ownerId: EntityId): GameState {
        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val cleared = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .clearLibraryReveals(state, ownerId)
        val (library, advanced) = cleared.nextRandom { shuffle(cleared.getZone(libraryZone)) }
        return advanced.copy(zones = advanced.zones + (libraryZone to library))
    }

    /**
     * Put a spell card that leaves the stack without resolving into a permanent — it fizzled
     * (CR 608.2b), or its entry was replaced by "put it into its owner's graveyard" (Mox Diamond's
     * unpaid [com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid], CR 614.6) — into its owner's
     * graveyard, honouring what a card leaving the stack for a graveyard is subject to: flashback /
     * harmonize exile, a not-only-if-resolved after-resolve destination, and graveyard
     * redirects ("from anywhere" replacements). Returns the new state and the [ZoneChangeEvent].
     */
    fun putSpellCardIntoGraveyard(
        state: GameState,
        spellId: EntityId,
        cardComponent: com.wingedsheep.engine.state.components.identity.CardComponent?,
        spellComponent: SpellOnStackComponent,
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
        predicateEvaluator: com.wingedsheep.engine.handlers.PredicateEvaluator,
        zones: com.wingedsheep.engine.handlers.effects.ZoneTransitionService,
    ): Pair<GameState, List<GameEvent>> {
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted —
        // Songcrafter Mage): a graveyard cast exiles on resolution instead of returning to the
        // graveyard. A spell cast *with* flashback is exiled even if a conditional flashback's
        // condition has since lapsed (Viral Spawning), so the recorded alternative cost counts too.
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            (spellComponent.alternativeCost == AlternativeCostType.FLASHBACK ||
                FlashbackGrants.effectiveFlashback(
                state, spellId, cardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
            ) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = state.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
        // Goliath Daydreamer-style components only redirect on actual resolution; if the spell
        // fizzles or is countered they go to graveyard normally.
        val riderOnFizzle = exileAfterResolveComp?.takeIf { !it.onlyIfResolved }
        // A fizzled spell heading to its owner's graveyard is a card put into a graveyard
        // "from anywhere" — honor RedirectZoneChange replacements (Valgavoth, Leyline).
        val fizzleRedirect = if (flashbackExile || riderOnFizzle != null) {
            com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult(
                riderOnFizzle?.zone ?: Zone.EXILE
            )
        } else {
            com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .checkZoneChangeRedirect(state, spellId, Zone.STACK, Zone.GRAVEYARD, predicateEvaluator = predicateEvaluator)
        }
        val destZone = fizzleRedirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destZone)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TextReplacementComponent>()
                .without<TargetsComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)
        // CR 712.8a — a fizzled card cast transformed is front face up again once off the stack.
        newState = restoreDfcFrontFace(newState, cardRegistry, spellId)
        newState = com.wingedsheep.engine.mechanics.PrototypeCasts.end(newState, spellId)
        val destinationObject = newState.objectRef(spellId)
        // A card-intrinsic redirect into the library shuffles the card in (Progenitus).
        if (destZone == Zone.LIBRARY && fizzleRedirect.shuffleIntoLibrary) {
            newState = SpellZoneMoves.shuffleOwnerLibrary(newState, ownerId)
        }
        if (destZone == Zone.EXILE && fizzleRedirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, fizzleRedirect.linkSourceId)
        }
        // The redirect's rider (Dauthi Voidwalker's void counter) applies to this move too.
        val riderEvents = fizzleRedirect.additionalEffect?.let { extra ->
            val (afterRider, events) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applyReplacementAdditionalEffect(
                    zones, newState, extra, fizzleRedirect.effectControllerId, spellId,
                    sourceId = fizzleRedirect.effectSourceId
                )
            newState = afterRider
            events
        }.orEmpty()

        return newState to listOf<GameEvent>(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                Zone.STACK,
                destZone,
                ownerId, oldObject = state.objectRef(spellId), newObject = destinationObject
            )
        ) + riderEvents
    }

    /**
     * Make a card that already sits in [ownerId]'s exile *plotted* (CR 718): tag it with
     * [PlottedComponent] + [PlayWithoutPayingCostComponent], grant a permanent may-play
     * permission gated on [SourcePlottedOnPriorTurn] (a plotted card can't be cast the turn it
     * was plotted), and emit [CardPlottedEvent]. Shared by [ExileTargetSpellEffect]'s
     * `makePlotted` path and the [AfterResolveDestinationComponent].`makePlotted` self-cast path
     * (Lilah, Undefeated Slickshot).
     */
    fun applyPlottedToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        cardName: String,
        events: MutableList<GameEvent>,
    ): GameState {
        val turnPlotted = state.turnNumber
        var newState = state.updateEntity(cardId) { c ->
            c.with(PlottedComponent(controllerId = ownerId, turnPlotted = turnPlotted))
                .with(PlayWithoutPayingCostComponent(controllerId = ownerId, permanent = true))
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                sourceId = cardId,
                condition = SourcePlottedOnPriorTurn,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
        events.add(CardPlottedEvent(ownerId, cardId, cardName))
        return newState
    }
}
