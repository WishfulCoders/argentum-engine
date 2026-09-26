package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Wirewood Hivemaster
 * {1}{G}
 * Creature — Elf
 * 1/1
 * Whenever another nontoken Elf enters, you may create a 1/1 green Insect creature token.
 */
val WirewoodHivemaster = card("Wirewood Hivemaster") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1
    oracleText = "Whenever another nontoken Elf enters, you may create a 1/1 green Insect creature token."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsPermanent,
                        CardPredicate.HasSubtype(Subtype("Elf")),
                        CardPredicate.IsNontoken
                    )
                )).enters()
        optional = true
        effect = Effects.CreateToken(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Darrell Riche"
        flavorText = "\"Most insects have been drawn to the Mirari. But all that remain in Wirewood are under my care.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea55b4fc-366f-4906-9eaa-9085f6a22612.jpg?1562942070"
    }
}
