package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Myr Battlesphere
 * {7}
 * Artifact Creature — Myr Construct
 * 4/7
 * When this creature enters, create four 1/1 colorless Myr artifact creature tokens.
 * Whenever this creature attacks, you may tap X untapped Myr you control. If you do, this creature
 * gets +X/+0 until end of turn and deals X damage to the player or planeswalker it's attacking.
 *
 * "Tap X untapped Myr" is the Gather → choose-any-number → Tap pipeline (Orphans of the Wheat's
 * shape): X is chosen as the ability resolves by picking that many untapped Myr — any Myr you
 * control, not only the tokens — so choosing none is the "may" declined, which adds +0/+0 and deals
 * 0. The damage goes to [EffectTarget.AttackedPlayerOrPlaneswalker] of this creature itself; if it
 * has left the battlefield by then, the pump does nothing but the damage still reaches what it was
 * attacking, from last-known information (third ruling).
 */
val MyrBattlesphere = card("Myr Battlesphere") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr Construct"
    power = 4
    toughness = 7
    oracleText = "When this creature enters, create four 1/1 colorless Myr artifact creature tokens.\n" +
        "Whenever this creature attacks, you may tap X untapped Myr you control. If you do, this creature " +
        "gets +X/+0 until end of turn and deals X damage to the player or planeswalker it's attacking."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Myr"),
            count = 4,
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/8/182308b3-86e0-46d8-9104-95576a3d3921.jpg?1783941681",
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val untappedMyr = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Any.withSubtype("Myr").youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = untappedMyr,
                prompt = "You may tap X untapped Myr you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.ModifyStats(
                power = tapped.count,
                toughness = DynamicAmounts.fixed(0),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            ))
            run(Effects.DealDamage(tapped.count, EffectTarget.AttackedPlayerOrPlaneswalker(EffectTarget.Self)))
        }
        description = "Whenever this creature attacks, you may tap X untapped Myr you control. If you do, " +
            "this creature gets +X/+0 until end of turn and deals X damage to the player or planeswalker it's attacking."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0ae94ed-7314-470b-baba-f2f58bbc894a.jpg?1783941703"
        ruling("2020-08-07", "You choose the value for X as the last ability resolves. You can't choose a value for X that's greater than the number of untapped Myr you control.")
        ruling("2020-08-07", "You can tap any untapped Myr you control as the last ability resolves, not just the Myr tokens you created with the first ability. This includes Myr that haven't been under your control since your most recent turn began.")
        ruling("2020-08-07", "As the last ability resolves, you can tap untapped Myr you control even if Myr Battlesphere is no longer on the battlefield by then. If that has happened, Myr Battlesphere won't be able to get the +X/+0 bonus, but it will still deal X damage to the appropriate player or planeswalker.")
    }
}
