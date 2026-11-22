package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.CardScript

/**
 * The characteristics a spell is announced with when they differ from the printed card — bestow's
 * Aura (CR 702.103), prototype's cost and size (CR 718.3) and a mutating spell's target (CR 702.140a). One entry point for the cast rails.
 */
object CastCharacteristics {
    fun announce(state: GameState, action: CastSpell, registry: CardRegistry): GameState =
        SplitSpellCasts.announce(PrototypeCasts.announce(BestowCasts.announce(state, action, registry), action, registry), action, registry)

    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? =
        MutateCasts.definitionForCast(
            SplitSpellCasts.definitionForCast(PrototypeCasts.definitionForCast(BestowCasts.definitionForCast(definition, action), action), action),
            action)

    /**
     * The script a face cast is evaluated with: the announced face's — an Adventure (CR 715.3a), an
     * Omen (CR 720.3a), a split half (CR 709.3a) — when [CastSpell.faceIndex] picks one, the card's
     * own otherwise. Its additional costs are the ones the cast owes, so a face's "as an additional
     * cost to cast this spell" is paid and the primary's is not.
     */
    fun scriptForCast(definition: CardDefinition, action: CastSpell): CardScript =
        action.faceIndex?.let { definition.cardFaces.getOrNull(it)?.script } ?: definition.script

    /** Abandons an announcement whose cast never reached the stack. */
    fun end(state: GameState, id: EntityId): GameState =
        SplitSpellCasts.end(PrototypeCasts.end(BestowCasts.end(state, id), id), id)
}
