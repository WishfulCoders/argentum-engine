package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Magda, Brazen Outlaw
 * {1}{R} — Legendary Creature — Dwarf Berserker 2/1 (Rare) — Kaldheim #142
 * Artist: Slawomir Maniak
 *
 * Other Dwarves you control get +1/+0.
 * Whenever a Dwarf you control becomes tapped, create a Treasure token.
 * Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that card
 * onto the battlefield, then shuffle.
 *
 * "A Dwarf you control" includes Magda herself. The tap trigger needs an untapped → tapped change
 * (ruling 2021-02-05), which is exactly what the engine's tap event reports.
 */
val MagdaBrazenOutlaw = card("Magda, Brazen Outlaw") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Berserker"
    power = 2
    toughness = 1
    oracleText = "Other Dwarves you control get +1/+0.\n" +
        "Whenever a Dwarf you control becomes tapped, create a Treasure token.\n" +
        "Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that card onto the battlefield, then shuffle."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Dwarf").youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dwarf").youControl()).becomesTapped()
        effect = Effects.CreateTreasure(1)
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(5, GameObjectFilter.Artifact.withSubtype("Treasure"))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype("Dragon"),
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
        description = "Sacrifice five Treasures: Search your library for an artifact or Dragon card, " +
            "put that card onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/079e6263-e54c-4899-a336-5315909b9322.jpg?1783928229"
        ruling("2021-02-05", "Magda's triggered ability doesn't allow you to tap any Dwarves. You have to find some other way to tap them. Attacking is still a great way to go.")
        ruling("2021-02-05", "For the triggered ability to trigger, a Dwarf you control has to actually change from untapped to tapped. If an effect attempts to tap a Dwarf you control while it is already tapped, the ability won't trigger.")
    }
}
