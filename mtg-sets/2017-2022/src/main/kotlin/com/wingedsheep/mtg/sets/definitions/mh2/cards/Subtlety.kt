package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
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
 * Subtlety — Modern Horizons 2 #67
 * {2}{U}{U} · Creature — Elemental Incarnation · 3 / 3
 *
 * Flash
 * Flying
 * When this creature enters, choose up to one target creature spell or planeswalker spell. Its
 * owner puts it on their choice of the top or bottom of their library.
 * Evoke—Exile a blue card from your hand.
 *
 * The evoke cost is non-mana only ([evokeWith] an exile-a-blue-card-from-hand cost); the evoke
 * sacrifice trigger is the engine's. The enters trigger targets a *spell* (a creature or
 * planeswalker spell on the stack — never a permanent) and moves it with Swat Away's
 * owner-chooses-top-or-bottom effect. That is not a counter, so it works on a spell that can't be
 * countered (the 2021-06-18 ruling).
 */
val Subtlety = card("Subtlety") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, choose up to one target creature spell or planeswalker spell. " +
        "Its owner puts it on their choice of the top or bottom of their library.\n" +
        "Evoke—Exile a blue card from your hand."

    keywords(Keyword.FLASH, Keyword.FLYING)

    evokeWith(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Any.withColor(Color.BLUE),
            fromZone = CostZone.HAND
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val spell = target(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.STACK),
            optional = true
        )
        effect = Effects.PutOnTopOrBottomOfLibrary(spell)
        description = "choose up to one target creature spell or planeswalker spell. Its owner puts it " +
            "on their choice of the top or bottom of their library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/701256d5-1389-48b7-9581-d6037209bd06.jpg?1783926869"
        ruling("2021-06-18", "Subtlety's triggered ability targets a spell on the stack. It can't target creatures or planeswalkers on the battlefield.")
        ruling("2021-06-18", "A spell that's put into its owner's library doesn't resolve, but it isn't countered. This will work on spells that say they can't be countered.")
        ruling("2021-06-18", "The owner of the spell is the one who chooses whether it goes on the top or bottom of their library. All players will know this information.")
        ruling(
            "2021-06-18",
            "If you pay the evoke cost, you can have the creature's own triggered ability resolve before " +
                "the evoke triggered ability. You can cast spells after that ability resolves but before " +
                "you have to sacrifice the creature."
        )
    }
}
