package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rites of Flourishing
 * {2}{G}
 * Enchantment
 * At the beginning of each player's draw step, that player draws an additional card.
 * Each player may play an additional land on each of their turns.
 *
 * The draw is a triggered ability on every player's draw step (the Dictate of Kruphix shape),
 * resolving after the turn's normal draw. The land drop is a symmetric
 * [GrantAdditionalLandDrop] — `affected = Player.Each` grants it to every player, not just the
 * enchantment's controller.
 */
val RitesOfFlourishing = card("Rites of Flourishing") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's draw step, that player draws an additional card.\n" +
        "Each player may play an additional land on each of their turns."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    staticAbility {
        ability = GrantAdditionalLandDrop(affected = Player.Each)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Brandon Kitkouski"
        flavorText = "\"Dance, and bring forth the coil! It is an umbilical to Gaea herself, fattening us with the earth's rich bounty.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/1/811458c7-dcdc-43ef-8c3e-a90e21ce315e.jpg?1783943097"
        ruling("2013-04-15", "The triggered ability is put onto the stack after you have already drawn your card for the turn.")
    }
}
