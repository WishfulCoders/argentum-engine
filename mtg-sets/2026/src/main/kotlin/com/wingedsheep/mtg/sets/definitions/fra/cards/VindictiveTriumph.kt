package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Vindictive Triumph — Reality Fracture #162
 * {W}{B}{B} · Instant · Rare
 *
 * Exile target creature or planeswalker. If that permanent's mana value was 3 or less, return it
 * to the battlefield tapped under your control. Exile it at the beginning of the next end step.
 *
 * "Was" is last-known information about the *permanent*, so the mana-value test runs before the
 * exile, while the target is still on the battlefield: [Conditions.TargetMatchesFilter] reads it
 * through the predicate evaluator, which sees a face-down permanent's mana value as 0 even though
 * the card underneath it may cost more. The gate then picks one of two branches:
 *
 *  - **MV ≤ 3** — the Come Back Wrong pipeline with exile in place of destroy: gather the target,
 *    exile it (capturing what actually moved), put the nontoken card back onto the battlefield
 *    tapped under your control, and arm a delayed "exile it" at the next end step bound to that
 *    returned permanent. A token stops existing once it has left the battlefield, so it never
 *    comes back.
 *  - **otherwise** — a plain exile.
 *
 * "The next end step" is anyone's, so the delayed trigger carries no player gate.
 */
val VindictiveTriumph = card("Vindictive Triumph") {
    manaCost = "{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Exile target creature or planeswalker. If that permanent's mana value was 3 or less, " +
        "return it to the battlefield tapped under your control. Exile it at the beginning of the next end step."

    spell {
        val permanent = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.manaValueAtMost(3), permanent),
            then = Effects.Pipeline {
                val targeted = gather(CardSource.ChosenTargets)
                val exiled = moveTracked(targeted, CardDestination.ToZone(Zone.EXILE))
                val returned = moveTracked(
                    exiled,
                    CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    filter = GameObjectFilter.Any.nontoken()
                )
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.Exile(returned.asTarget)
                ))
            },
            otherwise = Effects.Exile(permanent)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Johan Grenier"
        flavorText = "As much as Liliana never wanted to see her opaline double again, she had always " +
            "been loath to prematurely discard a useful tool."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a803dbe7-153a-4e92-ad4d-c2babebe003d.jpg?1789127665"
        inBooster = false
    }
}
