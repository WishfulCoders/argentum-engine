package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * "Choose a land of each basic land type, then destroy those lands." The ability's controller
 * makes every choice, from among all players' lands (2020-08-07), so this is
 * `chooseOnePerCategory` with a single [Chooser.Controller] over [Filters.BasicLandTypes]:
 * a type no land has is skipped, a lone candidate is forced (even one you control), a land with
 * two basic land types may be the pick for both, and the picks are destroyed together in one
 * move after the last choice — no player gets a window in between (no mana abilities).
 */
private val destroyALandOfEachBasicType = Effects.Pipeline {
    val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
    destroy(chooseOnePerCategory(lands, Filters.BasicLandTypes, chooser = Chooser.Controller, purpose = "destroy"))
}

/**
 * Sundering Titan
 * {8}
 * Artifact Creature — Golem
 * 7/10
 * When this creature enters or leaves the battlefield, choose a land of each basic land type,
 * then destroy those lands.
 *
 * "Enters or leaves" is written as the two triggers it fires on, each with the same effect
 * (Aven Riftwatcher's shape). The leaves half is controlled by the Titan's last controller.
 */
val SunderingTitan = card("Sundering Titan") {
    manaCost = "{8}"
    typeLine = "Artifact Creature — Golem"
    power = 7
    toughness = 10
    oracleText = "When this creature enters or leaves the battlefield, choose a land of each basic " +
        "land type, then destroy those lands."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = destroyALandOfEachBasicType
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = destroyALandOfEachBasicType
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Jim Murray"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e4bd552-e629-41a4-868f-d656187317b7.jpg?1783944418"
        ruling(
            "2020-08-07",
            "Sundering Titan's ability isn't targeted. When it resolves, Sundering Titan's controller must " +
                "choose one land for each basic land type (Plains, Island, Swamp, Mountain, and Forest), and " +
                "then they are destroyed simultaneously."
        )
        ruling(
            "2020-08-07",
            "Players can't take actions in between the time you choose the lands and the time you destroy " +
                "them. Notably, they can't activate mana abilities of those lands."
        )
        ruling(
            "2020-08-07",
            "If one of the basic land types isn't present, it isn't chosen. If the only land of a certain " +
                "type is one you control, you must choose it."
        )
        ruling("2020-08-07", "If a land has more than one basic land type, it can be chosen more than once.")
    }
}
