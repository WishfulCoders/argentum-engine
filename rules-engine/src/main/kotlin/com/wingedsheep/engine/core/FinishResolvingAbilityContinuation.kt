package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ObjectRef
import kotlinx.serialization.Serializable

/**
 * Completes a triggered or activated ability only after all of its resolving effects and choices
 * have finished (CR 608.2n: as the final part of its resolution, the ability is removed from the
 * stack and ceases to exist).
 *
 * Pre-pushed beneath the ability's effect. When the effect finishes without pausing, the resolver
 * finishes the ability inline and drops this frame; when it pauses, this frame runs once every
 * frame above it has. The resolved events are captured as the resolution starts, so both paths
 * report exactly the same thing.
 */
@Serializable
data class FinishResolvingAbilityContinuation(
    val abilityObject: ObjectRef,
    val abilityResolvedEvent: AbilityResolvedEvent,
    /** Present only for a Saga chapter ability. */
    val sagaChapterResolvedEvent: SagaChapterResolvedEvent? = null,
) : AutomaticContinuation {
    /** The events the ability reports as it finishes, after its effect's own. */
    val resolvedEvents: List<GameEvent>
        get() = listOf(abilityResolvedEvent) + listOfNotNull(sagaChapterResolvedEvent)
}
