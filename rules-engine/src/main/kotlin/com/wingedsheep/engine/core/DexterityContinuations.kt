package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.dexterity.FlipTable
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume after the player threw the card of a [com.wingedsheep.sdk.scripting.effects.FlipOntoBattlefieldEffect]:
 * roll the landing on [table] and publish the touched permanents under [storeTouchedAs].
 */
@Serializable
data class FlipOntoBattlefieldContinuation(
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String,
    val sourceImageUri: String?,
    val table: FlipTable,
    val storeTouchedAs: String,
) : AnswerContinuation
