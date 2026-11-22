package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.scavengeAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Young Deathclaws
 * {2}{B}{G}
 * Creature — Lizard Mutant
 * 4/2
 *
 * Menace
 * Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost.
 *
 * One granted scavenge ability whose cost is each card's own mana cost.
 */
val YoungDeathclaws = card("Young Deathclaws") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Lizard Mutant"
    power = 4
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost. " +
        "(Exile a creature card from your graveyard and pay its mana cost: Put a number of +1/+1 counters " +
        "equal to that card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.MENACE)

    staticAbility {
        ability = GrantActivatedAbility(
            scavengeAbility(),
            GroupFilter(GameObjectFilter.Creature.ownedByYou()),
            recipientZone = Zone.GRAVEYARD,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d5e6bd-de46-4bc2-854b-cc6cae713f98.jpg?1783912381"
        ruling("2024-03-08", "Exiling the creature card with scavenge is part of the cost of activating the scavenge ability. Once the ability is activated and the cost is paid, it's too late to stop the ability from being activated by trying to remove the creature card from the graveyard.")
        ruling("2024-03-08", "While Young Deathclaws is on the battlefield, the scavenge cost of a creature card in your graveyard is equal to its mana cost, including any colored mana requirements.")
        ruling("2024-03-08", "The number of counters that a card's scavenge ability puts on a creature is based on the card's power as it last existed in the graveyard.")
        ruling("2024-03-08", "If the creature card you scavenge has {X} in its mana cost, X is 0.")
        ruling("2024-03-08", "If a creature card has multiple instances of scavenge, you can activate either ability (but not both, as the card will be exiled when you activate one of them). Young Deathclaws's ability doesn't affect the scavenge cost of a creature's other scavenge ability.")
    }
}
