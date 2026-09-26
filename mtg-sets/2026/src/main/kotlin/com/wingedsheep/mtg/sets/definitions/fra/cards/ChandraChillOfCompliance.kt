package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chandra, Chill of Compliance
 * {1}{U}{U}
 * Legendary Planeswalker — Chandra
 * Starting Loyalty: 3
 *
 * - The first +1 is a surveil that remembers what it put into the graveyard
 *   (`Patterns.Library.surveil(1, storeGraveyardAs = …)`, as Enlightened Confidant); the
 *   noncreature, nonland card among them moves on to the hand. The surveil still emits its
 *   `SurveiledEvent`, so "whenever you surveil" triggers see it.
 * - The second +1 is a loyalty ability, not a mana ability (loyalty abilities never are), so it
 *   uses the stack; its {U} carries the negated creature card-type restriction The Emperor of
 *   Palamecia uses for "spend this mana only to cast a noncreature spell".
 * - −X is Stall Out's tap-then-stun composite with the count read from the chosen X.
 * - −6 is a permanent emblem carrying a "whenever you cast a spell" trigger.
 */
val ChandraChillOfCompliance = card("Chandra, Chill of Compliance") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 3
    oracleText = "+1: Surveil 1. If you put a noncreature, nonland card into your graveyard this " +
        "way, put that card into your hand.\n" +
        "+1: Add {U}. Spend this mana only to cast a noncreature spell.\n" +
        "−X: Tap target artifact or creature. Put X stun counters on it.\n" +
        "−6: You get an emblem with \"Whenever you cast a spell, draw a card.\""

    // +1: Surveil 1. If you put a noncreature, nonland card into your graveyard this way, put
    //     that card into your hand.
    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val surveiledIntoGraveyard = runStoringCollection { Patterns.Library.surveil(1, storeGraveyardAs = it) }
            move(
                surveiledIntoGraveyard,
                CardDestination.ToZone(Zone.HAND),
                filter = GameObjectFilter.Noncreature.withCardPredicate(CardPredicate.IsNonland)
            )
        }
        description = "Surveil 1. If you put a noncreature, nonland card into your graveyard this " +
            "way, put that card into your hand."
    }

    // +1: Add {U}. Spend this mana only to cast a noncreature spell.
    loyaltyAbility(+1) {
        effect = Effects.AddMana(
            Color.BLUE,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.CREATURE,
                negated = true,
            ),
        )
        description = "Add {U}. Spend this mana only to cast a noncreature spell."
    }

    // −X: Tap target artifact or creature. Put X stun counters on it.
    loyaltyAbilityX {
        val t = target(TargetFilter(GameObjectFilter.CreatureOrArtifact))
        effect = Effects.Tap(t) then Effects.AddDynamicCounters(CounterType.STUN, DynamicAmounts.xValue(), t)
        description = "Tap target artifact or creature. Put X stun counters on it."
    }

    // −6: You get an emblem with "Whenever you cast a spell, draw a card."
    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.casts(),
                effect = Effects.DrawCards(1),
                descriptionOverride = "Whenever you cast a spell, draw a card.",
            ),
            descriptionOverride = "Whenever you cast a spell, draw a card.",
        )
        description = "You get an emblem with \"Whenever you cast a spell, draw a card.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/240f58ab-944c-4f4c-9df9-5f40b132bf3e.jpg?1788329242"
        inBooster = false
    }
}
