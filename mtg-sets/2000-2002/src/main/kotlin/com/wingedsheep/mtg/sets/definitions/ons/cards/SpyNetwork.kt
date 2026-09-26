package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownLookScope
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets

/**
 * Spy Network
 * {U}
 * Instant
 * Look at target player's hand, the top card of that player's library, and any
 * face-down creatures they control. Look at the top four cards of your library,
 * then put them back in any order.
 */
val SpyNetwork = card("Spy Network") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at target player's hand, the top card of that player's library, and any face-down creatures they control. Look at the top four cards of your library, then put them back in any order."

    spell {
        val t = target(Targets.Player)
        effect = Effects.LookAtHand(t) then
            Effects.Pipeline {
                val targetTop = gather(CardSource.TopOfLibrary(1, t.asPlayer))
                toLibraryTop(targetTop, t.asPlayer)
            } then
            Effects.LookAtFaceDown(t, FaceDownLookScope.ALL_CONTROLLED_BY_TARGET_PLAYER) then
            Patterns.Library.lookAtTopAndReorder(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Andrew Robinson"
        flavorText = "\"Information is the most dangerous weapon.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a4bed3f-845c-4822-b8af-8b511dce6fe2.jpg?1562927629"
    }
}
