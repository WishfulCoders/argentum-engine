package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Broadside Bombardiers
 * {2}{R}
 * Creature — Goblin Pirate
 * 2/2
 * Menace, haste
 * Boast — Sacrifice another creature or artifact: This creature deals damage equal to 2 plus the
 * sacrificed permanent's mana value to any target. (Activate only if this creature attacked this
 * turn and only once each turn.)
 *
 * `isBoast = true` installs both boast clauses (attacked this turn, once each turn). The amount
 * reads the sacrificed permanent's mana value through last-known information
 * ([DynamicAmounts.sacrificedManaValue]); a transformed double-faced permanent uses its front
 * face's mana value (ruling 2023-11-10), which is what the engine's mana value already reports.
 */
val BroadsideBombardiers = card("Broadside Bombardiers") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    oracleText = "Menace, haste\nBoast — Sacrifice another creature or artifact: This creature deals " +
        "damage equal to 2 plus the sacrificed permanent's mana value to any target. (Activate only " +
        "if this creature attacked this turn and only once each turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.MENACE, Keyword.HASTE)

    activatedAbility {
        isBoast = true
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature or GameObjectFilter.Artifact)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2 + DynamicAmounts.sacrificedManaValue(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Tomek Larek"
        flavorText = "When they ran short on cannonballs, they went for the pots and pans."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9df172-9fdb-4b0c-a23a-865b83c8fa40.jpg?1783913918"
        ruling(
            "2023-11-10",
            "A boast ability can be activated at any point after the creature with that ability has been " +
                "declared as an attacker, including during the postcombat main phase and the end step.",
        )
        ruling(
            "2023-11-10",
            "If a creature with a boast ability is put onto the battlefield attacking, it was never declared " +
                "as an attacker. Its boast ability can't be activated that turn.",
        )
        ruling(
            "2023-11-10",
            "The back face of a double-faced card doesn't have a mana cost. A double-faced permanent with its " +
                "back face up has a mana value equal to the mana value of its front face.",
        )
    }
}
