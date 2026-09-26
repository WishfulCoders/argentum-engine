package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ravenous Sailback
 * {4}{G}
 * Creature — Dinosaur
 * 3/4
 * When this creature enters, choose one —
 * • This creature gains haste until end of turn.
 * • Destroy target artifact or enchantment.
 *
 * A modal *triggered* ability (CR 700.2 / 603.3c): the mode is chosen as the trigger goes on the
 * stack, so the destroy mode's target is picked then too. [TriggeredAbilityBuilder] has no `modal`
 * shorthand, so the body is a [ModalEffect] directly.
 */
val RavenousSailback = card("Ravenous Sailback") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    oracleText = "When this creature enters, choose one —\n" +
        "• This creature gains haste until end of turn.\n" +
        "• Destroy target artifact or enchantment."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(
                    effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self),
                    description = "This creature gains haste until end of turn."
                ),
                mode("Destroy target artifact or enchantment.") {
                    val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
                    effect = Effects.Destroy(artifactOrEnchantment)
                }
            ),
            chooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Andrew Mar"
        flavorText = "Converter beasts were sent to ensnare as many of Ixalan's dinosaurs as " +
            "possible. Many of them were converted themselves—into snacks."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a225cb30-9f3e-4cfa-bdd6-a7c73c95ea2b.jpg?1783916962"
    }
}
