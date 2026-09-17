package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stable policy-facing description of a response that cannot be folded into one action ID.
 *
 * [responseType] is the kotlinx-serialization discriminator expected by `POST /envs/{id}/decision`.
 * The concrete payload carries every option and constraint needed to construct that response
 * without reading engine classes. It is deliberately separate from `PendingDecision`: engine-only
 * continuation data and future internal fields do not silently become part of the gym contract.
 */
@Serializable
sealed interface StructuredDecisionPayload {
    val responseType: String
}

@Serializable
data class DecisionCardView(
    val name: String,
    val manaCost: String,
    val typeLine: String,
    val imageUri: String? = null,
    val colors: List<String> = emptyList(),
    val power: Int? = null,
)

@Serializable
data class DecisionTargetRequirementView(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val legalTargetIds: List<EntityId>,
    val sameOwner: Boolean = false,
    val totalManaValueAtMost: Int? = null,
    val differentNames: Boolean = false,
    val differentControllers: Boolean = false,
)

@Serializable
@SerialName("Targets")
data class TargetsDecisionPayload(
    override val responseType: String = "TargetsResponse",
    val requirements: List<DecisionTargetRequirementView>,
    val canCancel: Boolean = false,
    val cancelResponseType: String? = null,
) : StructuredDecisionPayload

@Serializable
data class ConditionalSelectionView(
    val requiredSelections: Int,
    val minimumSelections: Int,
    val matchingOptionIds: List<EntityId>,
    val requiredMatches: Int,
    val description: String? = null,
)

@Serializable
@SerialName("Cards")
data class CardsDecisionPayload(
    override val responseType: String = "CardsSelectedResponse",
    val optionIds: List<EntityId>,
    val minSelections: Int,
    val maxSelections: Int,
    val ordered: Boolean,
    val cardInfo: Map<EntityId, DecisionCardView> = emptyMap(),
    val nonSelectableOptionIds: List<EntityId> = emptyList(),
    val onePerCardType: Boolean = false,
    val onePerColor: Boolean = false,
    val availableColors: List<String> = emptyList(),
    val onePerCardName: Boolean = false,
    val onePerBasicLandType: Boolean = false,
    val onePerPower: Boolean = false,
    val maxTotalManaValue: Int? = null,
    val minTotalManaValue: Int? = null,
    val maxTotalPower: Int? = null,
    val conditionalMinimums: List<ConditionalSelectionView> = emptyList(),
) : StructuredDecisionPayload

@Serializable
data class DecisionModeView(
    val index: Int,
    val text: String,
    val available: Boolean,
)

@Serializable
@SerialName("Modes")
data class ModesDecisionPayload(
    override val responseType: String = "ModesChosenResponse",
    val modes: List<DecisionModeView>,
    val minModes: Int,
    val maxModes: Int,
) : StructuredDecisionPayload

@Serializable
data class ReplacementOptionView(
    val index: Int,
    val label: String,
    val id: String? = null,
    val description: String? = null,
    val iconKey: String? = null,
)

@Serializable
@SerialName("Replacement")
data class ReplacementDecisionPayload(
    override val responseType: String = "ReplacementChosenResponse",
    val fromOptions: List<ReplacementOptionView>,
    val toOptions: List<ReplacementOptionView>,
    val allowedToByFrom: List<List<Int>>,
    val defaultFromIndex: Int? = null,
) : StructuredDecisionPayload

@Serializable
@SerialName("Distribution")
data class DistributionDecisionPayload(
    override val responseType: String = "DistributionResponse",
    val totalAmount: Int,
    val targetIds: List<EntityId>,
    val minPerTarget: Int,
    val maxPerTarget: Map<EntityId, Int>,
    val allowPartial: Boolean,
) : StructuredDecisionPayload

@Serializable
@SerialName("Ordering")
data class OrderingDecisionPayload(
    override val responseType: String = "OrderedResponse",
    val objectIds: List<EntityId>,
    val cardInfo: Map<EntityId, DecisionCardView> = emptyMap(),
) : StructuredDecisionPayload

@Serializable
@SerialName("PileSplit")
data class PileSplitDecisionPayload(
    override val responseType: String = "PilesSplitResponse",
    val cardIds: List<EntityId>,
    val numberOfPiles: Int,
    val pileLabels: List<String>,
    val cardInfo: Map<EntityId, DecisionCardView> = emptyMap(),
) : StructuredDecisionPayload

@Serializable
@SerialName("LibrarySearch")
data class LibrarySearchDecisionPayload(
    override val responseType: String = "CardsSelectedResponse",
    val optionIds: List<EntityId>,
    val minSelections: Int,
    val maxSelections: Int,
    val cards: Map<EntityId, DecisionCardView>,
    val filterDescription: String,
) : StructuredDecisionPayload

@Serializable
@SerialName("DamageAssignment")
data class DamageAssignmentDecisionPayload(
    override val responseType: String = "DamageAssignmentResponse",
    val attackerId: EntityId,
    val availablePower: Int,
    val orderedTargetIds: List<EntityId>,
    val defenderId: EntityId? = null,
    val minimumAssignments: Map<EntityId, Int>,
    val defaultAssignments: Map<EntityId, Int>,
    val hasTrample: Boolean,
    val hasDeathtouch: Boolean,
) : StructuredDecisionPayload

