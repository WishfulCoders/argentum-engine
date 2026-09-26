package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Bucolic Ranch
 * Land — Desert
 *
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a Mount spell.
 * {3}, {T}: Look at the top card of your library. If it's a Mount card, you may reveal it and put
 * it into your hand. If you don't put it into your hand, you may put it on the bottom of your
 * library.
 *
 * The colored mana ability is restricted to casting Mount spells via
 * [ManaRestriction.SubtypeSpellsOnly] (same shape as Intrepid Stablemaster). The look ability is a
 * Gather→Select→Move pipeline: top card is gathered, [SelectionMode.ChooseUpTo] gates the optional
 * "reveal & put into hand" on the Mount-card filter (so non-Mounts can't be taken), and the
 * remainder is offered to the bottom of the library. Both stay (hand) and bottom are "may", so the
 * card can also be left on top by declining both choices.
 */
val BucolicRanch = card("Bucolic Ranch") {
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a Mount spell.\n" +
        "{3}, {T}: Look at the top card of your library. If it's a Mount card, you may reveal it " +
        "and put it into your hand. If you don't put it into your hand, you may put it on the " +
        "bottom of your library."

    // {T}: Add {C}.
    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {T}: Add one mana of any color. Spend this mana only to cast a Mount spell.
    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(
            amount = 1,
            restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Mount"))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {3}, {T}: Look at the top card of your library. If it's a Mount card, you may reveal it and
    // put it into your hand. If you don't put it into your hand, you may put it on the bottom of
    // your library.
    activatedAbility {
        cost = AbilityCost.Composite(listOf(Costs.Mana("{3}"), AbilityCost.Tap))
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            // If it's a Mount card, you may reveal it and put it into your hand.
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.HasSubtype(Subtype("Mount")))
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Leave"
            )
            toHand(kept, revealed = true)
            // If you don't put it into your hand, you may put it on the bottom of your library
            // (otherwise it stays on top — unselected cards are left in place).
            val toBottom = chooseUpTo(
                1,
                from = rest,
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c4f6b81-53d0-49fb-b404-c2ad67de7493.jpg?1712356362"
    }
}
