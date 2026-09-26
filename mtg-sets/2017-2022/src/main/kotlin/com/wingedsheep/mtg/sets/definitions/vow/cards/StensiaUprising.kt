package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Stensia Uprising — Innistrad: Crimson Vow #178
 * {2}{R}{R} · Enchantment · Rare
 * Artist: Dan Murayama Scott
 *
 * At the beginning of your end step, create a 1/1 red Human creature token. Then if you control
 * exactly thirteen permanents, you may sacrifice this enchantment. When you do, it deals 7 damage
 * to any target.
 *
 * A single `Triggers.you.beginningOf(Step.END)` trigger whose effect is a [Effects.Composite]:
 *  1. [Effects.CreateToken] — one 1/1 red Human.
 *  2. A [Effects.If] gated on `CompareAmounts(AggregateBattlefield(You, Any) == 13)` — the
 *     "then if you control exactly thirteen permanents" intervening clause. GTE-style helpers
 *     (`ControlPermanentsAtLeast`) can't express *exactly* 13, so this uses the general
 *     [Conditions.CompareAmounts] with `ComparisonOperator.EQ`. The token just created counts toward
 *     the 13 (it's on the battlefield when the composite's second effect resolves).
 *  3. When the condition holds, a [ReflexiveTriggerEffect] offers the optional sacrifice
 *     (`Effects.SacrificeTarget(Self)`); on completion the reflexive trigger deals 7 damage to any
 *     target (`Targets.Any` — creature, player, planeswalker, or battle). "It deals" — the damage
 *     source is the enchantment (Self), captured as last-known-information after it's sacrificed.
 */
val StensiaUprising = card("Stensia Uprising") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, create a 1/1 red Human creature token. " +
        "Then if you control exactly thirteen permanents, you may sacrifice this enchantment. " +
        "When you do, it deals 7 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf(Subtype.HUMAN.value),
            imageUri = "https://cards.scryfall.io/normal/front/1/1/11c8ff82-b598-4ccc-83a7-99f1e53b64d3.jpg?1783924697"
        ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any).count(),
                    ComparisonOperator.EQ,
                    13
                ),
                then = Effects.ReflexiveTrigger(
                    action = Effects.SacrificeTarget(EffectTarget.Self),
                    optional = true,
                    descriptionOverride = "You may sacrifice this enchantment. When you do, " +
                        "it deals 7 damage to any target."
                ) {
                    val anyTarget = target(Targets.Any)
                    effect = Effects.DealDamage(
                        amount = 7,
                        target = anyTarget,
                        damageSource = EffectTarget.Self
                    )
                }
            )
        description = "At the beginning of your end step, create a 1/1 red Human creature token. " +
            "Then if you control exactly thirteen permanents, you may sacrifice this enchantment. " +
            "When you do, it deals 7 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Dan Murayama Scott"
        flavorText = "\"Vampires have done nothing but take from us. Tonight, we reclaim what's ours!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df71a8c1-25af-4e6a-9197-11b96b959b46.jpg?1783924823"
    }
}
