package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flash
 * {1}{U}
 * Instant
 * You may put a creature card from your hand onto the battlefield. If you do, sacrifice it unless
 * you pay its mana cost reduced by {2}.
 *
 * "You may" is a choose-up-to-one from the creature cards in hand; "if you do" gates on what
 * actually entered (`moveTracked`). The payment is `Costs.pay.ManaCostOf(it, genericReduction = 2)`,
 * which `PayOrSufferExecutor` lowers at resolution to the creature's printed cost with {X} as 0
 * (ruling: X is 0) and only the generic part reduced (ruling: {1}{R} costs {R}). The creature
 * really enters, so its enters triggers fire even if it is then sacrificed, and the sacrifice
 * happens inside Flash's resolution — no player gets priority in between, and its leaves triggers
 * go on the stack together with its enters triggers (2018-03-16 rulings).
 */
val Flash = card("Flash") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may put a creature card from your hand onto the battlefield. If you do, sacrifice it " +
        "unless you pay its mana cost reduced by {2}."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
            val chosen = chooseUpTo(
                1,
                from = creatures,
                prompt = "You may put a creature card from your hand onto the battlefield"
            )
            val entered = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            ifNotEmpty(entered) {
                run(
                    Effects.PayOrSuffer(
                        cost = Costs.pay.ManaCostOf(entered.asTarget, genericReduction = 2),
                        suffer = Effects.SacrificeTarget(entered.asTarget),
                        consequenceDescription = "sacrifice the creature Flash put onto the battlefield"
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "David Ho"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63af3c26-5b1f-46f6-9aa2-036c615bf5ea.jpg?1783947110"
        ruling("2018-03-16", "If the creature has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2018-03-16",
            "Only the generic mana in that creature's mana cost is reduced. For example, if that creature's " +
                "mana cost is {1}{R}, you'll have to pay {R} to keep it."
        )
        ruling(
            "2018-03-16",
            "The creature enters the battlefield, so it will trigger enters-the-battlefield abilities even if " +
                "you choose not to pay."
        )
        ruling(
            "2018-03-16",
            "If you choose not to pay, the creature is sacrificed immediately. No player will get priority in " +
                "between the creature entering the battlefield and being sacrificed. Sacrificing the creature this " +
                "way will trigger any abilities that trigger when it leaves the battlefield, and those abilities " +
                "will be put onto the stack at the same time as those that triggered when it entered the battlefield."
        )
    }
}
