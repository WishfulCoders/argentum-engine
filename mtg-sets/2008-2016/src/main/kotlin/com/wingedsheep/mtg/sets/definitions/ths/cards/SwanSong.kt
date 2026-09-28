package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swan Song — Theros #65 (canonical printing)
 * {U} · Instant
 *
 * Counter target enchantment, instant, or sorcery spell. Its controller creates a 2/2 blue Bird
 * creature token with flying.
 *
 * The Bird is created before the counter, while the spell is still on the stack, so
 * `TargetController` reads the spell's controller (same order as An Offer You Can't Refuse). The
 * token doesn't depend on the counter: per the ruling, a spell that can't be countered still
 * gives its controller a Bird.
 */
val SwanSong = card("Swan Song") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target enchantment, instant, or sorcery spell. Its controller creates a " +
        "2/2 blue Bird creature token with flying."

    spell {
        target(
            TargetFilter(
                GameObjectFilter.Enchantment or GameObjectFilter.Instant or GameObjectFilter.Sorcery,
                zone = Zone.STACK
            )
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            controller = EffectTarget.TargetController,
            imageUri = "https://cards.scryfall.io/normal/front/c/a/ca72703f-d45b-4c80-98a8-55fad1fcf431.jpg?1783939699"
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Peter Mohrbacher"
        flavorText = "\"The most enlightened mages create beauty from violence.\"\n—Medomai the Ageless"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efd26041-059b-4a1e-9ce8-c3cfd69a3721.jpg?1783939790"
        ruling(
            "2013-09-15",
            "Swan Song can target a spell that can't be countered. That spell won't be countered when " +
                "Swan Song resolves, but its controller will get a Bird token."
        )
    }
}
