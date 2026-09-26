package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

// Create a token that's a copy of the card in this Saga's linked exile, except it isn't legendary
// and is a Mutant in addition to its other types (the Mardu Siegebreaker gather→copy idiom).
private fun cloneFromLinkedExile(): Effect = Effects.Pipeline {
    val exiled = gather(CardSource.FromLinkedExile())
    run(
        Effects.CreateTokenCopyOfTarget(
            target = exiled.asTarget,
            removedSupertypes = setOf(Supertype.LEGENDARY),
            addedSubtypes = setOf(Subtype("Mutant"))
        )
    )
}

/**
 * The Cloning of Shredder
 * {4}{B}{B}
 * Enchantment — Saga
 *
 * I — Exile target creature card from your graveyard. Create a token that's a copy of it,
 *     except it isn't legendary and is a Mutant in addition to its other types.
 * II, III — Create a token that's a copy of a card exiled with this Saga, except it isn't
 *     legendary and is a Mutant in addition to its other types.
 */
val TheCloningOfShredder = card("The Cloning of Shredder") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile target creature card from your graveyard. Create a token that's a copy of it, except it isn't legendary and is a Mutant in addition to its other types.\n" +
        "II, III — Create a token that's a copy of a card exiled with this Saga, except it isn't legendary and is a Mutant in addition to its other types."

    sagaChapter(1) {
        val target = target(TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD))
        // Exile the targeted graveyard card linked to this Saga, then copy it.
        effect = Effects.Move(target, Zone.EXILE, linkToSource = true) then cloneFromLinkedExile()
    }

    sagaChapter(2) {
        effect = cloneFromLinkedExile()
    }

    sagaChapter(3) {
        effect = cloneFromLinkedExile()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94449d88-2df4-4850-8ffa-d6d193835dda.jpg?1771603451"
    }
}
