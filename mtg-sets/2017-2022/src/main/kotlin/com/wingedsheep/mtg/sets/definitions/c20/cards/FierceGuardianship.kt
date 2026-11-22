package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fierce Guardianship — Commander 2020 #35 (canonical printing)
 * {2}{U} · Instant
 *
 * If you control a commander, you may cast this spell without paying its mana cost.
 * Counter target noncreature spell.
 *
 * "Without paying its mana cost" is a `{0}` [SelfAlternativeCost] gated on
 * [Conditions.YouControlACommander] — any player's commander counts. The gate is checked as the
 * cast is proposed, so removing the commander in response can't un-free it (ruling).
 */
val FierceGuardianship = card("Fierce Guardianship") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "If you control a commander, you may cast this spell without paying its mana cost.\n" +
        "Counter target noncreature spell."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.YouControlACommander
    )

    spell {
        target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Randy Vargas"
        flavorText = "\"Hunt somewhere else. This is your only warning.\"\n—Gavi, nest warden"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5ffa83-c88d-4f5d-851e-a642b229d596.jpg?1783931220"
        ruling(
            "2020-04-17",
            "It doesn't matter whose commander you control. Any one will do. If you have two commanders, " +
                "you just need to control one of them."
        )
        ruling(
            "2020-04-17",
            "Once you begin casting this spell, players can't take any other actions until you're done casting " +
                "it. Notably, they can't try to remove the commander you control to make you pay its cost."
        )
    }
}
