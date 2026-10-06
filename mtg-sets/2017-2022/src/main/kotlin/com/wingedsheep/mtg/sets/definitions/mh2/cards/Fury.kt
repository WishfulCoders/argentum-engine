package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evokeWith
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fury — Modern Horizons 2 #126
 * {3}{R}{R} · Creature — Elemental Incarnation · 3 / 3
 *
 * Double strike
 * When this creature enters, it deals 4 damage divided as you choose among any number of target
 * creatures and/or planeswalkers.
 * Evoke—Exile a red card from your hand.
 *
 * The evoke cost is non-mana only ([evokeWith] an exile-a-red-card-from-hand cost); the evoke
 * sacrifice trigger is the engine's. The enters trigger takes zero to four creature/planeswalker
 * targets (each must be dealt at least 1, so never more than four) and divides 4 damage among them
 * as it is put on the stack, the Kuldotha Flamefiend shape.
 */
val Fury = card("Fury") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 3
    oracleText = "Double strike\n" +
        "When this creature enters, it deals 4 damage divided as you choose among any number of " +
        "target creatures and/or planeswalkers.\n" +
        "Evoke—Exile a red card from your hand."

    keywords(Keyword.DOUBLE_STRIKE)

    evokeWith(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Any.withColor(Color.RED),
            fromZone = CostZone.HAND
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
            count = 4,
            minCount = 0,
            optional = true
        )
        effect = Effects.DividedDamage(total = 4, minTargets = 0, maxTargets = 4)
        description = "it deals 4 damage divided as you choose among any number of target creatures " +
            "and/or planeswalkers."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "126"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd281158-8180-40b9-a5b7-03cfc712d81a.jpg?1783926844"
        ruling(
            "2021-06-18",
            "If some of the targets are illegal as the triggered ability tries to resolve, the original " +
                "division of damage still applies, but the damage that would have been dealt to the " +
                "illegal targets isn't dealt at all."
        )
        ruling(
            "2021-06-18",
            "You divide the damage as you put the triggered ability on the stack, not as it resolves. " +
                "Each target must be assigned at least 1 damage. You can't choose more than four targets " +
                "and deal 0 damage to some of them."
        )
        ruling(
            "2021-06-18",
            "If you pay the evoke cost, you can have the creature's own triggered ability resolve before " +
                "the evoke triggered ability. You can cast spells after that ability resolves but before " +
                "you have to sacrifice the creature."
        )
    }
}
