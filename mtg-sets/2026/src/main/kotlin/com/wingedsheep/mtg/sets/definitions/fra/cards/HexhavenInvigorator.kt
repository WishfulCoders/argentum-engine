package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Hexhaven Invigorator — Reality Fracture #106
 * {G}{G}{G}{G} · Creature — Chimera Horror · 6/6
 *
 * Vigilance
 * Whenever this creature is dealt damage, you may search your library for up to that many land
 * cards, put them onto the battlefield tapped, then shuffle.
 *
 * "That many" is the damage the trigger saw ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT], the
 * Broodhatch Nantuko read). The "you may" gates the whole search, so declining neither searches
 * nor shuffles.
 */
val HexhavenInvigorator = card("Hexhaven Invigorator") {
    manaCost = "{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Chimera Horror"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "Whenever this creature is dealt damage, you may search your library for up to that many " +
        "land cards, put them onto the battlefield tapped, then shuffle."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land,
                count = DynamicAmounts.triggerDamageAmount(),
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
        description = "Whenever this creature is dealt damage, you may search your library for up to " +
            "that many land cards, put them onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Olivier Bernard"
        flavorText = "\"Behold ideal symbiosis.\"\n—Kwia Vigorbloom"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a446cae-e93c-4574-8ffd-7688f9729a8a.jpg?1788878191"
        inBooster = false
    }
}
