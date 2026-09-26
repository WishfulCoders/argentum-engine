package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Choco, Seeker of Paradise
 * {1}{G}{W}{U}
 * Legendary Creature — Bird
 * 3/5
 *
 * Whenever one or more Birds you control attack, look at that many cards from the top of
 * your library. You may put one of them into your hand. Then put any number of land cards
 * from among them onto the battlefield tapped and the rest into your graveyard.
 * Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn.
 *
 * Modeling notes:
 *  - The attack trigger is a once-per-combat group trigger (`Triggers.you.attacks(with)`)
 *    keyed on Birds you control — it fires once no matter how many Birds attack, not once
 *    per Bird.
 *  - "Look at that many cards" — the count is the number of attacking Birds you control,
 *    read at resolution via [DynamicAmount.AggregateBattlefield] over attacking Birds. This
 *    matches the established convention for attacker-count cards (Goblin Piledriver,
 *    Shaleskin Bruiser).
 *  - The hand/battlefield/graveyard distribution is built from the atomic
 *    GatherCards → SelectFromCollection → MoveCollection pipeline (cf. Ignis Scientia,
 *    Portent of Calamity): "look at" is a private gather; the optional one-to-hand pick is a
 *    ChooseUpTo(1); the land pile is a ChooseAnyNumber filtered to lands (showAllCards so the
 *    player sees every looked-at card); everything not chosen falls through to the graveyard.
 */
val ChocoSeekerOfParadise = card("Choco, Seeker of Paradise") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Bird"
    power = 3
    toughness = 5
    oracleText = "Whenever one or more Birds you control attack, look at that many cards from the top of your library. " +
        "You may put one of them into your hand. Then put any number of land cards from among them onto the " +
        "battlefield tapped and the rest into your graveyard.\n" +
        "Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn."

    // Whenever one or more Birds you control attack, look at that many cards...
    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Creature.withSubtype(Subtype.BIRD))
        effect = Effects.Pipeline {
            // Look at that many cards from the top of your library.
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature.withSubtype(Subtype.BIRD).attacking()
                    ).count(),
                    player = Player.You
                )
            )
            // You may put one of them into your hand.
            val (toHandCards, remaining) = chooseUpToSplit(
                1,
                from = looked,
                showAllCards = true,
                prompt = "You may put one of them into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Keep among them"
            )
            toHand(toHandCards)
            // Then put any number of land cards from among them onto the battlefield tapped...
            val (toBattlefield, toGraveyardCards) = chooseAnyNumberSplit(
                from = remaining,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "Put any number of land cards onto the battlefield tapped",
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your graveyard"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped))
            // ...and the rest into your graveyard.
            toGraveyard(toGraveyardCards)
        }
    }

    // Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, Choco, Seeker of Paradise gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Miho Midorikawa"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/409c305a-52dc-4538-8e72-efcd568eaf49.jpg?1748706564"
    }
}
