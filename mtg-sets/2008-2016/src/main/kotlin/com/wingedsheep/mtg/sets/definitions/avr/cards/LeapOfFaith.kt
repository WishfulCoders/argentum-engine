package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leap of Faith
 * {2}{W}
 * Instant
 *
 * Target creature gains flying until end of turn. Prevent all damage that would be dealt to that creature this turn.
 *
 * "That creature" is the same target both halves act on. The shield is [Effects.PreventDamage] with
 * every parameter at its default except the recipient (Djeru's Resolve).
 */
val LeapOfFaith = card("Leap of Faith") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gains flying until end of turn. Prevent all damage that would be dealt to that " +
        "creature this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature) then Effects.PreventDamage(target = creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Gabor Szikszai"
        flavorText = "The finest maneuvers take place in three dimensions."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7ba52aed-440c-4b32-8f25-0c5364441712.jpg?1783940733"
    }
}
