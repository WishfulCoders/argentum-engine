package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Osseous Exhale
 * {1}{W}
 * Instant
 *
 * As an additional cost to cast this spell, you may behold a Dragon. (You may choose a
 * Dragon you control or reveal a Dragon card from your hand.)
 * Osseous Exhale deals 5 damage to target attacking or blocking creature. If a Dragon was
 * beheld, you gain 2 life.
 *
 * Implementation note: the oracle frames the optional behold as a cast-time additional cost,
 * but Behold has no real cost component (it neither pays mana nor exiles). Following the
 * Celestial Reunion precedent, the entire spell is modelled at resolution time: a gather of
 * the Dragons you control or hold, then a `ChooseUpTo(1)` selection ("you may behold") storing
 * the chosen Dragon under "beheld", and the life-gain rider gated on
 * [Conditions.CollectionContainsMatch] of that store. Choosing zero (or having no Dragons)
 * leaves the store empty, so the rider is skipped.
 */
val OsseousExhale = card("Osseous Exhale") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may behold a Dragon. " +
        "(You may choose a Dragon you control or reveal a Dragon card from your hand.)\n" +
        "Osseous Exhale deals 5 damage to target attacking or blocking creature. " +
        "If a Dragon was beheld, you gain 2 life."

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
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
            run(Effects.DealDamage(5, t))
            run(Effects.If(
                condition = whenMatches(beheld),
                then = Effects.GainLife(2)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Camille Alquier"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2300da2f-2297-4c2f-90c1-11ce2b42d91f.jpg?1743204021"
    }
}
