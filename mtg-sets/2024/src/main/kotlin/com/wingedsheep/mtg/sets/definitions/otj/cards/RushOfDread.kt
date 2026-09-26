package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rush of Dread {1}{B}{B}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Target opponent sacrifices half the creatures they control of their choice, rounded up.
 * + {2} — Target opponent discards half the cards in their hand, rounded up.
 * + {2} — Target opponent loses half their life, rounded up.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166): at least one mode must be chosen and no mode
 * can be chosen more than once. Each mode targets an opponent independently
 * ([Targets.Opponent] → the mode's `ContextTarget(0)`). "Half … rounded up" is
 * `Divide(<count>, Fixed(2), roundUp = true)`, and the count is read off the *chosen* opponent
 * via [Player.ContextPlayer]/[EffectTarget.ContextTarget] index 0:
 * - creatures they control → `AggregateBattlefield(ContextPlayer(0), Creature)`
 * - cards in their hand → `AggregateZone(ContextPlayer(0), HAND)`
 * - their life → `LifeTotal(ContextPlayer(0))`.
 */
val RushOfDread = card("Rush of Dread") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Target opponent sacrifices half the creatures they control of their choice, rounded up.\n" +
        "+ {2} — Target opponent discards half the cards in their hand, rounded up.\n" +
        "+ {2} — Target opponent loses half their life, rounded up."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("+ {1} — Target opponent sacrifices half the creatures they " +
                    "control of their choice, rounded up.") {
                    val opponent = target(Targets.Opponent)
                    additionalManaCost = "{1}"
                    effect = Effects.Sacrifice(
                        filter = GameObjectFilter.Creature,
                        count = DynamicAmounts.battlefield(
                            opponent.asPlayer,
                            GameObjectFilter.Creature
                        ).count() divRoundedUp 2,
                        target = opponent
                    )
                },
                mode("+ {2} — Target opponent discards half the cards in their hand, rounded up.") {
                    val opponent = target(Targets.Opponent)
                    additionalManaCost = "{2}"
                    effect = Effects.Discard(
                        count = DynamicAmounts.zone(
                            opponent.asPlayer,
                            Zone.HAND
                        ).count() divRoundedUp 2,
                        target = opponent
                    )
                },
                mode("+ {2} — Target opponent loses half their life, rounded up.") {
                    val opponent = target(Targets.Opponent)
                    additionalManaCost = "{2}"
                    effect = Effects.LoseLife(
                        amount = DynamicAmounts.lifeTotal(opponent.asPlayer) divRoundedUp 2,
                        target = opponent
                    )
                }
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/721c7122-91b6-45ea-ba28-de0246a2fc1b.jpg?1712860603"

        ruling("2024-04-12", "Like the effects of all modal spells, Rush of Dread's effects happen in order. If the first two modes target different opponents, the opponent targeted by the second mode will see what the first opponent sacrificed before choosing what to discard.")
        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "If all targets for the chosen modes become illegal before a spell with spree resolves, the spell won't resolve. If at least one target is still legal, the spell resolves but has no effect on illegal targets.")
        ruling("2024-04-12", "The mana value of a spell with spree is determined only by its mana cost. It doesn't matter which modes you choose or which additional costs you pay.")
    }
}
