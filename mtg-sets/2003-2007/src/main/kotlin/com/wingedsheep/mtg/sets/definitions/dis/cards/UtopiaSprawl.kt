package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Utopia Sprawl
 * {G}
 * Enchantment — Aura
 * Enchant Forest
 * As this Aura enters, choose a color.
 * Whenever enchanted Forest is tapped for mana, its controller adds an additional one mana of the
 * chosen color.
 *
 * Shimmerwilds Growth without the colour change: the chosen colour only picks the extra mana
 * (`AdditionalManaOnTap(color = null)` reads it off the Aura), and the Forest still makes {G}.
 */
val UtopiaSprawl = card("Utopia Sprawl") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Forest\nAs this Aura enters, choose a color.\n" +
        "Whenever enchanted Forest is tapped for mana, its controller adds an additional one mana of the chosen color."
    auraTarget = TargetObject(filter = TargetFilter.Land.withSubtype("Forest"))

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))
    staticAbility {
        ability = AdditionalManaOnTap(color = null, amount = DynamicAmount.Fixed(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5047e271-fbf1-402c-9eb9-0806e5988f76.jpg?1783943407"
    }
}
