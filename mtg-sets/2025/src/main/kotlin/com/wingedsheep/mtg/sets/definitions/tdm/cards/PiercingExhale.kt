package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Piercing Exhale
 * {1}{G}
 * Instant
 *
 * As an additional cost to cast this spell, you may behold a Dragon. (You may choose a
 * Dragon you control or reveal a Dragon card from your hand.)
 * Target creature you control deals damage equal to its power to target creature or
 * planeswalker. If a Dragon was beheld, surveil 2.
 *
 * Implementation note: as with the other Exhale cards, the optional behold (which has no
 * real cost component) is modelled at resolution time as a gather of your Dragons followed by
 * a `ChooseUpTo(1)` selection ("you may behold") storing the chosen Dragon under "beheld";
 * the surveil rider is gated on [Conditions.CollectionContainsMatch] of that store (empty when
 * you choose none).
 */
val PiercingExhale = card("Piercing Exhale") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may behold a Dragon. " +
        "(You may choose a Dragon you control or reveal a Dragon card from your hand.)\n" +
        "Target creature you control deals damage equal to its power to target creature or " +
        "planeswalker. If a Dragon was beheld, surveil 2."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            // Optional behold: gather your Dragons and choose up to one of them.
            val beholdable = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.BATTLEFIELD, Zone.HAND),
                    player = Player.You,
                    filter = Filters.WithSubtype("Dragon")
                )
            )
            val beheld = chooseUpTo(1, from = beholdable, prompt = "You may behold a Dragon")
            reveal(beheld)
            run(Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = victim,
                damageSource = myCreature
            ))
            run(Effects.If(
                condition = whenMatches(beheld),
                then = Patterns.Library.surveil(2)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2a0deb9-5bc3-42d5-9e1e-5f463d176aef.jpg?1743204571"
    }
}
