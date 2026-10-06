package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hazoret the Fervent
 * {3}{R}
 * Legendary Creature — God
 * 5/4
 *
 * Indestructible, haste
 * Hazoret can't attack or block unless you have one or fewer cards in hand.
 * {2}{R}, Discard a card: Hazoret deals 2 damage to each opponent.
 *
 * The restriction is checked only when attackers / blockers are declared (CR 508.1c / 509.1b),
 * so Hazoret stays in combat if your hand grows afterwards (2017-04-18 ruling).
 */
val HazoretTheFervent = card("Hazoret the Fervent") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 4
    oracleText = "Indestructible, haste\n" +
        "Hazoret can't attack or block unless you have one or fewer cards in hand.\n" +
        "{2}{R}, Discard a card: Hazoret deals 2 damage to each opponent."

    keywords(Keyword.INDESTRUCTIBLE, Keyword.HASTE)

    staticAbility {
        ability = CantAttackUnless(Conditions.CardsInHandAtMost(1))
    }
    staticAbility {
        ability = CantBlockUnless(Conditions.CardsInHandAtMost(1))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardCard)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{2}{R}, Discard a card: Hazoret deals 2 damage to each opponent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "136"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36ed9f9d-99a7-49f4-b0ad-d71355809d32.jpg?1783936487"

        ruling("2017-04-18", "Once Hazoret has attacked or blocked, it will remain in combat even if the number of cards in your hand becomes two or greater.")
    }
}
