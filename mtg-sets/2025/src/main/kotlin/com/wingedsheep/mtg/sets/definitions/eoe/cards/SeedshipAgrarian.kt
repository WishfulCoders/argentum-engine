package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Seedship Agrarian
 * {3}{G}
 * Creature — Insect Scientist
 * Whenever this creature becomes tapped, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 * 3/3
 */
val SeedshipAgrarian = card("Seedship Agrarian") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Scientist"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes tapped, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")\nLandfall — Whenever a land you control enters, put a +1/+1 counter on this creature."

    // Whenever this creature becomes tapped, create a Lander token
    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.CreateLander()
    }

    // Landfall: put a +1/+1 counter on this creature
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Helge C. Balzer"
        flavorText = "Knowledge isn't found, but grown."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fc7946d-37b0-4dc8-9daa-8d2204d8e4d2.jpg?1752947386"
    }
}
