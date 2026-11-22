package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.mergeOutputs
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The effect interpreter's frame: an effect tree, a position in it that remembers how to finish
 * every node still open, and the environment the rest of the tree runs in.
 *
 * [com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter] walks
 * [CompositeEffect] sequences itself, so a nested composite inside [root] is a position in [root],
 * not a frame of its own. Every other node is a *leaf* that runs through the executor registry.
 * Before running each leaf, the interpreter pushes this frame beneath it; when the leaf pauses for
 * a decision, the frame waits beneath the leaf's own frames, and the automatic resumer continues
 * the program — through the same scope exits and the same output publication as a run that never
 * paused — once they have drained.
 *
 * **Reading the position (the observation contract).** With origin
 * [ProgramOrigin.Resolving], [root] is the resolving stack object's own effect and the cursor is
 * an absolute path into it. With [ProgramOrigin.BuiltBy], [root] is something the named step
 * built or ran (a gate's branch, a loop's body, effects an executor queued itself), and the
 * cursor is a local path into that root. With a null origin the frame was converted from the
 * retired `EffectContinuation` on load; its root is the old frame's remaining effects. Every live
 * program has a non-null origin.
 *
 * @property root A copy of the effect tree being run. Always a copy, carried in the frame, so
 *   resuming needs only the frame — no stack object or definition has to be reachable.
 * @property origin Where [root] came from (see [ProgramOrigin]). A label: resuming reads only
 *   whether it is null, which marks a frame converted from the retired `EffectContinuation` on
 *   load; such a frame publishes its whole saved context when it finishes, as the old frame did.
 * @property cursor The node of [root] **currently executing** — not the next one — as a zipper
 *   (see [ProgramCursor]). In a frame pushed beneath a leaf that paused, the cursor names that
 *   leaf; when the frame resumes, the leaf has finished: its outputs are already in
 *   [effectContext] (resumers hand them to "the frame beneath"), and the interpreter advances
 *   past it.
 * @property effectContext The environment the node at the cursor runs in: pipeline storage,
 *   targets, snapshots, object references.
 */
@Serializable
data class EffectProgram(
    val root: Effect,
    val origin: ProgramOrigin?,
    val cursor: ProgramCursor,
    val effectContext: EffectContext,
) : AutomaticContinuation {

    /**
     * This frame with the named pipeline values in [outputs] (collections, subtype groups,
     * numbers, chosen values, string lists — nothing else; snapshots and permits must be empty)
     * merged into its current context and into the entry of every open composite inside the root,
     * as a value handed to "every enclosing composite" used to go into each per-composite frame
     * before programs existed. The root's own entry — the program's base — is left alone, so the
     * values still count as the program's outputs and reach the frame beneath when it finishes,
     * as the old outermost frame published them.
     */
    fun withOutputsEverywhere(outputs: com.wingedsheep.engine.handlers.PipelineOutputs): EffectProgram {
        require(outputs.sacrificed.isEmpty() && outputs.departed.isEmpty() && outputs.permits.isEmpty()) {
            "withOutputsEverywhere merges named pipeline values only"
        }
        fun com.wingedsheep.engine.handlers.PipelineOutputs.plus(more: com.wingedsheep.engine.handlers.PipelineOutputs) = copy(
            collections = collections + more.collections, subtypeGroups = subtypeGroups + more.subtypeGroups,
            numbers = numbers + more.numbers, chosenValues = chosenValues + more.chosenValues,
            stringLists = stringLists + more.stringLists,
        )
        return copy(
            cursor = ProgramCursor(cursor.steps.mapIndexed { depth, step ->
                when (step) {
                    is CursorStep.Child ->
                        if (depth == 0) step else step.copy(delta = step.delta.copy(outputs = step.delta.outputs.plus(outputs)))
                    CursorStep.Start -> step
                }
            }),
            effectContext = effectContext.mergeOutputs(outputs, dedupSnapshots = true),
        )
    }

    companion object {
        /**
         * A program queued to run later, not started yet: [effects] as a [CompositeEffect] root
         * (always, even for one effect), with the cursor at [CursorStep.Start]. Its context
         * carries [origin] as the program's own.
         */
        fun queued(effects: List<Effect>, effectContext: EffectContext, origin: ProgramOrigin?): EffectProgram {
            val label = origin ?: ProgramOrigin.UNATTRIBUTED
            return EffectProgram(CompositeEffect(effects), label, ProgramCursor.START, effectContext.copy(programOrigin = label))
        }
    }
}

