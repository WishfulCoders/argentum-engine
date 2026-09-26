package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Kaslem's Stonetree // Kaslem's Strider (CR 702.167, The Lost Caverns of Ixalan)
 * {2}{G}
 * Artifact // Artifact Creature — Golem
 *
 * Front face — Kaslem's Stonetree ({2}{G}, Artifact)
 *   When this artifact enters, look at the top six cards of your library. You may put
 *   a land card from among them onto the battlefield tapped. Put the rest on the
 *   bottom in a random order.
 *   Craft with Cave {5}{G}
 *
 * Back face — Kaslem's Strider (Artifact Creature — Golem, 5/5)
 *   Vanilla.
 *
 * Implementation:
 *  - The ETB trigger is the Gather → Select → Move library pipeline (Freestrider
 *    Lookout's shape): [GatherCardsEffect] over the top six, an optional filtered
 *    [SelectFromCollectionEffect] ([SelectionMode.ChooseUpTo] 1 land), the pick moved
 *    to the battlefield [ZonePlacement.Tapped], the rest to the bottom of the library
 *    in [CardOrder.Random] order.
 *  - The `craft(...)` helper wires the activated ability: [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
 *    with an exactly-one Cave material filter (`minCount = maxCount = 1`; a Cave you
 *    control or a Cave card in your graveyard, CR 702.167a-b) plus the {5}{G} mana
 *    cost, resolving via
 *    [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect]
 *    (return transformed as the back face). Sorcery-only timing is enforced by the
 *    helper.
 */

private val CaveFilter: GameObjectFilter = GameObjectFilter.Any.withSubtype("Cave")

private val KaslemsStonetreeFront = card("Kaslem's Stonetree") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, look at the top six cards of your library. You may put a land card from among them onto the battlefield tapped. Put the rest on the bottom in a random order.\n" +
        "Craft with Cave {5}{G} ({5}{G}, Exile this artifact, Exile a Cave you control or a Cave card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    // ETB: look at the top six, optionally put a land onto the battlefield tapped,
    // rest to the bottom of the library in a random order.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card onto the battlefield tapped",
                showAllCards = true
            )
            move(kept, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    craft(filter = CaveFilter, cost = "{5}{G}", materialDescription = "Cave", minCount = 1, maxCount = 1)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78b1b412-228a-4e05-a4b3-8159ebf54dc6.jpg?1782694451"
    }
}

private val KaslemsStrider = card("Kaslem's Strider") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Artifact Creature — Golem"
    power = 5
    toughness = 5
    oracleText = ""

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Victor Adame Minguez"
        flavorText = "The Oltec long ago abandoned the caverns to the mycoids, but the Deep Gods still keep watch over the spore-strewn darkness."
        imageUri = "https://cards.scryfall.io/normal/back/7/8/78b1b412-228a-4e05-a4b3-8159ebf54dc6.jpg?1782694451"
    }
}

val KaslemsStonetree: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = KaslemsStonetreeFront,
    backFace = KaslemsStrider
)
