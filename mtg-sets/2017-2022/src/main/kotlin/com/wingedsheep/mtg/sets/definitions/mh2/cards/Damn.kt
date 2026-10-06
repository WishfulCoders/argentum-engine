package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Damn
 * {B}{B}
 * Sorcery
 *
 * Destroy target creature. A creature destroyed this way can't be regenerated.
 * Overload {2}{W}{W}
 *
 * Overloaded, "target creature" reads "each creature" (CR 702.96a) and the spell has no targets
 * (CR 702.96b): a Wrath of God that also ignores hexproof and protection. The overload cost's
 * white mana puts W in the colour identity even though the card itself is black.
 */
val Damn = card("Damn") {
    manaCost = "{B}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. A creature destroyed this way can't be regenerated.\n" +
        "Overload {2}{W}{W} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{2}{W}{W}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature, noRegenerate = true)

        overloadEffect = Effects.DestroyAll(GameObjectFilter.Creature, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efeae088-9ac5-4d2f-a15c-d8675a471ac5.jpg?1783926863"
        ruling("2024-01-12", "If you don't pay the overload cost of a spell with overload, that spell will have a single target. If you pay the overload cost, the spell won't have any targets.")
        ruling("2024-01-12", "Because a spell with overload doesn't target when its overload cost is paid, it may affect permanents with hexproof or with protection from the appropriate color.")
        ruling("2024-01-12", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying (such as an overload cost), add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2024-01-12", "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't choose to pay its overload cost instead.")
    }
}