/**
 * A position in [EffectProgram.root] as a zipper: one step per open node, outermost first. Each
 * step says where in its node execution is and holds what that node needs to finish exactly the
 * way a run that never paused would.
 *
 * - `[Start]`: the program hasn't started; resuming runs the root from the top.
 * - `[Child(i₀, d₀), Child(i₁, d₁), …]`: the root composite is at its child `i₀`, which is a
 *   composite at its child `i₁`, and so on. The node at the last step's index is the one currently
 *   executing. Each step's [EntryDelta] rebuilds the context its composite was entered with: the
 *   root's from the frame's current context with `d₀`'s values in place of every pipeline kind,
 *   snapshot list, permit list and depth; each deeper one from its parent's entry merged with its
 *   delta.
 *
 * [CursorStep] is sealed so later node kinds (gate stages, loop iterations) add steps of their own.
 */
@Serializable
data class ProgramCursor(val steps: List<CursorStep>) {
    companion object {
        /** The program hasn't started. */
        val START = ProgramCursor(listOf(CursorStep.Start))
    }
}

/** One step of a [ProgramCursor]. */
@Serializable
sealed interface CursorStep {
    /** Nothing in the program has run yet. Only ever the cursor's single step. */
    @Serializable
    data object Start : CursorStep

    /**
     * An open [CompositeEffect] at child [index]. [delta] says how the context it was entered with
     * differs from its parent's entry (for the root: what that context held, whole). Leaving the
     * composite hands its parent that entry plus what the composite stored, on every path.
     */
    @Serializable
    data class Child(val index: Int, val delta: EntryDelta) : CursorStep
}

/**
 * How an open composite's entry context differs from its parent's: the values the siblings before
 * it stored (new or changed), the snapshots and object-move permits they appended, and the change
 * in resolution depth. Exact, because inside a program a context only ever grows by merges. Every
 * other field of an entry is the frame's current context's: nothing in a program changes those
 * between composites. (The three reference-lost flags are re-derived from the game state before
 * every instruction, so an entry carries the current ones.)
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class EntryDelta(
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val outputs: com.wingedsheep.engine.handlers.PipelineOutputs = com.wingedsheep.engine.handlers.PipelineOutputs(),
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val depth: Int = 0,
)

/** Where an [EffectProgram]'s root came from. Identifies the root; never consulted to resume it. */
@Serializable
sealed interface ProgramOrigin {
    /** The root is the effect of this resolving stack object; the cursor is a path into it. */
    @Serializable
    data class Resolving(val stackObject: ObjectRef) : ProgramOrigin

    /**
     * The root was started by another step: a leaf running its own children (a gate's branch, a
     * loop's body), an executor queueing effects it built itself, or an engine path that runs a
     * card's tree outside a stack resolution (a mana ability, a permanent's entry, a special
     * action). [step] is a stable name for that step — the SDK effect's serial name when there is
     * one, otherwise the class's simple name — and [parent] is the enclosing program's origin, or
     * null when there is none. The cursor is a local path into the root.
     */
    @Serializable
    data class BuiltBy(val step: String, val parent: ProgramOrigin?) : ProgramOrigin

    companion object {
        /** The label of a program whose starting path set none (a test, an unlabelled engine path). */
        val UNATTRIBUTED: ProgramOrigin = BuiltBy("Unattributed", null)
    }
}
