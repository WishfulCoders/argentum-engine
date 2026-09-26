package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Solstice Revelations — Avatar: The Last Airbender #153
 * {2}{R} · Instant — Lesson · Uncommon
 *
 * Exile cards from the top of your library until you exile a nonland card. You may cast that card
 * without paying its mana cost if the spell's mana value is less than the number of Mountains you
 * control. If you don't cast that card this way, put it into your hand.
 * Flashback {6}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 *
 * The same "impulse-until-nonland, free-cast-if-cheap-else-to-hand" pipeline shape as Breaching
 * Dragonstorm, composed from existing primitives:
 *   1. [GatherUntilMatchEffect] exiles from the top until a [GameObjectFilter.Nonland] card — the
 *      nonland stored as `nonland`, every revealed card (lands + the nonland) as `allRevealed`,
 *   2. move `allRevealed` to exile,
 *   3. narrow the nonland to mana value < the number of Mountains you control via
 *      [CollectionFilter.ManaValueAtMost] of `Mountains − 1` (integer "less than N" ⇔ "at most N−1")
 *      → `castable`,
 *   4. `Effects.May(CastFromCollectionWithoutPayingCost("castable"))` — you may cast it for free (no
 *      candidate, so skipped, when the mana value isn't below your Mountain count),
 *   5. of the nonland, keep only the copy still in exile ([CollectionFilter.InZone] — a cast one has
 *      moved to the stack) → `uncast`, and put it into your hand.
 * The lands exiled along the way stay in exile (only the nonland is ever moved to hand). Flashback
 * lets the card be recast from the graveyard for {6}{R}.
 */
val SolsticeRevelations = card("Solstice Revelations") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant — Lesson"
    oracleText = "Exile cards from the top of your library until you exile a nonland card. You may " +
        "cast that card without paying its mana cost if the spell's mana value is less than the " +
        "number of Mountains you control. If you don't cast that card this way, put it into your " +
        "hand.\n" +
        "Flashback {6}{R} (You may cast this card from your graveyard for its flashback cost. Then " +
        "exile it.)"

    spell {
        effect = Effects.Pipeline {
            // Exile from the top of the library until a nonland card is exiled.
            val (nonland, allRevealed) = gatherUntilMatch(GameObjectFilter.Nonland)
            exile(allRevealed)
            // Only a mana value less than the number of Mountains you control may be cast free.
            val castable = filter(
                nonland,
                GameObjectFilter.Any.manaValueAtMostDynamic(
                    DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
                    ).count() - 1
                )
            )
            // You may cast it without paying its mana cost — only prompted when there is an
            // eligible nonland (no empty "may cast" when the mana value is too high).
            ifNotEmpty(castable) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(castable)))
            }
            // If you don't cast it this way, put it into your hand. A card just cast has left
            // exile for the stack, so only the nonland still in exile moves.
            val uncast = filter(nonland, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toHand(uncast)
        }
    }

    keywordAbility(KeywordAbility.flashback("{6}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Kotakan"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f22ac19f-66fb-4d54-9f09-495a20a29577.jpg?1764121053"
    }
}
