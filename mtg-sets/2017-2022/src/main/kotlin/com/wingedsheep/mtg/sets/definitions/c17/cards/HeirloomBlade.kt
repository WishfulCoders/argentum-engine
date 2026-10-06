package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heirloom Blade
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +3/+1.
 * Whenever equipped creature dies, you may reveal cards from the top of your library until you
 * reveal a creature card that shares a creature type with it. Put that card into your hand and
 * the rest on the bottom of your library in a random order.
 * Equip {1}
 *
 * Spinner of Souls' reveal-until-to-hand recipe, with the match narrowed to "shares a creature
 * type with it" — `it` being the triggering (dead) creature, compared by its last-known types
 * from the dies event (so a dead token or Changeling still counts).
 */
val HeirloomBlade = card("Heirloom Blade") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+1.\n" +
        "Whenever equipped creature dies, you may reveal cards from the top of your library until " +
        "you reveal a creature card that shares a creature type with it. Put that card into your " +
        "hand and the rest on the bottom of your library in a random order.\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(3, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        optional = true
        effect = Patterns.Library.revealUntilMatchToHand(
            filter = GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.TriggeringEntity)
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Carmen Sinek"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aae598eb-871d-45af-b12c-346c344ae74b.jpg?1783935931"
        ruling(
            "2017-08-25",
            "Compare the revealed cards to the creature as it last existed before it died, not to the " +
                "creature card as it exists in its owner's graveyard, to determine which one you put into your hand."
        )
        ruling(
            "2017-08-25",
            "If the equipped creature has no creature type, no card can share a creature type with it."
        )
        ruling(
            "2017-08-25",
            "If Heirloom Blade leaves the battlefield at the same time that the equipped creature dies, " +
                "its triggered ability triggers."
        )
    }
}
