package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vincent's Limit Break
 * {1}{B}
 * Instant
 *
 * Tiered (Choose one additional cost.)
 * Until end of turn, target creature you control gains "When this creature dies, return it to the
 * battlefield tapped under its owner's control" and has the chosen base power and toughness.
 * • Galian Beast — {0} — 3/2.
 * • Death Gigas — {1} — 5/2.
 * • Hellmasker — {3} — 7/2.
 *
 * Tiered (CR 702.183): a choose-one modal spell where the chosen tier's additional mana cost is
 * paid at cast. Every tier applies the same shared effect — set base power/toughness (layer 7b)
 * until end of turn plus a granted "dies → return tapped" self-trigger — differing only in the
 * base power/toughness value.
 */
val VincentsLimitBreak = card("Vincent's Limit Break") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Tiered (Choose one additional cost.)\n" +
        "Until end of turn, target creature you control gains \"When this creature dies, return it " +
        "to the battlefield tapped under its owner's control\" and has the chosen base power and " +
        "toughness.\n" +
        "• Galian Beast — {0} — 3/2.\n" +
        "• Death Gigas — {1} — 5/2.\n" +
        "• Hellmasker — {3} — 7/2."

    spell {
        tiered {
            tier("Galian Beast", "{0}", "3/2.") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = transform(creatureYouControl, 3, 2)
            }
            tier("Death Gigas", "{1}", "5/2.") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = transform(creatureYouControl, 5, 2)
            }
            tier("Hellmasker", "{3}", "7/2.") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = transform(creatureYouControl, 7, 2)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Ryuichi Sakuma"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0502c426-5271-4989-8598-5bc159afe79c.jpg?1748718952"
    }
}

/**
 * The shared per-tier effect: until end of turn, the target creature you control has base
 * power/toughness [power]/[toughness] and gains a granted "When this creature dies, return it to
 * the battlefield tapped under its owner's control" self-trigger. The grant lasts until end of
 * turn, but a creature that dies during that window leaves the ability to resolve from the
 * graveyard (mirrors the Earthbend "return tapped" composition).
 */
private fun transform(creature: EffectTarget, power: Int, toughness: Int): Effect {
    val diesReturnTapped = TriggeredAbility.create(
        trigger = Triggers.self.dies(),
        effect = Effects.Move(
            target = EffectTarget.Self,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.Tapped,
            fromZone = Zone.GRAVEYARD
        ),
        descriptionOverride = "When this creature dies, return it to the battlefield tapped under its owner's control."
    )
    return Effects.SetBasePowerAndToughness(power, toughness, creature, Duration.EndOfTurn) then
        Effects.GrantTriggeredAbility(diesReturnTapped, creature, Duration.EndOfTurn)
}
