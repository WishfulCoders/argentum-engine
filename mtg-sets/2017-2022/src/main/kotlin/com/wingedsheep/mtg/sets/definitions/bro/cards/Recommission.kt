package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Recommission — The Brothers' War #22
 * {1}{W} · Sorcery
 *
 * Return target artifact or creature card with mana value 3 or less from your graveyard to the
 * battlefield. If a creature enters this way, it enters with an additional +1/+1 counter on it.
 *
 * "Enters with" rides along with the move (`addCounterType`), and "if a creature enters this way" is
 * `addCounterIf`, read off projected state as the card lands — so a noncreature artifact that March
 * of the Machines makes a creature on arrival still gets the counter (the 2022-10-14 ruling).
 */
val Recommission = card("Recommission") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target artifact or creature card with mana value 3 or less from your graveyard " +
        "to the battlefield. If a creature enters this way, it enters with an additional +1/+1 counter on it."

    spell {
        val returned = target(
            TargetFilter(
                GameObjectFilter.CreatureOrArtifact.ownedByYou().manaValueAtMost(3),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Move(
            returned,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
            addCounterIf = GameObjectFilter.Creature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Andrew Mar"
        flavorText = "\"Put your backs into it! If we don't lay claim to it, the Fallaji will!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a64e330-1257-4ec3-9a75-889cdcac3ade.jpg?1783920125"
        ruling(
            "2022-10-14",
            "If a noncreature artifact is returned to the battlefield this way and another effect would " +
                "cause that artifact to be a creature while it is on the battlefield (such as the ability of " +
                "March of the Machines), that artifact will enter the battlefield with an additional +1/+1 counter."
        )
    }
}