@Serializable
data class CombatAttackerView(
    val id: EntityId,
    val name: String,
    val power: Int,
    val toughness: Int,
    val hasTrample: Boolean,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    val dealsDamageThisStep: Boolean,
    val bandId: String? = null,
    val attackedDefenderId: EntityId,
    val blockedByIds: List<EntityId>,
    val markedDamage: Int,
)

@Serializable
data class CombatBlockerView(
    val id: EntityId,
    val name: String,
    val power: Int,
    val toughness: Int,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    val dealsDamageThisStep: Boolean,
    val blockedAttackerIds: List<EntityId>,
    val orderedAttackers: List<EntityId>,
    val markedDamage: Int,
)

@Serializable
data class CombatDefenderView(
    val id: EntityId,
    val kind: String,
    val name: String,
    val lifeOrLoyaltyOrDefense: Int? = null,
)

@Serializable
data class CombatDamageEdgeView(
    val id: String,
    val sourceId: EntityId,
    val targetId: EntityId,
    val direction: String,
    val amount: Int,
    val maximum: Int,
    val lethal: Int,
    val orderConstrained: Boolean,
    val isTrampleDrain: Boolean,
    val editableBy: EntityId,
)

@Serializable
@SerialName("CombatResolution")
data class CombatResolutionDecisionPayload(
    override val responseType: String = "CombatResolutionResponse",
    val firstStrike: Boolean,
    val attackers: List<CombatAttackerView>,
    val blockers: List<CombatBlockerView>,
    val defenders: List<CombatDefenderView>,
    val edges: List<CombatDamageEdgeView>,
    val coChooserId: EntityId? = null,
) : StructuredDecisionPayload

@Serializable
data class ManaSourceView(
    val entityId: EntityId,
    val name: String,
    val producesColors: Set<Color>,
    val producesColorless: Boolean,
    val requiresSacrifice: Boolean,
    val requiresTappingAnotherPermanent: Boolean,
    val manaAmount: Int,
)

@Serializable
data class WaterbendPermanentView(
    val entityId: EntityId,
    val name: String,
    val isCreature: Boolean,
)

@Serializable
@SerialName("ManaSources")
data class ManaSourcesDecisionPayload(
    override val responseType: String = "ManaSourcesSelectedResponse",
    val availableSources: List<ManaSourceView>,
    val requiredCost: String,
    val autoPaySuggestion: List<EntityId>,
    val canDecline: Boolean,
    val waterbendPermanents: List<WaterbendPermanentView>,
) : StructuredDecisionPayload

@Serializable
data class BudgetModeView(
    val index: Int,
    val cost: Int,
    val description: String,
)

@Serializable
@SerialName("BudgetModal")
data class BudgetModalDecisionPayload(
    override val responseType: String = "BudgetModalResponse",
    val budget: Int,
    val modes: List<BudgetModeView>,
) : StructuredDecisionPayload

private fun SearchCardInfo.toDecisionCardView() = DecisionCardView(
    name = name,
    manaCost = manaCost,
    typeLine = typeLine,
    imageUri = imageUri,
    colors = colors,
    power = power,
)

private fun Map<EntityId, SearchCardInfo>?.toDecisionCardViews(): Map<EntityId, DecisionCardView> =
    this?.mapValues { (_, info) -> info.toDecisionCardView() } ?: emptyMap()

