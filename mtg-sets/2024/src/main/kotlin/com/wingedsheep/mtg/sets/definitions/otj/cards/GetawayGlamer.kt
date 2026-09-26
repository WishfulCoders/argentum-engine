package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Getaway Glamer {W}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Exile target nontoken creature. Return it to the battlefield under its owner's
 *         control at the beginning of the next end step.
 * + {2} — Destroy target creature if no other creature has greater power.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166 / OTJ release notes): at least one mode must
 * be chosen and the same mode can't be chosen more than once (so `allowRepeat = false`).
 *
 * Mode 1 (blink): [Effects.Move] to exile, then a [CreateDelayedTriggerEffect] on the next end
 * step returns the card to the battlefield — the proven Parting Gust blink shape, without the
 * +1/+1 counter. The "nontoken" restriction is on the target filter.
 *
 * Mode 2 (conditional destroy): "no other creature has greater power" — gate [Effects.Destroy]
 * on `target power >= the greatest power among all creatures`. Including the target itself in
 * the MAX makes this equivalent to "no other creature strictly exceeds the target's power":
 * if any other creature had greater power, the max would exceed the target's power and the
 * comparison would fail. The aggregate reads projected power, so layer/counter effects apply.
 */
val GetawayGlamer = card("Getaway Glamer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Exile target nontoken creature. Return it to the battlefield under its owner's " +
        "control at the beginning of the next end step.\n" +
        "+ {2} — Destroy target creature if no other creature has greater power."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("+ {1} — Exile target nontoken creature. Return it to the " +
                    "battlefield under its owner's control at the beginning of the next end step.") {
                    val creature = target(TargetFilter(GameObjectFilter.Creature.nontoken()))
                    additionalManaCost = "{1}"
                    effect = Effects.Move(creature, Zone.EXILE) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.Move(creature, Zone.BATTLEFIELD)
                        )
                },
                mode("+ {2} — Destroy target creature if no other creature has greater power.") {
                    val creature = target(TargetFilter.Creature)
                    additionalManaCost = "{2}"
                    effect = Effects.If(
                        condition = Conditions.CompareAmounts(
                            left = DynamicAmounts.powerOf(creature),
                            operator = ComparisonOperator.GTE,
                            right = DynamicAmounts.battlefield(
                                Player.Each,
                                GameObjectFilter.Creature
                            ).maxPower()
                        ),
                        then = Effects.Destroy(creature)
                    )
                }
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Forrest Imel"
        flavorText = "As the vault collapsed around them, Kellan made sure Akul couldn't follow them out."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69689049-a704-4f16-84ee-4d5d915028ec.jpg?1712860589"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "The second mode's destruction is conditional. As the spell resolves, you check whether any creature has greater power than the targeted creature. If one does, the targeted creature isn't destroyed.")
        ruling("2024-04-12", "The exiled creature returns at the beginning of the next end step as a new object with no relation to its previous existence.")
    }
}
