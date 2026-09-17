package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captain Lannery Storm
 * {2}{R}
 * Legendary Creature — Human Pirate
 * 2/2
 * Haste
 * Whenever Captain Lannery Storm attacks, create a Treasure token.
 * Whenever you sacrifice a Treasure, Captain Lannery Storm gets +1/+0 until end of turn.
 *
 * The last ability triggers on any Treasure sacrifice, not only for mana (2017-09-29 ruling), and
 * not for Treasures sacrificed to cast her, since she is not yet on the battlefield then.
 */
val CaptainLanneryStorm = card("Captain Lannery Storm") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever Captain Lannery Storm attacks, create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Whenever you sacrifice a Treasure, Captain Lannery Storm gets +1/+0 until end of turn."
    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateTreasure(1)
    }
    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Treasure"))
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ab86a8a-7a0a-473e-9a97-da2fe0ab866c.jpg?1783935752"
        ruling("2017-09-29", "Captain Lannery Storm's last ability triggers whenever you sacrifice a Treasure for any reason, not just to activate a Treasure's mana ability.")
        ruling("2017-09-29", "If you sacrifice Treasures to cast Captain Lannery Storm, its last ability won't trigger for those Treasures.")
    }
}
