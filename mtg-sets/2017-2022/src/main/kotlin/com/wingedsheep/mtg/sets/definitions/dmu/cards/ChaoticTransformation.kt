package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chaotic Transformation
 * {5}{R}
 * Sorcery
 * Exile up to one target artifact, up to one target creature, up to one target enchantment, up to
 * one target planeswalker, and/or up to one target land. For each permanent exiled this way, its
 * controller reveals cards from the top of their library until they reveal a card that shares a
 * card type with it, puts that card onto the battlefield, then shuffles.
 *
 * - Every chosen target is exiled by one move (ruling: "exiled all at the same time"). That move
 *   records each permanent as it last existed on the battlefield, and the loop's
 *   `IterationEntity` reads that record — so an animated land still shares Creature (its card in
 *   exile is only a Land), and a token is compared by its battlefield types even if it has ceased
 *   to exist; a stolen permanent's "controller" is the player who controlled it, not its owner
 *   (CR 608.2h).
 * - Players act in APNAP order (`ForEachPlayer(ActivePlayerFirst)`); each one handles the exiled
 *   permanents they controlled, read by their last-known controller (`youControl()` on a card
 *   that has left the battlefield). A permanent whose exile was replaced isn't "exiled this way",
 *   which is what `currentlyIn(EXILE)` checks.
 * - Deviation: the second ruling lets a player who controlled several of the exiled permanents
 *   choose the order they're considered in. The engine has no "order these objects, then act on
 *   each" step, so they're processed in target order (artifact, creature, enchantment,
 *   planeswalker, land). Each pass ends in a shuffle, so the order rarely matters.
 * - With no matching card, the whole library is revealed and shuffled and nothing is put onto the
 *   battlefield.
 */
val ChaoticTransformation = card("Chaotic Transformation") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile up to one target artifact, up to one target creature, up to one target " +
        "enchantment, up to one target planeswalker, and/or up to one target land. For each " +
        "permanent exiled this way, its controller reveals cards from the top of their library " +
        "until they reveal a card that shares a card type with it, puts that card onto the " +
        "battlefield, then shuffles."

    spell {
        target(TargetFilter.Artifact, optional = true)
        target(TargetFilter.Creature, optional = true)
        target(TargetFilter.Enchantment, optional = true)
        target(TargetFilter.Planeswalker, optional = true)
        target(TargetFilter.Land, optional = true)
        effect = Effects.Pipeline {
            exile(gather(CardSource.ChosenTargets))
            run(Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.Pipeline {
                val theirs = filter(
                    gather(CardSource.ChosenTargets),
                    GameObjectFilter.Any.youControl().currentlyIn(Zone.EXILE),
                )
                run(Effects.ForEachInCollection(theirs, Effects.Pipeline {
                    val (found, revealed) = gatherUntilMatch(
                        GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.IterationEntity),
                    )
                    reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
                    run(Effects.ShuffleLibrary(EffectTarget.Controller))
                }))
            }))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdfb44f1-e366-437a-bde3-59bfdb362864.jpg?1783921321"
        ruling("2022-09-09", "The target permanents are exiled all at the same time.")
        ruling(
            "2022-09-09",
            "If a player controls multiple permanents that were exiled this way, that player first " +
                "chooses which target they're considering, then they reveal cards until they reveal a " +
                "card that shares a type with that permanent as it last existed on the battlefield, " +
                "then they put that card onto the battlefield and shuffle. They then repeat this " +
                "process for the remaining targets they controlled."
        )
    }
}
