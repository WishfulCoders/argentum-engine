package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Sword of Fire and Ice — Darksteel #148
 * {3} · Artifact — Equipment · Rare
 *
 * Equipped creature gets +2/+2 and has protection from red and from blue.
 * Whenever equipped creature deals combat damage to a player, this Equipment deals 2 damage to any
 * target and you draw a card.
 * Equip {2}
 *
 * The Sword of Forge and Frontier shape: one [ModifyStats] and one [GrantProtection] per colour over
 * [Filters.EquippedCreature], and a [Triggers.attached] combat-damage-to-a-player trigger. The
 * trigger targets on the stack; the damage source is the Equipment (the ability's source), not the
 * creature. Per the 2020 ruling, an illegal target on resolution fizzles the whole ability, so the
 * draw is sequenced inside the same targeted ability rather than split off.
 */
val SwordOfFireAndIce = card("Sword of Fire and Ice") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has protection from red and from blue.\n" +
        "Whenever equipped creature deals combat damage to a player, this Equipment deals 2 damage to any target and you draw a card.\n" +
        "Equip {2}"

    // Equipped creature gets +2/+2 ...
    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }
    // ... and has protection from red and from blue.
    staticAbility {
        ability = GrantProtection(Color.RED, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantProtection(Color.BLUE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t) then Effects.DrawCards(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eb613ee-8b8b-4a34-886c-6592b27672b3.jpg?1783944417"

        ruling(
            "2020-08-07",
            "If the chosen target is an illegal target by the time the triggered ability tries to " +
                "resolve, the ability won't resolve. You won't draw a card."
        )
    }
}
