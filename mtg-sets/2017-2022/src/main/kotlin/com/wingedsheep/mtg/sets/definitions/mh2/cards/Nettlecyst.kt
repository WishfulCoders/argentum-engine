package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nettlecyst — Modern Horizons 2 #231
 * {3} · Artifact — Equipment · Rare
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +1/+1 for each artifact and/or enchantment you control.
 * Equip {2}
 *
 * Living weapon is the Mandibular Kite enters trigger. The bonus is the Nightmare Lash
 * [GrantDynamicStats] shape over the single-predicate [GameObjectFilter.ArtifactOrEnchantment], so
 * an artifact enchantment counts once (ruling) and Nettlecyst counts itself.
 */
val Nettlecyst = card("Nettlecyst") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\n" +
        "Equipped creature gets +1/+1 for each artifact and/or enchantment you control.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Germ"),
            imageUri = "https://cards.scryfall.io/normal/front/b/5/b53e0681-603e-4180-bc86-3dadf214e61a.jpg?1783926593"
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
        description = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian " +
            "Germ creature token, then attach this to it.)"
    }

    staticAbility {
        val artifactsAndEnchantments = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.ArtifactOrEnchantment
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = artifactsAndEnchantments,
            toughnessBonus = artifactsAndEnchantments
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a0bb5dc-75a6-4bd6-81f8-611197fb0fba.jpg?1783926803"

        ruling(
            "2021-06-18",
            "If a permanent you control is both an artifact and an enchantment, count it only once when " +
                "determining the bonus from an equipped Nettlecyst."
        )
    }
}
