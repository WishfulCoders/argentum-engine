package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Infernal Scarring
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+0 and has "When this creature dies, draw a card."
 *
 * The quoted ability is granted to the *host*, not run by the Aura, so it is a
 * [GrantTriggeredAbility] with the default self-scoped filter: the dies trigger belongs to the
 * enchanted creature, and the card is drawn by that creature's controller.
 */
val InfernalScarring = card("Infernal Scarring") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+0 and has \"When this creature dies, draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dies(),
                effect = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Mike Bierek"
        flavorText = "One who is marked by a demon in life is sure to be remembered as one in death."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1ead8cc-5b7d-4a6d-a56f-95da91a958d2.jpg"
    }
}
