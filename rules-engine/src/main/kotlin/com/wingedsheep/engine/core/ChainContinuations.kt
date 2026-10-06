package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import kotlinx.serialization.Serializable

/**
 * Resume after the inner action's continuations have all resolved.
 *
 * Pushed before executing the inner action; fires after any intermediate
 * decisions (e.g., discard card selection) complete. Simply calls offerChainCopy().
 *
 * @property effect The unified chain copy effect (carries all variant info)
 * @property recipientPlayerId The player who will be offered the copy
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyAfterActionContinuation(
    val effect: ChainCopyEffect,
    val recipientPlayerId: EntityId,
    val sourceId: EntityId?,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AutomaticContinuation

/**
 * Resume after the affected player decides whether to copy the chain spell (yes/no).
 *
 * - Yes → present cost payment (if any) or target selection
 * - No → chain ends
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who gets to copy
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyDecisionContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after the copying player selects a cost resource (land to sacrifice / card to discard).
 *
 * After paying cost, presents target selection for the copy.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateOptions The list of valid cost resource entity IDs (for validation)
 */
@Serializable
data class ChainCopyCostContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateOptions: List<EntityId>,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after the copying player selects a target for the chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with ChainCopyEffect targeting
 * the selected entity, enabling recursive chaining.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class ChainCopyTargetContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after the copy recipient picks mana sources for a mana copy cost ("may pay {R}{R}. If the
 * player does, they may copy this spell" — Chain Lightning).
 *
 * The recipient already said yes to the copy offer; this is the mana-payment window a rule or effect
 * opens mid-resolution (CR 605.3a), so they may tap sources from [availableSources] or activate any
 * mana ability of their own before confirming. Paying moves on to the copy's target selection;
 * declining (or a submission that can't produce the mana) ends the chain — "if the player does" is
 * not satisfied, so no copy is made.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player paying for, and then controlling, the copy
 * @property sourceId The source entity of the original spell/ability
 * @property manaCost The mana cost being paid
 * @property availableSources The source menu the window opened with (submissions are validated against it)
 */
@Serializable
data class ChainCopyManaPaymentContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val manaCost: com.wingedsheep.sdk.core.ManaCost,
    val availableSources: List<ManaSourceOption>,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation
