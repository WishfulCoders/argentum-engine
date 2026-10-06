package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hero's Blade
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +3/+2.
 * Whenever a legendary creature you control enters, you may attach this Equipment to it.
 * Equip {4}
 */
val HerosBlade = card("Hero's Blade") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+2.\n" +
        "Whenever a legendary creature you control enters, you may attach this Equipment to it.\n" +
        "Equip {4}"

    staticAbility {
        ability = ModifyStats(3, 2)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.legendary().youControl()).enters()
        optional = true
        effect = Effects.AttachEquipment(EffectTarget.TriggeringEntity)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Aaron Miller"
        flavorText = "The best swords are forged with dragonfire."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e69b72c1-232c-420e-b900-6dfe73cfff13.jpg?1783938671"
        ruling(
            "2020-11-10",
            "If the legendary creature that entered the battlefield leaves the battlefield before Hero's Blade " +
                "becomes attached, Hero's Blade stays as it was. If it's already attached to another creature, " +
                "it remains attached to that creature."
        )
        ruling(
            "2014-11-24",
            "The triggered ability will trigger when one of the Gods from Theros block enters the battlefield " +
                "only if your devotion is high enough that it's a creature when it enters. If Hero's Blade is " +
                "attached to a God that stops being a creature (or any creature that stops being a creature), " +
                "it will become unattached."
        )
    }
}
