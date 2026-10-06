package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evokeWith
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Solitude — Modern Horizons 2 #32
 * {3}{W}{W} · Creature — Elemental Incarnation · 3 / 2
 *
 * Flash
 * Lifelink
 * When this creature enters, exile up to one other target creature. That creature's controller
 * gains life equal to its power.
 * Evoke—Exile a white card from your hand.
 *
 * The evoke cost is non-mana only: [evokeWith] an exile-a-white-card-from-hand cost, so the cast
 * is still *evoked* and the engine's evoke sacrifice trigger follows the enters trigger. The
 * enters trigger is Swords to Plowshares on a body — the life gain reads the creature's power
 * (its last-known power once it is exiled) and goes to that creature's controller.
 */
val Solitude = card("Solitude") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "Lifelink\n" +
        "When this creature enters, exile up to one other target creature. That creature's " +
        "controller gains life equal to its power.\n" +
        "Evoke—Exile a white card from your hand."

    keywords(Keyword.FLASH, Keyword.LIFELINK)

    evokeWith(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Any.withColor(Color.WHITE),
            fromZone = CostZone.HAND
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.GainLife(DynamicAmounts.powerOf(creature), EffectTarget.TargetController) then
            Effects.Exile(creature)
        description = "exile up to one other target creature. That creature's controller gains life " +
            "equal to its power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "32"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47a6234f-309f-4e03-9263-66da48b57153.jpg?1783926885"
        ruling(
            "2021-06-18",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as an evoke cost), add any cost increases, then apply any cost reductions. " +
                "The mana value of the spell is determined by only its mana cost, no matter what the total " +
                "cost to cast that spell was."
        )
        ruling(
            "2021-06-18",
            "If you pay the evoke cost, you can have the creature's own triggered ability resolve before " +
                "the evoke triggered ability. You can cast spells after that ability resolves but before " +
                "you have to sacrifice the creature."
        )
    }
}
