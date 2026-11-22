package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deflecting Swat — Commander 2020 #50 (canonical printing)
 * {2}{R} · Instant
 *
 * If you control a commander, you may cast this spell without paying its mana cost.
 * You may choose new targets for target spell or ability.
 *
 * The free cast is Fierce Guardianship's `{0}` [SelfAlternativeCost] gated on
 * [Conditions.YouControlACommander]. The retarget is [Effects.ChangeTriggeringObjectTargets]
 * pointed at this spell's own target — the all-slots retarget (Sideswipe), so "new targets" covers
 * every target of a multi-target spell or ability, slot by slot, each kept or swapped for another
 * legal target. The target count is never changed and a divided split stays as cast (rulings).
 */
val DeflectingSwat = card("Deflecting Swat") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "If you control a commander, you may cast this spell without paying its mana cost.\n" +
        "You may choose new targets for target spell or ability."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.YouControlACommander
    )

    spell {
        val spellOrAbility = target(TargetFilter.SpellOrAbilityOnStack)
        effect = Effects.ChangeTriggeringObjectTargets(spell = spellOrAbility)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Izzy"
        flavorText = "Archmage attacks are little more than annoyances to Kalamax."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84f035e1-6c89-457b-b05f-85680a50ed91.jpg?1783931214"
        ruling("2020-04-17", "If the target spell has a variable number of targets, you can't change how many targets it has.")
        ruling("2020-04-17", "It doesn't matter whose commander you control. Any one will do. If you have two commanders, you just need to control one of them.")
        ruling("2020-04-17", "Once you begin casting this spell, players can't take any other actions until you're done casting it. Notably, they can't try to remove the commander you control to make you pay its cost.")
        ruling("2020-04-17", "If the target spell has damage divided as it was cast (like Mythos of Vadrok), the division can't be changed although the targets receiving that damage still can. The same is true of spells that distribute counters.")
        ruling("2020-04-17", "If you choose new targets for the target spell, the new targets must be legal.")
    }
}
