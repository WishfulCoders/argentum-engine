package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flash Thompson, Spider-Fan
 * {1}{W}
 * Legendary Creature — Human Citizen
 * 2/2
 *
 * Flash
 * When Flash Thompson enters, choose one or both —
 * • Heckle — Tap target creature.
 * • Hero Worship — Untap target creature.
 *
 * "Choose one or both" modal ETB triggered ability (`chooseCount = 2, minChooseCount = 1`),
 * mirroring the Charming Prince modal-ETB idiom with per-mode targets. Each mode targets a
 * creature via its mode-local [EffectTarget.ContextTarget]; a modal triggered ability picks its
 * mode(s) and target(s) as it goes on the stack (CR 603.3c), so both modes may name distinct
 * creatures. The two flavor keyword names (Heckle / Hero Worship) are purely descriptive labels
 * on the modes and carry no rules meaning of their own.
 */
val FlashThompsonSpiderFan = card("Flash Thompson, Spider-Fan") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When Flash Thompson enters, choose one or both —\n" +
        "• Heckle — Tap target creature.\n" +
        "• Hero Worship — Untap target creature."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Heckle — Tap target creature.") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Tap(creature)
                },
                mode("Hero Worship — Untap target creature.") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Untap(creature)
                }
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Gal Or"
        flavorText = "\"No one cares about your Spider-Man obsession, Flash.\"\n—Liz Allan"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44cf372b-f668-45e9-981e-4533295dcc74.jpg?1783905364"
    }
}
