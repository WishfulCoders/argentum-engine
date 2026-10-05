package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Batterskull — New Phyrexia #130
 * {5} · Artifact — Equipment · Mythic
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +4/+4 and has vigilance and lifelink.
 * {3}: Return this Equipment to its owner's hand.
 * Equip {5}
 *
 * Living weapon (CR 702.92) is the create-then-attach enters trigger, as on Mandibular Kite. The
 * bounce is an instant-speed activated ability on the Equipment itself; once it leaves, the Germ is
 * a 0/0 and dies to state-based actions. No New Phyrexia Germ token exists on Scryfall, so the token
 * art is the most recent Phyrexian Germ printing.
 */
val Batterskull = card("Batterskull") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\n" +
        "Equipped creature gets +4/+4 and has vigilance and lifelink.\n" +
        "{3}: Return this Equipment to its owner's hand.\n" +
        "Equip {5}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Germ"),
            imageUri = "https://cards.scryfall.io/normal/front/5/3/53c05e53-84c7-4796-ae02-d778faa5afa6.jpg?1790977617"
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
        description = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian " +
            "Germ creature token, then attach this to it.)"
    }

    staticAbility {
        ability = ModifyStats(4, 4, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "{3}: Return this Equipment to its owner's hand."
    }

    equipAbility("{5}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "130"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd114ec3-d286-4c70-a122-3043bc53cc88.jpg?1783941296"

        ruling(
            "2020-08-07",
            "The ability to return Batterskull to its owner's hand can be activated only if Batterskull " +
                "is on the battlefield. If Batterskull is no longer on the battlefield when the ability " +
                "resolves, Batterskull remains in its new zone and isn't returned to its owner's hand."
        )
        ruling(
            "2020-08-07",
            "The Germ token enters the battlefield as a 0/0 creature and the Equipment becomes attached " +
                "to it before state-based actions would cause the token to die."
        )
    }
}
