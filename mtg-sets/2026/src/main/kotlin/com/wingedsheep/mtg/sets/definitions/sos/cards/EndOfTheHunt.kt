package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.dsl.Targets

/**
 * End of the Hunt
 * {1}{B}
 * Sorcery
 * Target opponent exiles a creature or planeswalker they control with the greatest mana value
 * among creatures and planeswalkers they control.
 *
 * A targeted, exile-flavored "edict" restricted to the greatest-mana-value permanent. Composed from
 * the atomic pipeline (mirrors Barrin's Spite / Psychic Battle):
 *  1. [GatherCardsEffect] — gather every creature or planeswalker the target opponent controls.
 *  2. [FilterCollectionEffect] with [CollectionFilter.GreatestManaValue] — keep only those tied for
 *     the greatest mana value (a tie keeps several so the chooser can break it).
 *  3. [SelectFromCollectionEffect] with [Chooser.TargetPlayer] — the target opponent chooses exactly
 *     one of the greatest-mana-value permanents.
 *  4. [MoveCollectionEffect] — exile the chosen permanent.
 *
 * Edge cases handled for free: if the opponent controls no creatures or planeswalkers the gather is
 * empty and the spell does nothing; a single greatest-MV permanent auto-resolves; multiple ties
 * prompt the opponent to choose.
 */
val EndOfTheHunt = card("End of the Hunt") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles a creature or planeswalker they control with the greatest " +
        "mana value among creatures and planeswalkers they control."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val controlled = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.CreatureOrPlaneswalker,
                    player = opponent.asPlayer
                )
            )
            val greatestManaValue = filter(controlled, CollectionFilter.GreatestManaValue)
            val exiled = chooseExactly(
                1,
                from = greatestManaValue,
                chooser = Chooser.TargetPlayer,
                useTargetingUI = true,
                prompt = "Exile a creature or planeswalker you control with the greatest mana value."
            )
            move(exiled, CardDestination.ToZone(Zone.EXILE), moveType = MoveType.Default)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Alexandre Honoré"
        flavorText = "Just because the Oriq have gone silent doesn't mean their Mage Hunters have vanished."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0809b51a-6a05-4f18-9bf4-1b8382da648f.jpg?1775937476"
    }
}
