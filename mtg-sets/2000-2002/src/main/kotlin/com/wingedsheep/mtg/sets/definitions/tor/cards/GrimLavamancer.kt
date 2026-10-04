package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grim Lavamancer
 * {R}
 * Creature — Human Wizard
 * 1/1
 * {R}, {T}, Exile two cards from your graveyard: This creature deals 2 damage to any target.
 *
 * A plain activated ability whose cost composes mana, tap, and [Costs.ExileFromGraveyard] for
 * exactly two cards; the cards are exiled as the cost is paid, so they can't be moved in response
 * (ruling 2022-12-08). The tap symbol in the cost means summoning sickness applies.
 */
val GrimLavamancer = card("Grim Lavamancer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "{R}, {T}, Exile two cards from your graveyard: This creature deals 2 damage to any target."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.ExileFromGraveyard(2))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Jim Nelson"
        flavorText = "\"Fools dig for water, corpses, or gold. The earth's real treasure is far deeper.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dd72697-24be-42c7-a6d9-a837bdbd4662.jpg?1783945148"
        ruling("2022-12-08", "The two cards are exiled as the cost of Grim Lavamancer's ability is paid. Players can't respond to the paying of costs by trying to move those cards to another zone.")
    }
}
