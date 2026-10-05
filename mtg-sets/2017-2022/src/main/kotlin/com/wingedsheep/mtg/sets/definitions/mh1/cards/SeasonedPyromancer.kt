package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

private const val ELEMENTAL_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/e/5/e5b57672-c346-42f5-ac3e-82466a13b957.jpg?1783933226"

/**
 * Seasoned Pyromancer
 * {1}{R}{R}
 * Creature — Human Shaman
 * 2/2
 * When this creature enters, discard two cards, then draw two cards. For each nonland card
 * discarded this way, create a 1/1 red Elemental creature token.
 * {3}{R}{R}, Exile this card from your graveyard: Create two 1/1 red Elemental creature tokens.
 *
 * The ETB is a Gather → Select → Discard pipeline over your hand: choose exactly two (with fewer in
 * hand you discard what you have, and still draw two — ruling 2019-06-14), draw a flat two, then
 * filter the discarded collection to nonland cards and create that many tokens. The graveyard
 * ability is an instant-speed `activateFromZone = GRAVEYARD` ability paying [Costs.ExileSelf].
 */
val SeasonedPyromancer = card("Seasoned Pyromancer") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "When this creature enters, discard two cards, then draw two cards. For each nonland card " +
        "discarded this way, create a 1/1 red Elemental creature token.\n" +
        "{3}{R}{R}, Exile this card from your graveyard: Create two 1/1 red Elemental creature tokens."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseExactly(2, from = hand, prompt = "Choose two cards to discard")
            discard(discarded)
            run(Effects.DrawCards(2))
            val nonland = filter(discarded, GameObjectFilter.Nonland)
            run(
                Effects.CreateToken(
                    count = nonland.count,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Elemental"),
                    imageUri = ELEMENTAL_TOKEN_IMAGE,
                ),
            )
        }
        description = "When this creature enters, discard two cards, then draw two cards. For each nonland " +
            "card discarded this way, create a 1/1 red Elemental creature token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}{R}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            imageUri = ELEMENTAL_TOKEN_IMAGE,
        )
        activateFromZone = Zone.GRAVEYARD
        description = "{3}{R}{R}, Exile this card from your graveyard: Create two 1/1 red Elemental creature tokens."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e139ad1-1079-49e9-babd-6399c44ad333.jpg?1783933105"
        ruling("2019-06-14", "If you have fewer than two cards in hand as Seasoned Pyromancer's first ability resolves, you'll discard your hand, then draw two cards regardless of how many you discarded.")
    }
}
