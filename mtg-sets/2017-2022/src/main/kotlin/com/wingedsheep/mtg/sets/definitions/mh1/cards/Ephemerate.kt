package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ephemerate — Modern Horizons #7
 * {W} · Instant
 *
 * Exile target creature you control, then return it to the battlefield under its owner's control.
 * Rebound
 *
 * The blink is Momentary Blink's shape (exile, then move the same card back to the battlefield —
 * a token simply ceases to exist in exile). Rebound is the bare [Keyword.REBOUND]: `StackResolver`
 * reads it at resolution, exactly as for Staggershock, and the upkeep recast picks a fresh target.
 */
val Ephemerate = card("Ephemerate") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return it to the battlefield under its owner's control.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Exile(creature) then Effects.Move(creature, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2da5f3f8-5eef-498f-ba2c-2f3fbc3745aa.jpg?1783933164"
        ruling("2019-06-14", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
