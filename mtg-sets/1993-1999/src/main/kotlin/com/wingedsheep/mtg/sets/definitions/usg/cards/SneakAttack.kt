package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sneak Attack
 * {3}{R}
 * Enchantment
 *
 * {R}: You may put a creature card from your hand onto the battlefield. That creature gains haste.
 * Sacrifice the creature at the beginning of the next end step.
 *
 * The same pipeline as Through the Breach, on a repeatable activated ability (instant speed —
 * no timing restriction). Haste has no printed duration, so it is `Duration.Permanent`; the
 * delayed sacrifice is bound to that exact object, so a creature that left and came back is a new
 * object and is not sacrificed (2020-08-07 ruling).
 */
val SneakAttack = card("Sneak Attack") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{R}: You may put a creature card from your hand onto the battlefield. That creature " +
        "gains haste. Sacrifice the creature at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.Pipeline {
            val candidates = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
            val putting = chooseUpTo(1, from = candidates)
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(putting) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = putting.asTarget,
                    duration = Duration.Permanent
                ))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(putting.asTarget)
                ))
            }
        }
        description = "{R}: You may put a creature card from your hand onto the battlefield. It gains haste. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Jerry Tiritilli"
        flavorText = "\"Nothin' beat surprise—'cept rock.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d07dc95d-82a8-4a58-8ea2-d4513bd7316d.jpg?1783946324"

        ruling("2020-08-07", "You sacrifice the creature only if you still control it. If that creature has left the battlefield, even if it came back, you don't sacrifice it.")
    }
}
