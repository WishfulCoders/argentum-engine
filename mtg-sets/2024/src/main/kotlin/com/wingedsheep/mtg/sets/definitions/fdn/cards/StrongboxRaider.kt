package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Strongbox Raider
 * {2}{R}{R}
 * Creature — Orc Pirate
 * 5/2
 * Raid — When this creature enters, if you attacked this turn, exile the top two cards of your
 * library. Choose one of them. Until the end of your next turn, you may play that card.
 *
 * Raid is an intervening-if ETB trigger ([Conditions.YouAttackedThisTurn], CR 603.4). The impulse
 * half is the standard Gather → Move(EXILE) → Select(one) → grant may-play pipeline (see Riverwheel
 * Sweep); the non-chosen exiled card stays in exile with no play permission, matching "Choose one of
 * them," and the chosen card is playable until the end of the controller's next turn.
 */
val StrongboxRaider = card("Strongbox Raider") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Pirate"
    power = 5
    toughness = 2
    oracleText = "Raid — When this creature enters, if you attacked this turn, exile the top two cards " +
        "of your library. Choose one of them. Until the end of your next turn, you may play that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2))
            exile(exiled)
            val chosen = chooseExactly(
                1,
                from = exiled,
                prompt = "Choose a card you may play until the end of your next turn"
            )
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.UntilEndOfNextTurn
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Craig J Spearing"
        flavorText = "\"Tonight, we feast!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2223eb8-59f9-489b-a3f3-b6496218cb79.jpg?1782689184"
    }
}
