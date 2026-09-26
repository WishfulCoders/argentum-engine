package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tale of Tinúviel
 * {3}{W}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Target creature you control gains indestructible for as long as you control this Saga.
 * II — Return target creature card from your graveyard to the battlefield.
 * III — Up to two target creatures you control each gain lifelink until end of turn.
 */
val TaleOfTinuviel = card("Tale of Tinúviel") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Target creature you control gains indestructible for as long as you control this Saga.\n" +
        "II — Return target creature card from your graveyard to the battlefield.\n" +
        "III — Up to two target creatures you control each gain lifelink until end of turn."

    sagaChapter(1) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(
            Keyword.INDESTRUCTIBLE,
            creature,
            Duration.WhileSourceOnBattlefield("this Saga")
        )
    }

    sagaChapter(2) {
        val creatureCardFromYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(
            creatureCardFromYourGraveyard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD
        )
    }

    sagaChapter(3) {
        targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ae65f96-7bfd-4390-88bf-764c26bf4668.jpg?1688569295"
    }
}
