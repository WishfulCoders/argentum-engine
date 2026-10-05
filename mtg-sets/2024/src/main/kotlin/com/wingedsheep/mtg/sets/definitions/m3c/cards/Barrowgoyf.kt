package com.wingedsheep.mtg.sets.definitions.m3c.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Barrowgoyf — Modern Horizons 3 Commander #102
 * {2}{B} · Creature — Lhurgoyf · star / 1+star · Rare
 *
 * Deathtouch, lifelink
 * Barrowgoyf's power is equal to the number of card types among cards in all graveyards and its
 * toughness is equal to that number plus 1.
 * Whenever this creature deals combat damage to a player, you may mill that many cards. If you do,
 * you may put a creature card from among them into your hand.
 *
 * The P/T is Tarmogoyf's characteristic-defining ability verbatim (distinct card types across every
 * graveyard, toughness offset 1). The trigger mills the damage dealt
 * ([DynamicAmounts.triggerDamageAmount]) as a real mill (`isMill = true`, so mill-amount
 * replacements apply) under an optional [Effects.May]; the Ripples of Undeath shape then offers
 * "up to one" creature card among the cards that actually reached the graveyard.
 *
 * Canonical here: Barrowgoyf debuted in the MH3 Commander decks (M3C #50 is the extended-art
 * variant of #102), not in the MH3 main set.
 */
val Barrowgoyf = card("Barrowgoyf") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lhurgoyf"
    dynamicStats(
        DynamicAmounts.zone(
            Player.Each,
            Zone.GRAVEYARD,
        ).distinctTypes(),
        toughnessOffset = 1,
    )
    oracleText = "Deathtouch, lifelink\n" +
        "Barrowgoyf's power is equal to the number of card types among cards in all graveyards and its " +
        "toughness is equal to that number plus 1.\n" +
        "Whenever this creature deals combat damage to a player, you may mill that many cards. If you do, " +
        "you may put a creature card from among them into your hand."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(
            Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmounts.triggerDamageAmount(), isMill = true))
                val milled = moveTracked(top, CardDestination.ToZone(Zone.GRAVEYARD), name = "milled")
                val chosen = chooseUpTo(
                    1,
                    from = milled,
                    filter = GameObjectFilter.Creature,
                    showAllCards = true,
                    prompt = "You may put a creature card from among the milled cards into your hand",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Leave in graveyard"
                )
                toHand(chosen)
            }
        )
        description = "Whenever this creature deals combat damage to a player, you may mill that many cards. " +
            "If you do, you may put a creature card from among them into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cea3b218-0e6e-443a-84f7-380f1021e8e1.jpg?1783911410"

        ruling(
            "2024-06-07",
            "The ability that defines Barrowgoyf's power and toughness works in all zones, not just the battlefield."
        )
        ruling(
            "2024-06-07",
            "The ability that defines Barrowgoyf's power and toughness counts card types, not cards. If the only " +
                "card in all graveyards is a single artifact creature card, Barrowgoyf will be a 2/3. If the cards " +
                "in all graveyards are ten artifact cards and ten creature cards, Barrowgoyf will still be a 2/3."
        )
        ruling(
            "2024-06-07",
            "Card types that can appear on cards in a graveyard are artifact, battle, creature, enchantment, " +
                "instant, kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are supertypes, not " +
                "card types; Lhurgoyf, Forest, and Siege are subtypes, not card types."
        )
    }
}
