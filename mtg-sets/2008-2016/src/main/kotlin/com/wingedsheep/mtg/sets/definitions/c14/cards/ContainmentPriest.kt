package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Containment Priest — Commander 2014 #5
 * {1}{W} · Creature — Human Cleric · 2/2
 *
 * Flash
 * If a nontoken creature would enter and it wasn't cast, exile it instead.
 *
 * A [RedirectZoneChange] on battlefield entries with `notCast = true`: a permanent spell resolving
 * from the stack was cast and is untouched (also one cast from an unusual zone — ruling); a creature
 * put onto the battlefield by an effect, or a creature land *played* (Dryad Arbor, CR 305.1), is
 * exiled instead. The filter is checked against the permanent as it would exist on the battlefield
 * (CR 614.12): a noncreature card entering as a creature (March of the Machines) is exiled, a
 * creature card entering as a noncreature isn't, and a card put onto the battlefield face down is a
 * 2/2 creature (CR 708.2). The replacement only exists while the Priest is on the battlefield, so it
 * never exiles itself or creatures entering alongside it (rulings).
 */
val ContainmentPriest = card("Containment Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Flash\nIf a nontoken creature would enter and it wasn't cast, exile it instead."
    power = 2
    toughness = 2

    keywords(Keyword.FLASH)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken(),
                to = Zone.BATTLEFIELD,
                notCast = true,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "John Stanko"
        flavorText = "Some protection requires a bit of finesse."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2c794b9-09da-49be-b258-b0e21f1663e3.jpg?1783938874"
        ruling("2021-03-19", "Containment Priest's last ability won't affect any creatures that were cast, including ones cast from unusual zones such as your graveyard.")
        ruling("2021-03-19", "If a noncreature card wasn't cast and is entering the battlefield as a creature (due to an effect such as that of March of the Machines), it will be exiled. Conversely, if a creature card wasn't cast and is entering the battlefield as a noncreature permanent (for example, Heliod, Sun-Crowned with insufficient devotion), it won't be exiled.")
        ruling("2021-03-19", "If Containment Priest enters the battlefield without being cast, its ability won't exile itself.")
        ruling("2021-03-19", "If Containment Priest enters the battlefield at the same time as other creatures, its ability won't affect those creatures.")
    }
}
