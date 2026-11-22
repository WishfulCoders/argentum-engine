package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Stolen Strategy
 * {4}{R}
 * Enchantment
 *
 * At the beginning of your upkeep, exile the top card of each opponent's library. Until end of
 * turn, you may cast spells from among those exiled cards, and you may spend mana as though it
 * were mana of any color to cast those spells.
 *
 * The Etali, Primal Storm gather (top card of each opponent's library, one collection) feeding the
 * Curse of Hospitality grant: an end-of-turn may-play permission with `withAnyManaColor` — every
 * colored pip relaxed, while a `{C}` pip still needs colorless mana (CR 609.4b: colorless is a
 * type, not a color).
 * "Cast spells" is `nonLandOnly` — an exiled land can't be played. Normal timing still applies and
 * a card cast leaves exile, so it can't be cast twice (2018-06-08 rulings).
 */
val StolenStrategy = card("Stolen Strategy") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, exile the top card of each opponent's library. Until end of turn, you may cast spells from among those exiled cards, and you may spend mana as though it were mana of any color to cast those spells."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val stolen = gather(
                CardSource.TopOfLibrary(
                    count = 1,
                    player = Player.EachOpponent
                )
            )
            exile(stolen)
            run(
                Effects.GrantMayPlayFromExile(
                    from = stolen,
                    expiry = MayPlayExpiry.EndOfTurn,
                    withAnyManaColor = true,
                    nonLandOnly = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb90e4b8-8f66-45f0-a457-a081f2ff231c.jpg?1783934855"
        ruling("2018-06-08", "Stolen Strategy doesn't change when you can cast the exiled card. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty.")
        ruling("2018-06-08", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
        ruling("2018-06-08", "If you don't cast the exiled cards, they remain exiled. They can't be cast on later turns.")
    }
}
