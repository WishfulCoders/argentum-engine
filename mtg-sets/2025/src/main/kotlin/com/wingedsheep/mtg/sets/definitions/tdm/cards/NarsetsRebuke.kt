package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Narset's Rebuke
 * {4}{R}
 * Instant
 *
 * Narset's Rebuke deals 5 damage to target creature. Add {U}{R}{W}.
 * If that creature would die this turn, exile it instead.
 *
 * The "exile instead of die" clause reuses [MarkExileOnDeathEffect] (see Scorching Lava).
 * The mana ({U}{R}{W}) and the death-replacement both happen regardless of whether the
 * damaged creature survives, so all three sub-effects are composed unconditionally.
 */
val NarsetsRebuke = card("Narset's Rebuke") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Narset's Rebuke deals 5 damage to target creature. Add {U}{R}{W}. " +
        "If that creature would die this turn, exile it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then
            Effects.AddMana(Color.BLUE) then
                Effects.AddMana(Color.RED) then
                Effects.AddMana(Color.WHITE) then
                Effects.MarkExileOnDeath(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Diego Gisbert"
        flavorText = "With a swift motion Narset struck Sarkhan down. She would not let his " +
            "fevered dreams of draconic dominance come to fruition. Not after all she had done to free Tarkir."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5098bd73-d51c-4db4-bf06-fd4854089d37.jpg?1743204422"
    }
}
