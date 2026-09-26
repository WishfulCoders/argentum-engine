package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shifting Grift
 * {U}{U}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Exchange control of two target creatures.
 * + {1} — Exchange control of two target artifacts.
 * + {1} — Exchange control of two target enchantments.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 702.166 / 700.2). Every mode targets
 * two permanents of the listed type (any controller — you needn't control either) and
 * resolves the swap via [Effects.ExchangeControl] over the mode-local `ContextTarget(0)` and
 * `ContextTarget(1)` (same atom as Chromeshell Crab / Phyrexian Infiltrator). The control
 * change lasts indefinitely (CR ruling) — `ExchangeControlEffect` installs a permanent
 * control override, so it doesn't wear off at cleanup and survives the target later losing
 * the permanent type.
 *
 * Per the spree rulings, each mode's two targets are independent target requirements, so a
 * mode is selectable only if two legal targets of that type exist; if one target becomes
 * illegal before resolution, that mode's exchange simply doesn't happen while other chosen
 * modes still resolve.
 */
val ShiftingGrift = card("Shifting Grift") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Exchange control of two target creatures.\n" +
        "+ {1} — Exchange control of two target artifacts.\n" +
        "+ {1} — Exchange control of two target enchantments."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                // + {2} — Exchange control of two target creatures.
                mode("+ {2} — Exchange control of two target creatures.") {
                    val firstCreature = target(TargetFilter.Creature)
                    val secondCreature = target(TargetFilter.Creature)
                    additionalManaCost = "{2}"
                    effect = Effects.ExchangeControl(
                        firstCreature,
                        secondCreature
                    )
                },
                // + {1} — Exchange control of two target artifacts.
                mode("+ {1} — Exchange control of two target artifacts.") {
                    val firstArtifact = target(TargetFilter.Artifact)
                    val secondArtifact = target(TargetFilter.Artifact)
                    additionalManaCost = "{1}"
                    effect = Effects.ExchangeControl(
                        firstArtifact,
                        secondArtifact
                    )
                },
                // + {1} — Exchange control of two target enchantments.
                mode("+ {1} — Exchange control of two target enchantments.") {
                    val firstEnchantment = target(TargetFilter.Enchantment)
                    val secondEnchantment = target(TargetFilter.Enchantment)
                    additionalManaCost = "{1}"
                    effect = Effects.ExchangeControl(
                        firstEnchantment,
                        secondEnchantment
                    )
                }
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Nereida"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20b8313b-a680-4dca-959a-1a7fa5cb4b1b.jpg?1712860608"

        ruling("2024-04-12", "Gaining control of a permanent doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2024-04-12", "If one of the target permanents is an illegal target when Shifting Grift resolves, the exchange that permanent is involved in won't happen.")
        ruling("2024-04-12", "The effects of Shifting Grift's modes last indefinitely. They don't wear off during the cleanup step, and they don't expire if one of the target permanents stops having that permanent type after Shifting Grift has resolved.")
        ruling("2024-04-12", "You don't have to control any of the target permanents.")
        ruling("2024-04-12", "If the same player controls both permanents involved in a single exchange when that exchange occurs, nothing happens to those permanents.")
    }
}
