package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wishing Well {3}{U}
 * Artifact
 *
 * {T}: Put a coin counter on this artifact. When you do, you may cast target instant or
 * sorcery card with mana value equal to the number of coin counters on this artifact from
 * your graveyard without paying its mana cost. If that spell would be put into your
 * graveyard, exile it instead. Activate only as a sorcery.
 *
 * "When you do" is a reflexive triggered ability (CR 603.12) whose target is chosen as it is put
 * on the stack, *after* the coin counter lands — so the mana value to match is the new counter
 * count. The cap rides on the target filter, so it is read again as the reflexive ability
 * resolves. The cast happens straight from the graveyard while the reflexive ability resolves
 * (the Breaching Dragonstorm shape: [Effects.May] around the cast), and `insteadOfGraveyard = EXILE` is the printed
 * "exile it instead" rider.
 */
val WishingWell = card("Wishing Well") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}: Put a coin counter on this artifact. When you do, you may cast target instant or sorcery card with mana value equal to the number of coin counters on this artifact from your graveyard without paying its mana cost. If that spell would be put into your graveyard, exile it instead. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed

        effect = Effects.ReflexiveTrigger(
            action = Effects.AddCounters(CounterType.COIN, 1, EffectTarget.Self),
            optional = false,
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.InstantOrSorcery.ownedByYou()
                            .manaValueEqualsDynamic(DynamicAmounts.countersOnSelf(CounterType.COIN)),
                        zone = Zone.GRAVEYARD
                    )
                )
            ),
            reflexiveEffect = Effects.Pipeline {
                val wishedSpell = gather(CardSource.ChosenTargets)
                run(Effects.May(
                    Effects.CastFromCollectionWithoutPayingCost(
                        from = wishedSpell,
                        insteadOfGraveyard = AfterResolveDestination.EXILE
                    )
                ))
            },
            descriptionOverride = "Put a coin counter on this artifact. When you do, you may cast " +
                "target instant or sorcery card with mana value equal to the number of coin counters " +
                "on this artifact from your graveyard without paying its mana cost. If that spell " +
                "would be put into your graveyard, exile it instead."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edeb20aa-b253-49b8-9947-c397a3a4002a.jpg?1721426330"
        ruling("2024-07-26", "You cast the instant or sorcery while the ability is resolving and still on the stack. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2024-07-26", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
