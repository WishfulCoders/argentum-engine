package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Witherbloom Charm
 * {B}{G}
 * Instant
 * Choose one —
 * • You may sacrifice a permanent. If you do, draw two cards.
 * • You gain 5 life.
 * • Destroy target nonland permanent with mana value 2 or less.
 *
 * The standard `modal(chooseCount = 1)` charm shape (sibling of Silverquill / Prismari / Lorehold).
 *
 * Mode 1 is the optional "you may sacrifice a permanent. If you do, draw two cards": a
 * [Effects.May] (the "you may") wrapping an [Effects.IfYouDo] whose `action` is a gather → choose →
 * sacrifice pipeline over the permanents you control, gating the two-card draw on the sacrifice
 * actually happening (the Highway Robbery idiom — `SuccessCriterion.Auto` infers success from the
 * terminal sacrifice move). Mode 2 is a flat life gain. Mode 3 destroys a single target nonland
 * permanent whose mana value is 2 or less (`TargetFilter.NonlandPermanent` + `manaValueAtMost(2)`).
 */
val WitherbloomCharm = card("Witherbloom Charm") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• You may sacrifice a permanent. If you do, draw two cards.\n" +
        "• You gain 5 life.\n" +
        "• Destroy target nonland permanent with mana value 2 or less."

    spell {
        modal(chooseCount = 1) {
            mode("You may sacrifice a permanent. If you do, draw two cards") {
                effect = Effects.May(
                    effect = Effects.IfYouDo(
                        action = Effects.Pipeline {
                            val permanents = gather(GameObjectFilter.Permanent, player = Player.You)
                            val chosen = chooseExactly(
                                1,
                                from = permanents,
                                useTargetingUI = true,
                                prompt = "Choose a permanent to sacrifice",
                            )
                            sacrifice(chosen)
                        },
                        then = Effects.DrawCards(2),
                    ),
                    descriptionOverride = "You may sacrifice a permanent. If you do, draw two cards.",
                )
            }
            mode("You gain 5 life") {
                effect = Effects.GainLife(5)
            }
            mode("Destroy target nonland permanent with mana value 2 or less") {
                val t = target(TargetFilter(GameObjectFilter.NonlandPermanent.manaValueAtMost(2)))
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Florian Herold"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/254437f7-7a8a-4b11-9cea-e8e7ea23c59e.jpg?1775938703"
    }
}
