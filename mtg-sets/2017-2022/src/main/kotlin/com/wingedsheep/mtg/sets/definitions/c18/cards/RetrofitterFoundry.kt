package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Retrofitter Foundry — Commander 2018 #57
 * {1} · Artifact · Rare
 *
 * {3}: Untap this artifact.
 * {2}, {T}: Create a 1/1 colorless Servo artifact creature token.
 * {1}, {T}, Sacrifice a Servo: Create a 1/1 colorless Thopter artifact creature token with flying.
 * {T}, Sacrifice a Thopter: Create a 4/4 colorless Construct artifact creature token.
 *
 * Four plain activated abilities. "Sacrifice a Servo / a Thopter" is any permanent you control with
 * that creature type (an animated or changeling Servo counts), as on Arms Dealer's "Sacrifice a
 * Goblin". The untap ability has no timing restriction, so it can be looped.
 */
val RetrofitterFoundry = card("Retrofitter Foundry") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}: Untap this artifact.\n" +
        "{2}, {T}: Create a 1/1 colorless Servo artifact creature token.\n" +
        "{1}, {T}, Sacrifice a Servo: Create a 1/1 colorless Thopter artifact creature token with flying.\n" +
        "{T}, Sacrifice a Thopter: Create a 4/4 colorless Construct artifact creature token."

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "{3}: Untap this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/6/e/6ec42fab-86b8-4479-90f2-a1690318d6d4.jpg?1783934348"
        )
        description = "{2}, {T}: Create a 1/1 colorless Servo artifact creature token."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Servo"))
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/6/2/62cafc0a-cd02-4265-aa1f-b8a6cb7cc8db.jpg?1783934348"
        )
        description = "{1}, {T}, Sacrifice a Servo: Create a 1/1 colorless Thopter artifact creature " +
            "token with flying."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Thopter"))
        )
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/f/7fee6034-f0bb-49b9-ad75-67d3d6be1159.jpg?1783934350"
        )
        description = "{T}, Sacrifice a Thopter: Create a 4/4 colorless Construct artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5da578b8-19e6-4068-9336-e7cd33c585f1.jpg?1783934323"
    }
}
