package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

val TownGreeter = card("Town Greeter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, mill four cards. You may put a land card from among them into your hand. If you put a Town card into your hand this way, you gain 2 life. (To mill four cards, a player puts the top four cards of their library into their graveyard.)"

    val townFilter = GameObjectFilter.Land.withSubtype("Town")

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = gather(CardSource.TopOfLibrary(4))
            toGraveyard(milled)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "You may put a land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
            run(Effects.If(
                condition = whenMatches(selected, townFilter),
                then = Effects.GainLife(2)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Hayaken-Sarena"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49cd4efa-4df4-4257-9a42-60330f7781e2.jpg?1748706545"
    }
}
