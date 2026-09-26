package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * The Antiquities War
 * {3}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Look at the top five cards of your library. You may reveal an artifact card from
 *          among them and put it into your hand. Put the rest on the bottom of your library
 *          in a random order.
 * III — Artifacts you control become artifact creatures with base power and toughness 5/5
 *        until end of turn.
 */
val TheAntiquitiesWar = card("The Antiquities War") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Look at the top five cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order.\n" +
        "III — Artifacts you control become artifact creatures with base power and toughness 5/5 until end of turn."

    val lookForArtifact = Effects.Pipeline {
        val looked = gather(CardSource.TopOfLibrary(5))
        val (kept, rest) = chooseUpToSplit(
            1,
            from = looked,
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card and put it into your hand",
            showAllCards = true
        )
        toHand(kept)
        toLibraryBottom(rest, order = CardOrder.Preserve)
    }

    sagaChapter(1) {
        effect = lookForArtifact
    }

    sagaChapter(2) {
        effect = lookForArtifact
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.youControl()),
            effect = Effects.BecomeCreature(
                target = EffectTarget.IterationEntity,
                power = 5,
                toughness = 5,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbda670a-00a7-419c-b4b5-bfdb323f006d.jpg?1562741999"
        ruling("2018-04-27", "The final chapter ability of The Antiquities War affects only artifacts you control at the time it resolves.")
        ruling("2018-04-27", "The final chapter doesn't remove any abilities from the artifacts that become creatures.")
    }
}
