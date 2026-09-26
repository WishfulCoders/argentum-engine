package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hagra Crocodile
 * {3}{B}
 * Creature — Crocodile
 * 3/1
 * This creature can't block.
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 *
 * Landfall is `Triggers.a(GameObjectFilter.Land.youControl()).enters()` — the `ZoneChangeEvent` over
 * `GameObjectFilter.Land.youControl()` with `TriggerBinding.ANY`.
 */
val HagraCrocodile = card("Hagra Crocodile") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Crocodile"
    power = 3
    toughness = 1
    oracleText = "This creature can't block.\n" +
        "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Daren Bader"
        flavorText = "The creatures of Zendikar are opportunists, eating whatever is available to them. Like goblins. Or boats."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3394a7a2-8f1a-4f07-843a-77db62de49bd.jpg"
    }
}
