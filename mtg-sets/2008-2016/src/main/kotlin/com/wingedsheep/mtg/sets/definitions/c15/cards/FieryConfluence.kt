package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fiery Confluence
 * {2}{R}{R} — Sorcery (Rare) — Commander 2015 #26
 * Artist: Kieran Yanner
 *
 * Choose three. You may choose the same mode more than once.
 * • Fiery Confluence deals 1 damage to each creature.
 * • Fiery Confluence deals 2 damage to each opponent.
 * • Destroy target artifact.
 *
 * Modeled via [modal] with chooseCount = 3 and allowRepeat = true; each chosen mode is its own
 * damage event (ruling 2015-11-04), and each instance of the artifact mode picks its own target.
 */
val FieryConfluence = card("Fiery Confluence") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose three. You may choose the same mode more than once.\n" +
        "• Fiery Confluence deals 1 damage to each creature.\n" +
        "• Fiery Confluence deals 2 damage to each opponent.\n" +
        "• Destroy target artifact."

    spell {
        modal(chooseCount = 3, allowRepeat = true) {
            mode(
                "Fiery Confluence deals 1 damage to each creature",
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature),
                    Effects.DealDamage(1, EffectTarget.IterationEntity)
                )
            )
            mode(
                "Fiery Confluence deals 2 damage to each opponent",
                Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
            )
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b61c9bc-16e8-417f-99e7-8bd83d4666c5.jpg?1783938111"
        ruling("2015-11-04", "If the first or second modes are chosen multiple times, each of those modes represents a separate damage-dealing event.")
        ruling("2015-11-04", "If a mode requires a target, you can select that mode only if there's a legal target available. Each time you select that mode, you can choose a different target, or you can choose the same target.")
    }
}
