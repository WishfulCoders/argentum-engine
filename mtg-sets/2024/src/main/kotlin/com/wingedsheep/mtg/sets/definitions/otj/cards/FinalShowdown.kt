package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Final Showdown {W}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — All creatures lose all abilities until end of turn.
 * + {1} — Choose a creature you control. It gains indestructible until end of turn.
 * + {3}{W}{W} — Destroy all creatures.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, and per-mode
 * `additionalManaCost` (CR 702.166). Chosen modes always resolve in printed order (CR ruling),
 * which the resolver honours by mode index — so "lose all abilities" precedes "indestructible"
 * precedes "destroy all". Each mode composes existing atoms:
 *  - Mode 1: [Effects.ForEachInGroup] over every creature, each losing all abilities
 *    (`RemoveAllAbilities(IterationEntity)` per iteration). A creature that gains an ability *after* this
 *    resolves keeps it (the ruling) because the removal is a one-shot snapshot, not a continuous
 *    layer effect tied to a filter.
 *  - Mode 2 does **not** target (per ruling: the creature is chosen as the spell resolves, too
 *    late to respond). Modeled as a resolution-time gather -> choose-exactly-one -> grant pipeline:
 *    [GatherCardsEffect] over creatures you control, [SelectFromCollectionEffect] (battlefield
 *    click UI), then [ForEachEffect] over the singleton selection grants indestructible to `IterationEntity`.
 *  - Mode 3: [Effects.DestroyAll] over all creatures (honours indestructible granted by mode 2).
 */
val FinalShowdown = card("Final Showdown") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — All creatures lose all abilities until end of turn.\n" +
        "+ {1} — Choose a creature you control. It gains indestructible until end of turn.\n" +
        "+ {3}{W}{W} — Destroy all creatures."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                Mode(
                    effect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature),
                        Effects.RemoveAllAbilities(EffectTarget.IterationEntity)
                    ),
                    description = "+ {1} — All creatures lose all abilities until end of turn.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.Pipeline {
                        val chooseIndestructible = gather(
                            CardSource.ControlledPermanents(
                                filter = GameObjectFilter.Creature
                            )
                        )
                        val indestructibleChosen = chooseExactly(
                            1,
                            from = chooseIndestructible,
                            useTargetingUI = true,
                            prompt = "Choose a creature you control to gain indestructible"
                        )
                        run(Effects.ForEachInCollection(
                            indestructibleChosen,
                            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
                        ))
                    },
                    description = "+ {1} — Choose a creature you control. It gains indestructible until end of turn.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.DestroyAll(GameObjectFilter.Creature),
                    description = "+ {3}{W}{W} — Destroy all creatures.",
                    additionalManaCost = "{3}{W}{W}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "11"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/358968f9-45bd-4022-b6bc-f1f7e0adf0e7.jpg?1712860587"

        ruling("2024-04-12", "The second mode of Final Showdown doesn't target the creature. You don't choose which creature will gain indestructible until the spell is resolving. At that point, it's too late for anyone to respond to the spell.")
        ruling("2024-04-12", "If an effect grants a creature an ability after Final Showdown causes all creatures to lose all abilities, that creature won't lose that ability.")
        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "No matter which modes you choose, you always follow the instructions in the order they are written.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "No player can cast spells or activate abilities in between the modes of a resolving spell. Any abilities that trigger won't be put onto the stack until the spell is done resolving.")
    }
}
