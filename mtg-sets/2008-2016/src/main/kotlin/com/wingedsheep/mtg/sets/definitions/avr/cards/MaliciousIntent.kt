package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Malicious Intent
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{T}: Target creature can't block this turn."
 *
 * The Lavamancer's Skill shape: [GrantActivatedAbility] hands the quoted ability to the enchanted
 * creature (the static's filter defaults to the attached creature), so the {T} is the *creature's*
 * tap and only its controller can activate it. Enchanting an opponent's creature therefore gives
 * them the ability, which is the printed reading — the Aura's usual home is a creature of your own
 * that has already attacked, or one you can tap for free.
 */
val MaliciousIntent = card("Malicious Intent") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{T}: Target creature can't block this turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.CantBlock(creature)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Kev Walker"
        flavorText = "As peace returned to Thraben, some cathars made the mistake of letting down their guard."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1dda42d-55eb-46fc-89da-17cc5bfaa823.jpg?1783940679"
    }
}
