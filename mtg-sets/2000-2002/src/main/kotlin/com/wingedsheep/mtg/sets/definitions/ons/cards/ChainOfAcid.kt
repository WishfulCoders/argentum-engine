package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chain of Acid
 * {3}{G}
 * Sorcery
 * Destroy target noncreature permanent. Then that permanent's controller may copy this spell
 * and may choose a new target for that copy.
 */
val ChainOfAcid = card("Chain of Acid") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target noncreature permanent. Then that permanent's controller may copy this spell and may choose a new target for that copy."

    spell {
        val permanent = TargetObject(filter = TargetFilter.NoncreaturePermanent)
        val t = target(permanent)
        effect = Effects.ChainCopy(
            action = Effects.Destroy(t),
            target = t,
            offerTo = CopyRecipient.TARGET_CONTROLLER,
            copyTarget = permanent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Arnie Swekel"
        flavorText = "\"We have no quarrel with you,\" said the elf. \"But neither do we have sympathy.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d47ddca-a363-4ab7-b7f2-d0e0043c9916.jpg?1562901859"
    }
}
