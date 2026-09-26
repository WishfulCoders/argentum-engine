package com.wingedsheep.sdk.tooling

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdScope

/**
 * Post-deserialization processing to assign AbilityIds.
 *
 * Card JSON should not contain ability IDs. After deserializing a CardDefinition, call
 * [withGeneratedIds] to replace any placeholder/deserialized IDs on its top-level abilities with
 * ones minted under the card's name — the same ids `card(name) { }` would give them, and the same
 * on every load.
 */
fun CardDefinition.withGeneratedIds(): CardDefinition = AbilityIdScope.within(name) {
    val newScript = script.copy(
        triggeredAbilities = script.triggeredAbilities.map { it.copy(id = AbilityId.next()) },
        activatedAbilities = script.activatedAbilities.map { it.copy(id = AbilityId.next()) },
    )
    copy(
        script = newScript,
        backFace = backFace?.withGeneratedIds()
    )
}
