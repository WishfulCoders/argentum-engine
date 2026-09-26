package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

val MeandersGuide = card("Meanders Guide") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Merfolk Scout"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, you may tap another untapped Merfolk you control. " +
        "When you do, return target creature card with mana value 3 or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        // The "may tap another untapped Merfolk" is not a target — it's a resolution-time
        // choice. Selecting the Merfolk happens via SelectTargetEffect after the player
        // accepts the optional, so declining the may does not force them to commit to one.
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val merfolkToTap = selectTarget(
                    TargetObject(
                        // Permanent (not Creature) so Kindred Artifacts with the Merfolk subtype qualify.
                        filter = TargetFilter.PermanentYouControl
                            .withSubtype("Merfolk")
                            .untapped()
                            .other()
                    )
                )
                run(Effects.Tap(merfolkToTap.asTarget))
            },
            optional = true) {
            val creature = target(
                TargetFilter(
                    GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),
                    zone = Zone.GRAVEYARD
                ),
            )
            effect = Effects.Move(
                target = creature,
                destination = Zone.BATTLEFIELD
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Julie Dillon"
        flavorText = "In the silence of the Dark Meanders, she thought only of those still lost."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c41a0ad-138e-4eef-8f7f-35017e3b086f.jpg?1767871713"
    }
}
