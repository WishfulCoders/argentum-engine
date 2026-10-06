package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Haywire Mite
 * {1}
 * Artifact Creature — Insect
 * 1/1
 * When this creature dies, you gain 2 life.
 * {G}, Sacrifice this creature: Exile target noncreature artifact or noncreature enchantment.
 *
 * "Noncreature artifact or noncreature enchantment" is the artifact-or-enchantment filter narrowed
 * by [GameObjectFilter.notCreature] (Guerrilla Gorilla's target). Sacrificing the Mite to its own
 * ability is a death, so the life-gain trigger fires alongside the exile.
 */
val HaywireMite = card("Haywire Mite") {
    manaCost = "{1}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Insect"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, you gain 2 life.\n" +
        "{G}, Sacrifice this creature: Exile target noncreature artifact or noncreature enchantment."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.SacrificeSelf)
        val victim = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment.notCreature()))
        effect = Effects.Exile(victim)
        description = "{G}, Sacrifice this creature: Exile target noncreature artifact or noncreature enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Izzy"
        flavorText = "\"They're easy enough to squish, but they're a pain to wash out of a workbench.\"\n" +
            "—Tergel, goblin explosioneer"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/847a175e-ead1-4596-baf3-5f7f57859e0b.jpg?1783920034"
    }
}
