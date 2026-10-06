package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Yotian Frontliner
 * {1}
 * Artifact Creature — Soldier
 * 1/1
 * Whenever this creature attacks, another target creature you control gets +1/+1 until end of turn.
 * Unearth {W}
 */
val YotianFrontliner = card("Yotian Frontliner") {
    manaCost = "{1}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature attacks, another target creature you control gets +1/+1 until end of turn.\n" +
        "Unearth {W} ({W}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val other = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.ModifyStats(1, 1, other)
    }

    unearth("{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfa05d1e-6728-4e77-bfb2-da0b9598e44b.jpg?1783920115"
        ruling("2022-10-14", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2022-10-14", "Unearth grants haste to the permanent that's returned to the battlefield (even if it's not a creature card). However, neither of the \"exile\" abilities is granted to that permanent. If that permanent loses all its abilities, it will still be exiled at the beginning of the next end step, and if it would leave the battlefield, it is still exiled instead.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
