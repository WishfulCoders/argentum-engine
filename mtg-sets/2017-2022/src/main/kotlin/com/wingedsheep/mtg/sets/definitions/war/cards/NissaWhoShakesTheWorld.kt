package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nissa, Who Shakes the World — War of the Spark #169
 * {3}{G}{G} · Legendary Planeswalker — Nissa · Starting loyalty 5
 *
 * Whenever you tap a Forest for mana, add an additional {G}.
 * +1: Put three +1/+1 counters on up to one target noncreature land you control. Untap it. It
 *     becomes a 0/0 Elemental creature with vigilance and haste that's still a land.
 * −8: You get an emblem with "Lands you control have indestructible." Search your library for any
 *     number of Forest cards, put them onto the battlefield tapped, then shuffle.
 *
 * - The static is a triggered mana ability (CR 605.1b), so it is [AdditionalManaOnSourceTap] — High
 *   Tide's shape — over Forests you control (only a permanent's controller can tap it for mana).
 *   The Forest test reads projected subtypes, so a land made a Forest (Yavimaya) counts.
 * - +1 is Tendril of the Mycotyrant's animation with vigilance added and an untap; per the ruling
 *   the effect lasts indefinitely ([Duration.Permanent]).
 * - −8's emblem grants indestructible to the group "lands you control", re-evaluated each
 *   projection so later lands are covered. "Any number" of Forest cards is a search for up to the
 *   whole library.
 */
val NissaWhoShakesTheWorld = card("Nissa, Who Shakes the World") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 5
    oracleText = "Whenever you tap a Forest for mana, add an additional {G}.\n" +
        "+1: Put three +1/+1 counters on up to one target noncreature land you control. Untap it. It " +
        "becomes a 0/0 Elemental creature with vigilance and haste that's still a land.\n" +
        "−8: You get an emblem with \"Lands you control have indestructible.\" Search your library for " +
        "any number of Forest cards, put them onto the battlefield tapped, then shuffle."

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.withSubtype(Subtype.FOREST).youControl(),
            color = Color.GREEN,
        )
    }

    loyaltyAbility(+1) {
        val land = target(TargetFilter(GameObjectFilter.Land.notCreature().youControl()), optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, land) then
            Effects.Untap(land) then
            Effects.BecomeCreature(
                target = land,
                power = 0,
                toughness = 0,
                keywords = setOf(Keyword.VIGILANCE, Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.Permanent
            )
        description = "Put three +1/+1 counters on up to one target noncreature land you control. Untap it. " +
            "It becomes a 0/0 Elemental creature with vigilance and haste that's still a land."
    }

    loyaltyAbility(-8) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter(GameObjectFilter.Land.youControl()),
            grantedKeywords = listOf(Keyword.INDESTRUCTIBLE.name),
            emblemDescription = "Lands you control have indestructible."
        ) then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
            count = DynamicAmounts.zone(Player.You, Zone.LIBRARY).count(),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "You get an emblem with \"Lands you control have indestructible.\" Search your library " +
            "for any number of Forest cards, put them onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f857bbe4-5619-4733-a0c7-69700f2ef4f3.jpg?1783933410"

        ruling("2019-05-03", "The effect of Nissa's first loyalty ability lasts indefinitely. It doesn't wear off during the cleanup step.")
    }
}
