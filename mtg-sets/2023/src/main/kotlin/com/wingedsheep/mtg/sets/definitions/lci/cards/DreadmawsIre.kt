package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Dreadmaw's Ire
 * {R}
 * Instant
 * Until end of turn, target attacking creature gets +2/+2 and gains trample and "Whenever this
 * creature deals combat damage to a player, destroy target artifact that player controls."
 */
val DreadmawsIre = card("Dreadmaw's Ire") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Until end of turn, target attacking creature gets +2/+2 and gains trample and " +
        "\"Whenever this creature deals combat damage to a player, destroy target artifact that player controls.\""

    spell {
        val t = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(2, 2, t) then
            Effects.GrantKeyword(Keyword.TRAMPLE, t) then
            Effects.GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                    val artifact = target(TargetFilter(GameObjectFilter.Artifact.controlledByTriggeringPlayer()))
                    effect = Effects.Destroy(artifact)
                    description = "Whenever this creature deals combat damage to a player, " +
                        "destroy target artifact that player controls."
                },
                target = t
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Crystal Sully"
        flavorText = "\"That's the fifth ship lost in three days! Can't we build them out of something stronger?\"\n—Malcolm Lee"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/062e00a1-f1dc-4089-b640-800ab781c590.jpg?1782694492"
    }
}
