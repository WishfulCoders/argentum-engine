package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Persist
 * {1}{B}
 * Sorcery
 * Return target nonlegendary creature card from your graveyard to the battlefield with a -1/-1
 * counter on it.
 *
 * "With a -1/-1 counter on it" is the move's own `addCounterType` (Vigor Mortis / Recommission): the
 * counter is placed as the card lands, not by a later effect, so an X/1 returns and dies to
 * state-based actions without ever being a 1/1-sized creature on the battlefield. `fromZone` keeps the
 * graveyard guard.
 */
val Persist = card("Persist") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target nonlegendary creature card from your graveyard to the battlefield with a " +
        "-1/-1 counter on it."

    spell {
        val creatureCard = target(
            TargetFilter(GameObjectFilter.Creature.nonlegendary().ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.MINUS_ONE_MINUS_ONE,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Milivoj Ćeran"
        flavorText = "In the tight-knit doun of Mistmeadow, death is less important than duty."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90f390c3-af1c-424f-9721-e26e9321e5a3.jpg?1783926857"
    }
}
