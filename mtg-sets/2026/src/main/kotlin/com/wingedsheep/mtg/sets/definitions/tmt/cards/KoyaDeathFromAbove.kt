package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Koya, Death from Above
 * {2}{W}
 * Legendary Creature — Mutant Ninja Bird
 * 2/1
 *
 * Flying
 * When Koya enters, exile up to one other target creature. At the beginning of the
 * next end step, you may pay {3}{B}. If you don't, return that card to the
 * battlefield under its owner's control.
 */
val KoyaDeathFromAbove = card("Koya, Death from Above") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Mutant Ninja Bird"
    oracleText = "Flying\nWhen Koya enters, exile up to one other target creature. At the beginning of the next end step, you may pay {3}{B}. If you don't, return that card to the battlefield under its owner's control."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    // Exile the creature linked to Koya; at the next end step, pay to keep it gone, otherwise
    // return the linked-exiled card to its owner's control (read by Koya's linked exile).
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature, excludeSelf = true), optional = true)
        effect = Effects.Move(creature, Zone.EXILE, linkToSource = true) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Mana("{3}{B}"),
                    suffer = Effects.Pipeline {
                        val koyaExile = gather(CardSource.FromLinkedExile())
                        // The exiled creature returns under its *owner's* control, not Koya's
                        // controller's — it may be a creature an opponent owns.
                        move(koyaExile, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
                    }
                )
            )
        description = "When Koya enters, exile up to one other target creature. At the beginning of the next end step, you may pay {3}{B}. If you don't, return that card to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Irina Nordsol"
        flavorText = "\"Hello, prey.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a4f1ccc-2225-4cd0-abef-0eb7a8eae9cf.jpg?1771586885"
    }
}
