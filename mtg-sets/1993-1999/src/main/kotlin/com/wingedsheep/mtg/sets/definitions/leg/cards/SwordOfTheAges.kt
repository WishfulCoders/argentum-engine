package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sword of the Ages
 * {6}
 * Artifact
 *
 * This artifact enters tapped.
 * {T}, Sacrifice this artifact and any number of creatures you control: This artifact deals X
 * damage to any target, where X is the total power of the creatures sacrificed this way, then
 * exile this artifact and those creature cards.
 *
 * The creatures are a variable-count sacrifice cost (zero is legal), snapshotted as they last
 * existed on the battlefield, so X is their last-known total power (2009-10-01 ruling). The Sword
 * itself is sacrificed by its own `SacrificeSelf` cost and isn't one of "the creatures sacrificed
 * this way". Each exile only finds a card still in the graveyard it was sacrificed into.
 */
val SwordOfTheAges = card("Sword of the Ages") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}, Sacrifice this artifact and any number of creatures you control: This artifact deals X " +
        "damage to any target, where X is the total power of the creatures sacrificed this way, then " +
        "exile this artifact and those creature cards."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf,
            Costs.SacrificePermanents(GameObjectFilter.Creature, minCount = 0, excludeSelf = true)
        )
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            run(Effects.DealDamage(DynamicAmounts.totalPowerSacrificedThisWay(), t))
            run(Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD))
            exile(gather(CardSource.SacrificedThisWay))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "296"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b37fd4cc-ab5c-4c65-80ff-8f905b31e801.jpg?1783948025"

        ruling(
            "2009-10-01",
            "The activated ability checks the power the exiled creatures had as they last existed on the battlefield."
        )
    }
}
