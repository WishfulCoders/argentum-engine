package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thunder Lasso
 * {2}{W}
 * Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, tap target creature defending player controls.
 * Equip {2}
 *
 * The attack trigger binds to the attached (equipped) creature
 * ([TriggerBinding.ATTACHED]); "creature defending player controls" is the opponent's
 * creature during your attack, matching Heart-Piercer Bow's "defending player controls".
 */
val ThunderLasso = card("Thunder Lasso") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, tap target creature defending player controls.\n" +
        "Equip {2}"

    // ETB: attach to target creature you control
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    // Equipped creature gets +1/+1
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // Whenever equipped creature attacks, tap target creature defending player controls
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Camille Alquier"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73dff5fc-2adf-447a-b35f-e7883e0fd821.jpg?1712355369"
    }
}
