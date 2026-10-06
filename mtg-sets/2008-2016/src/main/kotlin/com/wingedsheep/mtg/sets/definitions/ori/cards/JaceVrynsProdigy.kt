package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Vryn's Prodigy // Jace, Telepath Unbound — Magic Origins #60
 * {1}{U} · Legendary Creature — Human Wizard 0/2 // Legendary Planeswalker — Jace (loyalty 5)
 *
 * Modeling notes:
 *  - Front: the loot is a plain draw-then-discard; the graveyard count is read *after* the discard
 *    (ruling), so it is an [Effects.If] on resolution — not an intervening "if" — and a madness card
 *    exiled instead of discarded doesn't count. The flip is [Effects.ExileAndReturnTransformed]: a
 *    new object enters back face up with its printed loyalty (CR 306.5b; ruling). If Jace has left
 *    the battlefield by then, the exile-and-return does nothing (CR 400.7) — the draw and discard
 *    still happen.
 *  - +1: "up to one target creature" is an optional target slot, so it may be activated with no
 *    target; the -2/-0 lasts [Duration.UntilYourNextTurn].
 *  - −3: a may-cast permission on the targeted graveyard card for this turn
 *    ([MayPlayExpiry.EndOfTurn]), paying its costs and following its normal timing (ruling), plus
 *    the [AfterResolveDestination.EXILE] rider for "if that spell would be put into your graveyard,
 *    exile it instead" — honoured on resolution, counter and fizzle. The rider is stamped on that
 *    card and is dropped if it changes zones any other way than being cast (CR 400.7), so a card
 *    that goes to a hidden zone and back is not exiled later (ruling). If it isn't cast this turn,
 *    nothing happens and it stays in the graveyard.
 *  - −9: the emblem is a permanent global triggered ability ([Effects.CreateGlobalTriggeredAbility]);
 *    its trigger targets an opponent as it is put on the stack and resolves above the spell.
 */
private val JaceVrynsProdigyFront = card("Jace, Vryn's Prodigy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 0
    toughness = 2
    oracleText = "{T}: Draw a card, then discard a card. If there are five or more cards in your " +
        "graveyard, exile Jace, then return him to the battlefield transformed under his owner's " +
        "control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1) then
            Effects.If(
                Conditions.CardsInGraveyardAtLeast(5),
                then = Effects.ExileAndReturnTransformed(EffectTarget.Self)
            )
        description = "Draw a card, then discard a card. If there are five or more cards in your " +
            "graveyard, exile Jace, then return him to the battlefield transformed under his " +
            "owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Jaime Jones"
        flavorText = "\"People's thoughts just come to me. Sometimes I don't know if it's them or me thinking.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02d6d693-f1f3-4317-bcc0-c21fa8490d38.jpg?1783938353"

        ruling("2016-04-08", "If you discard a card with madness while resolving the ability of Jace, Vryn's Prodigy, you'll need to already have five other cards in your graveyard to satisfy that ability's condition. You can't choose to put the card directly into your graveyard to satisfy it.")
        ruling("2015-06-22", "The activated ability of Jace, Vryn's Prodigy checks to see if there are five or more cards in your graveyard after you discard a card. Putting a fifth card into your graveyard at other times won't cause Jace to be exiled, nor will Jace entering the battlefield while there are five or more cards in your graveyard.")
        ruling("2015-06-22", "In some rare cases, a spell or ability may cause one of these five cards to transform while it's a creature (front face up) on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
        ruling("2015-06-22", "A Magic Origins planeswalker that enters the battlefield because of the ability of its front face will enter with loyalty counters as normal.")
    }
}

private val JaceTelepathUnbound = card("Jace, Telepath Unbound") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 5
    oracleText = "+1: Up to one target creature gets -2/-0 until your next turn.\n" +
        "−3: You may cast target instant or sorcery card from your graveyard this turn. If that " +
        "spell would be put into your graveyard, exile it instead.\n" +
        "−9: You get an emblem with \"Whenever you cast a spell, target opponent mills five cards.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-2, 0, creature, Duration.UntilYourNextTurn)
        description = "Up to one target creature gets -2/-0 until your next turn."
    }

    loyaltyAbility(-3) {
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = card,
                expiry = MayPlayExpiry.EndOfTurn,
                insteadOfGraveyard = AfterResolveDestination.EXILE
            ))
        }
        description = "You may cast target instant or sorcery card from your graveyard this turn. " +
            "If that spell would be put into your graveyard, exile it instead."
    }

    loyaltyAbility(-9) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts()
                val opponent = target(Targets.Opponent)
                effect = Patterns.Library.mill(5, opponent)
                description = "Whenever you cast a spell, target opponent mills five cards."
            },
            descriptionOverride = "Whenever you cast a spell, target opponent mills five cards."
        )
        description = "You get an emblem with \"Whenever you cast a spell, target opponent mills " +
            "five cards.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/back/0/2/02d6d693-f1f3-4317-bcc0-c21fa8490d38.jpg?1783938353"

        ruling("2015-06-22", "If you activate the second ability of Jace, Telepath Unbound, you must follow the timing rules for the card's types. For example, if you target a sorcery card, you may cast it during your main phase when the stack is empty. You pay all the spell's costs.")
        ruling("2015-06-22", "If you don't cast the card that turn, nothing happens. It remains in your graveyard.")
        ruling("2015-06-22", "The card is exiled only if it's cast from the graveyard and put back into the graveyard (either by resolving or being countered). If, at any time, the card goes to a hidden zone (such as your hand or your library), the effect loses track of the card. It won't be exiled, even if that card is put into your graveyard later that turn.")
        ruling("2015-06-22", "You can activate one of the planeswalker's loyalty abilities the turn it enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if the planeswalker enters the battlefield during combat, there will be an opportunity for your opponent to remove it before you can activate one of its abilities.")
    }
}

val JaceVrynsProdigy: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = JaceVrynsProdigyFront,
    backFace = JaceTelepathUnbound,
)
