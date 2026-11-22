package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jodah, the Unifier
 * {W}{U}{B}{R}{G}
 * Legendary Creature — Human Wizard
 * 5/5
 * Legendary creatures you control get +X/+X, where X is the number of legendary creatures you
 * control.
 * Whenever you cast a legendary spell from your hand, exile cards from the top of your library
 * until you exile a legendary nonland card with lesser mana value. You may cast that card without
 * paying its mana cost. Put the rest on the bottom of your library in a random order.
 *
 * - The anthem is a [GrantDynamicStats] over legendary creatures you control (Jodah included),
 *   counting the same group.
 * - The trigger is a legendary cascade: [Effects.Cascade] is fixed to "nonland", so it is spelled
 *   out as a pipeline. "Lesser mana value" is a strict `<`, written as the triggering spell's mana
 *   value minus one (Kethek's idiom); that mana value is the cast event's, so an X spell counts its
 *   chosen X (ruling). Every revealed card is exiled; the found card may be cast for free from
 *   exile right away, and if it isn't it stays in exile (ruling). Everything else revealed —
 *   the whole library if nothing qualified — goes to the bottom in a random order.
 */
val JodahTheUnifier = card("Jodah, the Unifier") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Wizard"
    power = 5
    toughness = 5
    oracleText = "Legendary creatures you control get +X/+X, where X is the number of legendary " +
        "creatures you control.\n" +
        "Whenever you cast a legendary spell from your hand, exile cards from the top of your " +
        "library until you exile a legendary nonland card with lesser mana value. You may cast " +
        "that card without paying its mana cost. Put the rest on the bottom of your library in a " +
        "random order."

    staticAbility {
        val legendaryCount = DynamicAmounts.legendaryCreaturesYouControl()
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.legendary().youControl()),
            powerBonus = legendaryCount,
            toughnessBonus = legendaryCount,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.legendary(),
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        effect = Effects.Pipeline {
            val lesserLegend = GameObjectFilter.Nonland.legendary().manaValueAtMostDynamic(
                DynamicAmounts.triggeringSpellManaValue() - 1
            )
            val (found, revealed) = gatherUntilMatch(lesserLegend)
            exile(revealed)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
            toLibraryBottom(exclude(revealed, found), order = CardOrder.Random)
        }
        description = "Whenever you cast a legendary spell from your hand, exile cards from the top " +
            "of your library until you exile a legendary nonland card with lesser mana value. You " +
            "may cast that card without paying its mana cost. Put the rest on the bottom of your " +
            "library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "203"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4b1aa1e-b4e3-4346-8937-76b312501c70.jpg?1783921283"

        ruling("2022-09-09", "Both Jodah, the Unifier's triggered ability and the spell cast with that ability resolve before the spell that caused the ability to trigger, even if the original spell was countered in response.")
        ruling("2022-09-09", "The legendary nonland card exiled may be cast immediately. If you do not cast it immediately, you don't get to cast it at a later time.")
        ruling("2022-09-09", "If you choose not to cast the card, it remains in exile. The rest of the cards will be put on the bottom of your library in a random order.")
        ruling("2022-09-09", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the spell.")
        ruling("2022-09-09", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2022-09-09", "The mana value of the spell that caused the ability to trigger is determined only by its mana cost, no matter what the total cost to cast the spell was. If that spell had an {X} in its cost, include the chosen value for X when determining its mana value.")
        ruling("2022-09-09", "If you exile your entire library without exiling a legendary nonland card with lesser mana value, you will randomize the exiled cards and they again become your library, ending the effect. You will not continue to exile and shuffle your library forever.")
    }
}
