package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withCount

/**
 * Target legality belongs to the prospective copy, not the effect making it or the original.
 * The scratch entity is never put on the stack or returned to gameplay: new targets are chosen
 * before placement (CR 707.10c). In particular, the original keeps its own colors and remains a
 * possible target; a red copy can target something protected from the original's blue color.
 */
internal object SpellCopyTargets {

    data class Selection(val targets: List<ChosenTarget>, val retained: Set<Int>)

    fun originals(state: GameState, sourceId: EntityId, ordinal: Int? = null): List<ChosenTarget> =
        if (ordinal == null) state.getEntity(sourceId)?.get<TargetsComponent>()?.targets.orEmpty()
        else state.getEntity(sourceId)?.get<SpellOnStackComponent>()?.modeTargetsOrdered?.getOrNull(ordinal).orEmpty()

    fun selection(state: GameState, sourceId: EntityId, response: TargetsResponse, ordinal: Int? = null): Selection {
        val originals = originals(state, sourceId, ordinal)
        val retained = originals.indices.filterTo(mutableSetOf()) { response.selectedTargets[it].isNullOrEmpty() }
        return Selection(originals.mapIndexed { i, original ->
            response.selectedTargets[i]?.singleOrNull()?.let { entityIdToChosenTarget(state, it) } ?: original
        }, retained)
    }

    data class Prompt(val requirements: List<TargetRequirementInfo>, val legal: Map<Int, List<EntityId>>)

    fun prompt(state: GameState, finder: TargetFinder, sourceId: EntityId, controllerId: EntityId,
               requirements: List<TargetRequirement>, exceptions: CopyExceptions, ordinal: Int? = null): Prompt {
        val originals = originals(state, sourceId, ordinal)
        val byRequirement = legalTargets(state, finder, sourceId, controllerId, requirements, exceptions)
        val distribution = state.getEntity(sourceId)?.get<SpellOnStackComponent>()?.damageDistribution.orEmpty()
        val infos = mutableListOf<TargetRequirementInfo>()
        val legal = mutableMapOf<Int, List<EntityId>>()
        var slot = 0
        for ((index, req) in alignedRequirements(requirements, originals.size).withIndex()) {
            repeat(req.count) {
                val amount = distribution[originals[slot].entityId()]
                infos += TargetRequirementInfo(index = slot, minTargets = 0, maxTargets = 1,
                    description = "${req.description} — target ${slot + 1}" +
                        (amount?.let { ": $it to this target" } ?: ""), emptyChoiceLabel = "Keep original target")
                legal[slot] = byRequirement[index].orEmpty()
                slot++
            }
        }
        return Prompt(infos, legal)
    }

    fun validate(state: GameState, finder: TargetFinder, answer: AnswerContinuation, response: TargetsResponse): String? {
        val sourceId: EntityId
        val controller: EntityId
        val requirements: List<TargetRequirement>
        val exceptions: CopyExceptions
        val ordinal: Int?
        when (answer) {
            is StormCopyTargetContinuation -> {
                sourceId = answer.sourceId; controller = answer.controllerId
                requirements = answer.spellTargetRequirements; exceptions = answer.exceptions; ordinal = null
            }
            is StormCopyModalTargetContinuation -> {
                sourceId = answer.sourceId; controller = answer.controllerId
                requirements = answer.modeTargetRequirements[answer.chosenModes[answer.currentOrdinal]].orEmpty()
                exceptions = answer.exceptions; ordinal = answer.currentOrdinal
            }
            else -> return null
        }
        val selection = selection(state, sourceId, response, ordinal)
        if (selection.retained.size == selection.targets.size) return null
        val source = state.getEntity(sourceId) ?: return "Source spell missing"
        val card = source.get<CardComponent>() ?: return "Source card missing"
        val spell = source.get<SpellOnStackComponent>() ?: return "Source spell missing"
        val (id, scratch) = state.newEntity()
        val copied = CopyExceptionApplier.apply(card, exceptions).copy(ownerId = controller)
        val preview = scratch.withEntity(id, ComponentContainer.of(copied, spell.copy(casterId = controller)))
        return finder.validator.validateTargets(preview, selection.targets, alignedRequirements(requirements, selection.targets.size), controller,
            copied.colors, copied.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }, id, spell.xValue,
            TargetingSourceType.SPELL, retainedTargetIndices = selection.retained)
    }

    // The last requirement owns the remaining targets, including an unbounded/X group.
    // Earlier groups use the recorded counts, as do stack resolution and named-target binding.
    fun alignedRequirements(requirements: List<TargetRequirement>, size: Int): List<TargetRequirement> {
        var left = size
        return requirements.mapIndexed { i, req ->
            val count = if (i == requirements.lastIndex) left else minOf(req.count, left)
            left -= count
            req.withCount(count)
        }
    }

    fun ChosenTarget.entityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Spell -> spellEntityId
        is ChosenTarget.Card -> cardId
    }

    fun legalTargets(
        state: GameState,
        finder: TargetFinder,
        sourceId: EntityId,
        controllerId: EntityId,
        requirements: List<TargetRequirement>,
        exceptions: CopyExceptions,
    ): Map<Int, List<EntityId>> {
        val source = state.getEntity(sourceId)
        val card = source?.get<CardComponent>() ?: return emptyMap()
        val spell = source.get<SpellOnStackComponent>() ?: return emptyMap()
        val (copyId, scratch) = state.newEntity()
        val copiedCard = CopyExceptionApplier.apply(card, exceptions).copy(ownerId = controllerId)
        val preview = scratch.withEntity(copyId, ComponentContainer.of(
            copiedCard,
            spell.copy(casterId = controllerId),
        ))
        val sourceSubtypes = copiedCard.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }
        return requirements.mapIndexed { index, requirement ->
            index to finder.findLegalTargets(
                preview, requirement, controllerId, copyId, targetingSourceType = TargetingSourceType.SPELL
            ).filter { candidate ->
                finder.validator.validateSingleTarget(
                    state = preview,
                    target = com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget(preview, candidate),
                    requirement = requirement,
                    casterId = controllerId,
                    sourceColors = copiedCard.colors,
                    sourceSubtypes = sourceSubtypes,
                    sourceId = copyId,
                    xValue = spell.xValue,
                    targetingSourceType = TargetingSourceType.SPELL,
                ) == null
            }
        }.toMap()
    }
}
