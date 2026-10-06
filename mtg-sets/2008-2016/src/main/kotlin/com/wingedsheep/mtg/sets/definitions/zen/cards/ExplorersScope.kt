package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Explorer's Scope
 * {1}
 * Artifact — Equipment
 * Whenever equipped creature attacks, look at the top card of your library. If it's a land
 * card, you may put it onto the battlefield tapped.
 * Equip {1}
 *
 * The attack trigger mirrors Mobile Homestead's top-card pipeline: a non-land (or a declined
 * land) stays on top of the library.
 */
val ExplorersScope = card("Explorer's Scope") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, look at the top card of your library. If it's a land card, you may put it onto the battlefield tapped.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val landCards = filter(looked, GameObjectFilter.Land)
            val toBattlefield = chooseUpTo(1, from = landCards, selectedLabel = "Put onto the battlefield tapped")
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea3a9cdb-4842-44b3-8143-0fda31692600.jpg?1783942126"
    }
}
