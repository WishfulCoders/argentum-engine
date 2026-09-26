package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Gishath, Sun's Avatar
 * {5}{R}{G}{W}
 * Legendary Creature — Dinosaur Avatar
 * 7/6
 *
 * Vigilance, trample, haste
 * Whenever Gishath, Sun's Avatar deals combat damage to a player, reveal that many
 * cards from the top of your library. Put any number of Dinosaur creature cards
 * from among them onto the battlefield and the rest on the bottom of your library
 * in a random order.
 */
val GishathSunsAvatar = card("Gishath, Sun's Avatar") {
    manaCost = "{5}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Dinosaur Avatar"
    power = 7
    toughness = 6
    oracleText = "Vigilance, trample, haste\nWhenever Gishath, Sun's Avatar deals combat damage to a player, reveal that many cards from the top of your library. Put any number of Dinosaur creature cards from among them onto the battlefield and the rest on the bottom of your library in a random order."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val damageDealt = DynamicAmounts.triggerDamageAmount()
        effect = Effects.Pipeline {
            val gishathRevealed = gather(
                CardSource.TopOfLibrary(count = damageDealt, player = Player.You),
                revealed = true
            )
            val (gishathToBattlefield, gishathToBottom) = chooseAnyNumberSplit(
                from = gishathRevealed,
                filter = GameObjectFilter.Creature.withSubtype("Dinosaur"),
                showAllCards = true,
                prompt = "Put any number of Dinosaur creature cards onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on the bottom of your library"
            )
            move(gishathToBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toLibraryBottom(gishathToBottom, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "222"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7335e500-342d-476d-975c-817512e6e3d6.jpg?1562558022"
    }
}
