@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * What a finished step stored for the steps after it: every pipeline kind, the permanents it
 * sacrificed or moved off the battlefield, and the object moves it authorised. One shape, merged by
 * one function ([mergeOutputs]), whether the outputs arrive as a synchronous [EffectResult], are
 * handed by a resumer to the frame beneath a paused step, or are an open composite's entry delta in
 * a program's cursor.
 */
@Serializable
data class PipelineOutputs(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val collections: Map<String, List<EntityId>> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val subtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val numbers: Map<String, Int> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val chosenValues: Map<String, String> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val stringLists: Map<String, List<String>> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val sacrificed: List<EntitySnapshot> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val departed: List<EntitySnapshot> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val permits: List<PermittedObjectMove> = emptyList(),
) {
    fun isEmpty(): Boolean = collections.isEmpty() && subtypeGroups.isEmpty() && numbers.isEmpty() &&
        chosenValues.isEmpty() && stringLists.isEmpty() && sacrificed.isEmpty() && departed.isEmpty() &&
        permits.isEmpty()

    companion object {
        /**
         * What [context] holds of every kind these outputs carry, whole: the base an open root
         * composite's entry is rebuilt from.
         */
        fun of(context: EffectContext): PipelineOutputs = PipelineOutputs(
            collections = context.pipeline.storedCollections,
            subtypeGroups = context.pipeline.storedSubtypeGroups,
            numbers = context.pipeline.storedNumbers,
            chosenValues = context.pipeline.chosenValues,
            stringLists = context.pipeline.storedStringLists,
            sacrificed = context.sacrificedPermanents,
            departed = context.departedPermanents,
            permits = context.objectReferences.permittedMoves,
        )

        /**
         * What [context] stored on top of [base]: each named value that is new or different, and the
         * snapshots and permits appended after [base]'s. Exact for a context that only ever grew
         * from [base] by merges, which is how every context inside a program grows.
         */
        fun between(base: EffectContext, context: EffectContext): PipelineOutputs = PipelineOutputs(
            collections = changed(base.pipeline.storedCollections, context.pipeline.storedCollections),
            subtypeGroups = changed(base.pipeline.storedSubtypeGroups, context.pipeline.storedSubtypeGroups),
            numbers = changed(base.pipeline.storedNumbers, context.pipeline.storedNumbers),
            chosenValues = changed(base.pipeline.chosenValues, context.pipeline.chosenValues),
            stringLists = changed(base.pipeline.storedStringLists, context.pipeline.storedStringLists),
            sacrificed = context.sacrificedPermanents.drop(base.sacrificedPermanents.size),
            departed = context.departedPermanents.drop(base.departedPermanents.size),
            permits = context.objectReferences.permittedMoves.drop(base.objectReferences.permittedMoves.size),
        )

        private fun <V> changed(base: Map<String, V>, now: Map<String, V>): Map<String, V> =
            if (base === now) emptyMap() else now.filter { (key, value) -> !base.containsKey(key) || base[key] != value }
    }
}

/** The pipeline outputs a result carries. */
val EffectResult.pipelineOutputs: PipelineOutputs
    get() = PipelineOutputs(
        collections = updatedCollections,
        subtypeGroups = updatedSubtypeGroups,
        numbers = updatedStoredNumbers,
        chosenValues = updatedChosenValues,
        stringLists = updatedStringLists,
        sacrificed = updatedSacrificedPermanents,
        departed = updatedDepartedPermanents,
        permits = updatedPermittedMoves,
    )

/**
 * What a caller folding this result into [base] would merge, as outputs a hand-off can carry: the
 * result's own outputs plus the object moves its events authorise on top of [base] — the part of
 * `foldResult` that comes from events rather than outputs. A resumer forwarding a finished step to
 * the frame beneath uses it, so a paused hand-off agrees with the synchronous fold even when no
 * resolution key would propagate those permits on its own.
 */
fun EffectResult.outputsFoldedInto(base: EffectContext): PipelineOutputs {
    val outputs = pipelineOutputs
    val authorised = base.objectReferences.authorize(events).permittedMoves
        .filterNot { it in base.objectReferences.permittedMoves || it in outputs.permits }
    return if (authorised.isEmpty()) outputs else outputs.copy(permits = outputs.permits + authorised)
}

/**
 * Merge [outputs] into this context: each named pipeline value replaces any earlier one of that
 * name, snapshots are appended, and permitted object moves this context doesn't hold yet are added.
 * [dedupSnapshots] appends only snapshots not already held — the hand-off case, where a resumer
 * forwards its frame's whole list and the frame beneath may already hold some of it (a cost-paid
 * sacrifice); a duplicate would inflate any count read off the list.
 */
fun EffectContext.mergeOutputs(outputs: PipelineOutputs, dedupSnapshots: Boolean): EffectContext {
    if (outputs.isEmpty()) return this
    fun List<EntitySnapshot>.plusSnapshots(more: List<EntitySnapshot>) =
        if (dedupSnapshots) this + more.filterNot { it in this } else this + more
    val newPermits = outputs.permits.filterNot { it in objectReferences.permittedMoves }
    return copy(
        sacrificedPermanents = if (outputs.sacrificed.isEmpty()) sacrificedPermanents
            else sacrificedPermanents.plusSnapshots(outputs.sacrificed),
        departedPermanents = if (outputs.departed.isEmpty()) departedPermanents
            else departedPermanents.plusSnapshots(outputs.departed),
        objectReferences = if (newPermits.isEmpty()) objectReferences
            else objectReferences.copy(permittedMoves = objectReferences.permittedMoves + newPermits),
        pipeline = pipeline.copy(
            storedCollections = pipeline.storedCollections + outputs.collections,
            storedSubtypeGroups = pipeline.storedSubtypeGroups + outputs.subtypeGroups,
            storedNumbers = pipeline.storedNumbers + outputs.numbers,
            chosenValues = pipeline.chosenValues + outputs.chosenValues,
            storedStringLists = pipeline.storedStringLists + outputs.stringLists,
        ),
    )
}
