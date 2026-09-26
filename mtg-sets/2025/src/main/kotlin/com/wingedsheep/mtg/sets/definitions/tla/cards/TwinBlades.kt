package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Twin Blades
 * {2}{R}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature
 * gains double strike until end of turn.
 * Equipped creature gets +1/+1.
 * Equip {2}
 */
val TwinBlades = card("Twin Blades") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. That creature gains " +
        "double strike until end of turn.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        trigger = Triggers.self.enters()
        effect = Effects.AttachEquipment(creature) then
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature, Duration.EndOfTurn)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Jo Cordisco"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc399ae0-36b5-4c92-9fe2-138caf8d7a86.jpg?1764121078"
    }
}
