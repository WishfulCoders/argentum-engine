package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.core.Step

/**
 * Ashling, Rekindled // Ashling, Rimebound
 * {1}{R}
 * Legendary Creature — Elemental Sorcerer // Legendary Creature — Elemental Wizard (transform)
 * 1/3 // 1/3
 *
 * Front — Ashling, Rekindled
 *   Whenever this creature enters or transforms into Ashling, Rekindled, you may discard a card.
 *   If you do, draw a card.
 *   At the beginning of your first main phase, you may pay {U}. If you do, transform Ashling.
 *
 * Back — Ashling, Rimebound
 *   Whenever this creature transforms into Ashling, Rimebound and at the beginning of your first
 *   main phase, add two mana of any one color. Spend this mana only to cast spells with mana
 *   value 4 or greater.
 *   At the beginning of your first main phase, you may pay {R}. If you do, transform Ashling.
 */

private val rummageMay = Effects.May(
    effect = Patterns.Hand.rummage(1),
    descriptionOverride = "You may discard a card. If you do, draw a card."
)

private val addRimeboundMana = Effects.ChooseColorForTarget(
    target = EffectTarget.Self,
    prompt = "Choose a color for Ashling's mana"
) then
    Effects.AddManaOfChoice(
        colorSet = ManaColorSet.SourceChosenColor,
        amount = 2,
        restriction = ManaRestriction.SpellsWithManaValueAtLeast(4)
    )

private val AshlingRimebound = card("Ashling, Rimebound") {
    manaCost = ""
    typeLine = "Legendary Creature — Elemental Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature transforms into Ashling, Rimebound and at the beginning " +
        "of your first main phase, add two mana of any one color. Spend this mana only to cast " +
        "spells with mana value 4 or greater.\n" +
        "At the beginning of your first main phase, you may pay {R}. If you do, transform Ashling."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        effect = addRimeboundMana
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = addRimeboundMana
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{R}"),
            then = Effects.Transform(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/back/7/d/7d7faefe-9c0d-45b6-8ea4-5fa666762a2c.jpg?1759144841"
    }
}

private val AshlingRekindledFront = card("Ashling, Rekindled") {
    manaCost = "{1}{R}"
    typeLine = "Legendary Creature — Elemental Sorcerer"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature enters or transforms into Ashling, Rekindled, you may " +
        "discard a card. If you do, draw a card.\n" +
        "At the beginning of your first main phase, you may pay {U}. If you do, transform Ashling."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = rummageMay
    }

    triggeredAbility {
        trigger = Triggers.self.transforms(false)
        effect = rummageMay
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{U}"),
            then = Effects.Transform(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d7faefe-9c0d-45b6-8ea4-5fa666762a2c.jpg?1759144841"
    }
}

val AshlingRekindled: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AshlingRekindledFront,
    backFace = AshlingRimebound
)
