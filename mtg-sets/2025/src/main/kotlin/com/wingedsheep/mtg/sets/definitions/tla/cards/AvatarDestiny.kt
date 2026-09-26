package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Avatar Destiny
 * {2}{G}{G}
 * Enchantment — Aura
 *
 * Enchant creature you control
 * Enchanted creature gets +1/+1 for each creature card in your graveyard and is an Avatar in
 *   addition to its other types.
 * When enchanted creature dies, mill cards equal to its power. Return this card to its owner's
 *   hand and up to one creature card milled this way to the battlefield under your control.
 *
 * Modeling notes:
 * - The dynamic +1/+1 buff (layer 7c) and the "is an Avatar in addition to its other types"
 *   type addition (layer 4) are continuous static abilities scoped to the enchanted creature.
 * - The death trigger binds to the attached creature (TriggerBinding.ATTACHED). "Its power" is
 *   the dying creature's last-known power (which still includes this Aura's buff, since the
 *   creature and Aura leave simultaneously), read via DynamicAmounts.triggeringPower().
 * - The mill stores the milled cards as "milled" so the final clause can put up to one creature
 *   card from among exactly those cards onto the battlefield under the controller's control.
 *   The Aura itself (now in the graveyard) returns to its owner's hand.
 */
val AvatarDestiny = card("Avatar Destiny") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Enchanted creature gets +1/+1 for each creature card in your graveyard and is an Avatar " +
        "in addition to its other types.\n" +
        "When enchanted creature dies, mill cards equal to its power. Return this card to its " +
        "owner's hand and up to one creature card milled this way to the battlefield under your control."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    // Enchanted creature gets +1/+1 for each creature card in your graveyard.
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.creatureCardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.creatureCardsInYourGraveyard()
        )
    }

    // ...and is an Avatar in addition to its other types.
    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter.attachedCreature(),
            addSubtypes = listOf("Avatar")
        )
    }

    // When enchanted creature dies, mill cards equal to its power. Return this card to its owner's
    // hand and up to one creature card milled this way to the battlefield under your control.
    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Pipeline {
            // Mill cards equal to its power (last-known power of the dying creature).
            val milled = gather(CardSource.TopOfLibrary(DynamicAmounts.triggeringPower()))
            toGraveyard(milled)
            // Return this card (the Aura) to its owner's hand.
            run(Effects.Move(EffectTarget.Self, Zone.HAND))
            // Up to one creature card milled this way to the battlefield under your control.
            val reanimated = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "Put up to one creature card milled this way onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave in graveyard"
            )
            move(reanimated, CardDestination.ToZone(Zone.BATTLEFIELD), markEnteredViaSourceAbility = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Iwamoto05"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d74da72-8443-4fce-9d64-d2041f6a3292.jpg?1764121128"
    }
}
