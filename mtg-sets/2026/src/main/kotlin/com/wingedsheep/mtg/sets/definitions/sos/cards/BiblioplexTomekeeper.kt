package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Biblioplex Tomekeeper — Secrets of Strixhaven #247
 * {4} · Artifact Creature — Construct · 3/4
 *
 * When this creature enters, choose up to one —
 * • Target creature becomes prepared. (Only creatures with prepare spells can become prepared.)
 * • Target creature becomes unprepared.
 *
 * Modeled as a modal ETB triggered ability with `chooseCount = 1, minChooseCount = 0` — the proven
 * "choose up to one"/"you may" shape (Highway Robbery, Hullbreaker Horror). Declining the modal is
 * the "up to" zero. Each mode targets a creature: mode 1 makes it prepared via
 * [Effects.BecomePrepared] (a no-op on non-PREPARE-layout creatures, matching the reminder text
 * "Only creatures with prepare spells can become prepared"); mode 2 makes it unprepared via
 * [Effects.Unprepare].
 */
val BiblioplexTomekeeper = card("Biblioplex Tomekeeper") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, choose up to one —\n" +
        "• Target creature becomes prepared. (Only creatures with prepare spells can become prepared.)\n" +
        "• Target creature becomes unprepared."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Target creature becomes prepared") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.BecomePrepared(creature)
                },
                mode("Target creature becomes unprepared") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Unprepare(creature)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0,
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Raph Lomotan"
        flavorText = "\"Due dates are firm, and will be enforced.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2efdd9-d2b4-4bea-a5b9-dbb2eee4dfba.jpg?1775938724"
    }
}
