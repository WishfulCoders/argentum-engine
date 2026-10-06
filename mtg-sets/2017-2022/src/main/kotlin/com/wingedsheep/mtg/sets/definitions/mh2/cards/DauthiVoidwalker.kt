package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dauthi Voidwalker
 * {B}{B}
 * Creature — Dauthi Rogue
 * 3/2
 *
 * Shadow (This creature can block or be blocked by only creatures with shadow.)
 * If a card would be put into an opponent's graveyard from anywhere, instead exile it with a void
 * counter on it.
 * {T}, Sacrifice this creature: Choose an exiled card an opponent owns with a void counter on it.
 * You may play it this turn without paying its mana cost.
 *
 * Implementation notes:
 * - The replacement is a [RedirectZoneChangeWith] whose rider stamps the void counter on the card
 *   as it lands in exile — the modified event is "exile it with a void counter" (CR 614.6). It
 *   watches every route into a graveyard: a creature dying, a discard, a mill, a resolving spell,
 *   and a countered or fizzled spell. A card goes to its owner's graveyard, so "an opponent's
 *   graveyard" is `ownedByOpponent()`; `nontoken()` is "a card", which is why tokens still die
 *   (and their dies triggers still fire) while opponents' nontoken creatures are exiled instead
 *   and never "die" at all.
 * - The sacrifice ability chooses on resolution (it does not target) among every void-countered
 *   card in an opponent's exile — not only ones this Voidwalker exiled — and grants a this-turn
 *   permission to play it without paying its mana cost. Normal timing still applies (a land only
 *   as your land drop in a main phase, a sorcery-speed spell only then), X is 0, and other
 *   alternative costs are unavailable, per the rulings.
 */
val DauthiVoidwalker = card("Dauthi Voidwalker") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dauthi Rogue"
    power = 3
    toughness = 2
    oracleText = "Shadow (This creature can block or be blocked by only creatures with shadow.)\n" +
        "If a card would be put into an opponent's graveyard from anywhere, instead exile it with a " +
        "void counter on it.\n" +
        "{T}, Sacrifice this creature: Choose an exiled card an opponent owns with a void counter on " +
        "it. You may play it this turn without paying its mana cost."

    keywords(Keyword.SHADOW)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.AddCounters(CounterType.VOID, 1, EffectTarget.TriggeringEntity),
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any.nontoken().ownedByOpponent(),
                to = Zone.GRAVEYARD,
            ),
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val voided = gather(
                CardSource.FromZone(
                    zone = Zone.EXILE,
                    player = Player.EachOpponent,
                    filter = GameObjectFilter.Any.withCounter(CounterType.VOID),
                )
            )
            val chosen = chooseExactly(
                1,
                from = voided,
                prompt = "Choose an exiled card with a void counter to play this turn for free",
            )
            run(Effects.GrantMayPlayFromExile(chosen))
            run(Effects.GrantPlayWithoutPayingCost(chosen))
        }
        description = "{T}, Sacrifice this creature: Choose an exiled card an opponent owns with a " +
            "void counter on it. You may play it this turn without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dce5db87-4a78-4b8d-b5c2-918ccd1ba4e3.jpg?1783926864"

        ruling("2021-06-18", "If your opponent discards a card while you control Dauthi Voidwalker, " +
            "abilities that function when that card is discarded still work, even though that card " +
            "never reaches that player's graveyard. In addition, spells or abilities that check the " +
            "characteristics of the discarded card can find that card in exile.")
        ruling("2021-06-18", "While Dauthi Voidwalker is on the battlefield, nontoken creatures your " +
            "opponents control won't die. They'll be exiled instead. Abilities that would trigger " +
            "when those creatures die won't trigger.")
        ruling("2021-06-18", "Tokens still die while Dauthi Voidwalker is on the battlefield.")
        ruling("2021-06-18", "If a card has {X} in its mana cost, you must choose 0 as the value of X " +
            "when casting it without paying its mana cost.")
        ruling("2021-06-18", "Playing a card with Dauthi Voidwalker's last ability is still subject " +
            "to normal timing restrictions.")
        ruling("2021-06-18", "If you cast a card this way, you may not cast it for any other " +
            "alternative costs it has, but you may pay for additional costs, such as kicker costs. " +
            "If the spell requires an additional cost, you must pay that cost.")
    }
}
