package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Seasoned Dungeoneer
 * {3}{W}
 * Creature — Human Warrior
 * 3/4
 * When this creature enters, you take the initiative.
 * Whenever you attack, target attacking Cleric, Rogue, Warrior, or Wizard gains protection from
 * creatures until end of turn. It explores.
 */
val SeasonedDungeoneer = card("Seasoned Dungeoneer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you take the initiative.\n" +
        "Whenever you attack, target attacking Cleric, Rogue, Warrior, or Wizard gains protection from " +
        "creatures until end of turn. It explores. (Reveal the top card of your library. Put that card " +
        "into your hand if it's a land. Otherwise, put a +1/+1 counter on the creature, then put the " +
        "card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val adventurer = target(
            TargetFilter(
                GameObjectFilter.Creature.attacking().withAnySubtype("Cleric", "Rogue", "Warrior", "Wizard")
            )
        )
        effect = Effects.GrantProtectionFromCardType(CardType.CREATURE, adventurer) then
            Effects.Explore(adventurer)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "660"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fee3f76-20a8-4621-84fb-ddf79c955532.jpg?1783922514"
        ruling("2022-06-10", "A player who currently has the initiative may take the initiative again. This causes that player to venture into Undercity again, but does not cause them to have multiple initiative designations.")
        ruling("2022-06-10", "If the player with the initiative leaves the game, the active player takes the initiative at the same time that player leaves the game. If the active player is leaving the game or if there is no active player, the next player in turn order takes the initiative.")
    }
}
