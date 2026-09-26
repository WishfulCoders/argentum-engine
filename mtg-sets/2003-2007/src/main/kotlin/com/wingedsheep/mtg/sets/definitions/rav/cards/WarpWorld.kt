package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Warp World — Ravnica: City of Guilds #150
 * {5}{R}{R}{R} · Sorcery
 *
 * Each player shuffles all permanents they own into their library, then reveals that many cards
 * from the top of their library. Each player puts all artifact, creature, and land cards revealed
 * this way onto the battlefield, then does the same for enchantment cards, then puts all cards
 * revealed this way that weren't put onto the battlefield on the bottom of their library.
 *
 * The phases interleave *across* players — every player's artifacts, creatures and lands enter
 * together, and only then every player's enchantments (so an Aura can enchant a creature Warp
 * World just put onto the battlefield). So the per-player half — count, shuffle, reveal, sort —
 * runs in a [ForEachPlayerCollectingEffect] whose sorted piles are appended into shared
 * collections, and the two battlefield entries run once each, afterwards, over everyone's cards:
 *
 * 1. Per player: gather every permanent they **own** (tokens included — each token still earns a
 *    card, the ruling), shuffle the cards into their library and put the tokens (which are about
 *    to cease to exist, CR 111.7) on the bottom, then reveal that many cards from the top. Tokens
 *    are never revealed cards, so a small library that reaches them filters them out.
 * 2. The revealed cards split into artifact/creature/land, enchantment, and the rest; the rest
 *    go to the bottom in an order the owner picks.
 * 3. All artifact/creature/land cards enter under their owners' control, then all enchantments;
 *    each owner picks what their Auras enchant (CR 303.4f).
 * 4. An Aura with nothing to enchant stays where it is (CR 303.4g) — the top of its owner's
 *    library — so the enchantments that did *not* move are put on the bottom (owner-routed,
 *    CR 400.3).
 *
 * An Aura can't enchant an enchantment entering alongside it (the ruling): `MoveCollection`
 * excludes every card of the batch from its Auras' host choices.
 */
val WarpWorld = card("Warp World") {
    manaCost = "{5}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles all permanents they own into their library, then reveals " +
        "that many cards from the top of their library. Each player puts all artifact, creature, " +
        "and land cards revealed this way onto the battlefield, then does the same for enchantment " +
        "cards, then puts all cards revealed this way that weren't put onto the battlefield on the " +
        "bottom of their library."

    val artifactCreatureOrLand =
        GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land

    spell {
        effect = Effects.Pipeline {
            val (warpAcl, warpEnchantments) = forEachPlayerCollecting(Player.Each) {
                val owned = gather(CardSource.BattlefieldMatching(GameObjectFilter.Any.ownedByYou(), Player.Each))
                val (ownedTokens, ownedCards) = filterSplit(owned, GameObjectFilter.Any.token())
                move(ownedCards, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
                toLibraryBottom(ownedTokens, order = CardOrder.Preserve)
                val revealed = gather(CardSource.TopOfLibrary(owned.count), revealed = true)
                val revealedCards = filter(revealed, GameObjectFilter.Any.nontoken())
                val (acl, notAcl) = filterSplit(revealedCards, artifactCreatureOrLand)
                val (enchantments, rest) = filterSplit(notAcl, GameObjectFilter.Enchantment)
                toLibraryBottom(rest)
                listOf(acl, enchantments)
            }
            move(warpAcl, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            move(warpEnchantments, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            // Read where each enchantment actually is rather than a moved-set: an Aura's enchant
            // choice pauses the move, and whatever didn't make it is simply not on the battlefield.
            val (_, warpStranded) = filterSplit(warpEnchantments, GameObjectFilter.Any.onBattlefield())
            toLibraryBottom(warpStranded, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdbf743a-6e28-47c4-acfe-1c5d42f80eee.jpg?1783943644"
    }
}

