package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dark Tutelage
 * {2}{B}
 * Enchantment
 * At the beginning of your upkeep, reveal the top card of your library and put that card into your
 * hand. You lose life equal to its mana value.
 *
 * Dark Confidant's upkeep trigger on an enchantment.
 */
val DarkTutelage = card("Dark Tutelage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, reveal the top card of your library and put that card into your hand. You lose life equal to its mana value."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                    storeAs = "revealed"
                ),
                MoveCollectionEffect(
                    from = "revealed",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You),
                    revealed = true
                ),
                LoseLifeEffect(
                    DynamicAmount.StoredCardManaValue("revealed"),
                    EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "James Ryman"
        flavorText = "\"It is a rough road that leads to the heights of greatness.\"\n—Seneca, *Epistles*, trans. Gummere"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ec3dd9f-3969-4fd7-97f7-e9868eb19a64.jpg?1783941817"
        ruling("2010-08-15", "The mana value of the revealed card is determined solely by the mana symbols printed in its upper right corner. The mana value is the total amount of mana in that cost, regardless of color. For example, a card with mana cost {3}{U}{U} has mana value 5.")
        ruling("2010-08-15", "If the mana cost of the revealed card includes {X}, X is considered to be 0.")
        ruling("2010-08-15", "If the revealed card has no mana symbols in its upper right corner (because it's a land card, for example), its mana value is 0.")
    }
}
