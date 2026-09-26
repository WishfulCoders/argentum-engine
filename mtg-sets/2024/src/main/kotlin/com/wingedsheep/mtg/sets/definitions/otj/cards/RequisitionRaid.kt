package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Requisition Raid {W}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Destroy target artifact.
 * + {1} — Destroy target enchantment.
 * + {1} — Put a +1/+1 counter on each creature target player controls.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`,
 * `chooseCount = modes.size`, and per-mode `additionalManaCost` (CR 702.166).
 * Mode 2 distributes a +1/+1 counter over every creature the targeted player
 * controls (`ForEachInGroup` over a target-relative group filter, with the counter
 * placed on each iterated permanent via [EffectTarget.IterationEntity]).
 */
val RequisitionRaid = card("Requisition Raid") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Destroy target artifact.\n" +
        "+ {1} — Destroy target enchantment.\n" +
        "+ {1} — Put a +1/+1 counter on each creature target player controls."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("+ {1} — Destroy target artifact.") {
                    val artifact = target(TargetFilter.Artifact)
                    additionalManaCost = "{1}"
                    effect = Effects.Destroy(artifact)
                },
                mode("+ {1} — Destroy target enchantment.") {
                    val enchantment = target(TargetFilter.Enchantment)
                    additionalManaCost = "{1}"
                    effect = Effects.Destroy(enchantment)
                },
                mode("+ {1} — Put a +1/+1 counter on each creature target player controls.") {
                    val player = target(Targets.Player)
                    additionalManaCost = "{1}"
                    effect = Effects.ForEachInGroup(
                        filter = GroupFilter(
                            GameObjectFilter.Creature.targetPlayerControls(player)
                        ),
                        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                    )
                }
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Viko Menezes"
        flavorText = "The Freestriders believe in liberation, in all its many forms."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/154e9ba9-0d0e-4b0e-acf2-f66a993cf3a2.jpg?1712860601"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
    }
}