/** Exhaustive subtype projection. A new engine decision cannot compile until classified here. */
internal fun PendingDecision.toStructuredDecisionPayload(): StructuredDecisionPayload? = when (this) {
    is ChooseTargetsDecision -> TargetsDecisionPayload(
        requirements = targetRequirements.map { requirement ->
            DecisionTargetRequirementView(
                index = requirement.index,
                description = requirement.description,
                minTargets = requirement.minTargets,
                maxTargets = requirement.maxTargets,
                legalTargetIds = legalTargets[requirement.index].orEmpty(),
                sameOwner = requirement.sameOwner,
                totalManaValueAtMost = requirement.totalManaValueAtMost,
                differentNames = requirement.differentNames,
                differentControllers = requirement.differentControllers,
            )
        },
        canCancel = canCancel,
        cancelResponseType = if (canCancel) "CancelDecisionResponse" else null,
    )
    is SelectCardsDecision -> CardsDecisionPayload(
        optionIds = options,
        minSelections = minSelections,
        maxSelections = maxSelections,
        ordered = ordered,
        cardInfo = cardInfo.toDecisionCardViews(),
        nonSelectableOptionIds = nonSelectableOptions,
        onePerCardType = onePerCardType,
        onePerColor = onePerColor,
        availableColors = availableColors.orEmpty(),
        onePerCardName = onePerCardName,
        onePerBasicLandType = onePerBasicLandType,
        onePerPower = onePerPower,
        maxTotalManaValue = maxTotalManaValue,
        minTotalManaValue = minTotalManaValue,
        maxTotalPower = maxTotalPower,
        conditionalMinimums = conditionalMinimums.map {
            ConditionalSelectionView(
                requiredSelections = it.requiredSelections,
                minimumSelections = it.minimumSelections,
                matchingOptionIds = it.matchingOptions,
                requiredMatches = it.requiredMatches,
                description = it.description,
            )
        },
    )
    is ChooseModeDecision -> ModesDecisionPayload(
        modes = modes.map { DecisionModeView(it.index, it.text, it.available) },
        minModes = minModes,
        maxModes = maxModes,
    )
    is ChooseReplacementDecision -> ReplacementDecisionPayload(
        fromOptions = fromOptions.mapIndexed { index, label ->
            val metadata = fromMetadata.getOrNull(index)
            ReplacementOptionView(index, label, metadata?.id, metadata?.description, metadata?.iconKey)
        },
        toOptions = toOptions.mapIndexed { index, label ->
            val metadata = toMetadata.getOrNull(index)
            ReplacementOptionView(index, label, metadata?.id, metadata?.description, metadata?.iconKey)
        },
        allowedToByFrom = allowedToByFrom,
        defaultFromIndex = defaultFromIndex,
    )
    is DistributeDecision -> DistributionDecisionPayload(
        totalAmount = totalAmount,
        targetIds = targets,
        minPerTarget = minPerTarget,
        maxPerTarget = maxPerTarget,
        allowPartial = allowPartial,
    )
    is OrderObjectsDecision -> OrderingDecisionPayload(
        objectIds = objects,
        cardInfo = cardInfo.toDecisionCardViews(),
    )
    is SplitPilesDecision -> PileSplitDecisionPayload(
        cardIds = cards,
        numberOfPiles = numberOfPiles,
        pileLabels = pileLabels,
        cardInfo = cardInfo.toDecisionCardViews(),
    )
    is SearchLibraryDecision -> LibrarySearchDecisionPayload(
        optionIds = options,
        minSelections = minSelections,
        maxSelections = maxSelections,
        cards = cards.toDecisionCardViews(),
        filterDescription = filterDescription,
    )
    is ReorderLibraryDecision -> OrderingDecisionPayload(
        objectIds = cards,
        cardInfo = cardInfo.toDecisionCardViews(),
    )
    is AssignDamageDecision -> DamageAssignmentDecisionPayload(
        attackerId = attackerId,
        availablePower = availablePower,
        orderedTargetIds = orderedTargets,
        defenderId = defenderId,
        minimumAssignments = minimumAssignments,
        defaultAssignments = defaultAssignments,
        hasTrample = hasTrample,
        hasDeathtouch = hasDeathtouch,
    )
    is CombatResolutionDecision -> CombatResolutionDecisionPayload(
        firstStrike = firstStrike,
        attackers = attackers.map {
            CombatAttackerView(
                it.id, it.name, it.power, it.toughness, it.hasTrample, it.hasDeathtouch,
                it.hasFirstStrike, it.hasDoubleStrike, it.dealsDamageThisStep, it.bandId,
                it.attackedDefenderId, it.blockedByIds, it.markedDamage,
            )
        },
        blockers = blockers.map {
            CombatBlockerView(
                it.id, it.name, it.power, it.toughness, it.hasDeathtouch, it.hasFirstStrike,
                it.hasDoubleStrike, it.dealsDamageThisStep, it.blockedAttackerIds,
                it.orderedAttackers, it.markedDamage,
            )
        },
        defenders = defenders.map {
            CombatDefenderView(it.id, it.kind.name, it.name, it.lifeOrLoyaltyOrDefense)
        },
        edges = edges.map {
            CombatDamageEdgeView(
                it.id, it.sourceId, it.targetId, it.direction.name, it.amount, it.maximum,
                it.lethal, it.orderConstrained, it.isTrampleDrain, it.editableBy,
            )
        },
        coChooserId = coChooserId,
    )
    is SelectManaSourcesDecision -> ManaSourcesDecisionPayload(
        availableSources = availableSources.map {
            ManaSourceView(
                it.entityId, it.name, it.producesColors, it.producesColorless,
                it.requiresSacrifice, it.requiresTappingAnotherPermanent, it.manaAmount,
            )
        },
        requiredCost = requiredCost,
        autoPaySuggestion = autoPaySuggestion,
        canDecline = canDecline,
        waterbendPermanents = waterbendPermanents.map {
            WaterbendPermanentView(it.entityId, it.name, it.isCreature)
        },
    )
    is BudgetModalDecision -> BudgetModalDecisionPayload(
        budget = budget,
        modes = modes.mapIndexed { index, mode -> BudgetModeView(index, mode.cost, mode.description) },
    )

    // These shapes are fully enumerated into LegalActionView entries and do not need a second
    // structured payload. Keeping them explicit makes this projection exhaustive by construction.
    is YesNoDecision,
    is BatchYesNoDecision,
    is ChooseColorDecision,
    is ChooseNumberDecision,
    is ChooseOptionDecision -> null
}
