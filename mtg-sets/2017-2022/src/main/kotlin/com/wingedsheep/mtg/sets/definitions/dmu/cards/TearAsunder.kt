package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tear Asunder — Dominaria United #183
 * {1}{G} · Instant
 *
 * Kicker {1}{B}
 * Exile target artifact or enchantment. If this spell was kicked, exile target nonland permanent
 * instead.
 *
 * "Instead" swaps the target restriction, so the kicked cast announces a different target
 * (`kickerTarget` / `kickerEffect`, as Expel the Unworthy and Fight with Fire do).
 */
val TearAsunder = card("Tear Asunder") {
    manaCost = "{1}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Kicker {1}{B} (You may pay an additional {1}{B} as you cast this spell.)\n" +
        "Exile target artifact or enchantment. If this spell was kicked, exile target nonland permanent instead."

    keywordAbility(KeywordAbility.kicker("{1}{B}"))

    spell {
        val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Exile(artifactOrEnchantment)

        val nonland = kickerTarget(TargetFilter.NonlandPermanent)
        kickerEffect = Effects.Exile(nonland)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Dave Kendall"
        flavorText = "No one knew why the spiritmongers were so enraged by Phyrexian technology, but it was a stroke of good luck for the Coalition."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/629aa907-9533-4681-9bf2-9e56450a4cc2.jpg?1783921294"

        ruling("2022-09-09", "If Tear Asunder is kicked, it can target any nonland permanent, not just an artifact or enchantment.")
    }
}
