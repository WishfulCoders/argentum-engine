package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sewer-veillance Cam
 * {U}
 * Artifact
 *
 * Flash
 * When this artifact enters or leaves the battlefield, you may tap or untap target creature.
 * {3}{U}, Sacrifice this artifact: Draw two cards.
 */
val SewerVeillanceCam = card("Sewer-veillance Cam") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Flash\nWhen this artifact enters or leaves the battlefield, you may tap or untap target creature.\n{3}{U}, Sacrifice this artifact: Draw two cards."

    keywords(Keyword.FLASH)

    // "you may tap or untap target creature" — a 2-mode modal (tap / untap) over the
    // declared target, made optional with Effects.May (Wingnut's countsAsModalSpell=false idiom).
    fun tapOrUntapTarget(target: EffectTarget) = Effects.May(
        Effects.Modal(
            modes = listOf(
                Mode.noTarget(Effects.Tap(target), "Tap that creature"),
                Mode.noTarget(Effects.Untap(target), "Untap that creature")
            ),
            chooseCount = 1,
            countsAsModalSpell = false
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = tapOrUntapTarget(creature)
        description = "When this artifact enters, you may tap or untap target creature."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        val creature = target(TargetFilter.Creature)
        effect = tapOrUntapTarget(creature)
        description = "When this artifact leaves the battlefield, you may tap or untap target creature."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{U}"),
            Costs.SacrificeSelf
        )
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Nicholas Gregory"
        flavorText = "No one in New York was going to notice one more cockroach."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab47a37b-b66d-4f70-9bf0-4d5ed6b518f3.jpg?1779102308"
    }
}
