package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaldra Compleat — Modern Horizons 2 #227
 * {7} · Legendary Artifact — Equipment · Mythic
 *
 * Living weapon
 * Indestructible
 * Equipped creature gets +5/+5 and has first strike, trample, indestructible, haste, and
 * "Whenever this creature deals combat damage to a creature, exile that creature."
 * Equip {7}
 *
 * Living weapon (CR 702.92) is the create-then-attach enters trigger, as on Mandibular Kite. The
 * quoted trigger is granted to the equipped creature ([GrantTriggeredAbility], read by the trigger
 * detector rather than the layer system), so "this creature" is the creature that received it;
 * "that creature" is the damaged creature, the Voracious Cobra shape.
 */
val KaldraCompleat = card("Kaldra Compleat") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Living weapon\n" +
        "Indestructible\n" +
        "Equipped creature gets +5/+5 and has first strike, trample, indestructible, haste, and " +
        "\"Whenever this creature deals combat damage to a creature, exile that creature.\"\n" +
        "Equip {7}"

    keywords(Keyword.INDESTRUCTIBLE)

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
        ability = ModifyStats(5, 5, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature),
                effect = Effects.Exile(EffectTarget.TriggeringEntity),
                descriptionOverride = "Whenever this creature deals combat damage to a creature, " +
                    "exile that creature."
            )
        )
    }

    equipAbility("{7}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87cc2855-6b14-44dd-a398-7dc2bbae081f.jpg?1783926807"
    }
}
