package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

val RentIsDue = card("Rent Is Due") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, you may tap two untapped creatures and/or Treasures you control. If you do, draw a card. Otherwise, sacrifice this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val tapCost = Effects.Pipeline {
            val rentTargets = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = (GameObjectFilter.Creature or GameObjectFilter.Artifact.withSubtype("Treasure")).untapped()
                )
            )
            val toTap = chooseExactly(
                2,
                from = rentTargets,
                prompt = "Tap two untapped creatures and/or Treasures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(toTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.DrawCards(1),
            otherwise = SacrificeSelfEffect,
            descriptionOverride = "You may tap two untapped creatures and/or Treasures you control. If you do, draw a card. Otherwise, sacrifice this enchantment."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Gal Or"
        flavorText = "\"You're a month late again, Parker! Give me my money or you're out on the street!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3f8d221-081f-49f5-a501-07e5eb21a840.jpg?1757376808"
    }
}
