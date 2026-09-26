package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aang, at the Crossroads // Aang, Destined Savior
 * {2}{G}{W}{U} — Legendary Creature — Human Avatar Ally 3/3
 * //  — Legendary Creature — Avatar Ally 4/4
 *
 * Front — Aang, at the Crossroads:
 *   Flying
 *   When Aang enters, look at the top five cards of your library. You may put a creature card
 *   with mana value 4 or less from among them onto the battlefield. Put the rest on the bottom
 *   of your library in a random order.
 *   When another creature you control leaves the battlefield, transform Aang at the beginning
 *   of the next upkeep.
 *
 * Back — Aang, Destined Savior:
 *   Flying
 *   Land creatures you control have vigilance.
 *   At the beginning of combat on your turn, earthbend 2. (Target land you control becomes a
 *   0/0 creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or
 *   is exiled, return it to the battlefield tapped.)
 */
private val AangDestinedSavior = card("Aang, Destined Savior") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Creature — Avatar Ally"
    oracleText = "Flying\n" +
        "Land creatures you control have vigilance.\n" +
        "At the beginning of combat on your turn, earthbend 2. (Target land you control becomes a " +
        "0/0 creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or " +
        "is exiled, return it to the battlefield tapped.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    // Land creatures you control have vigilance.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl())
        )
    }

    // At the beginning of combat on your turn, earthbend 2.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/back/f/e/fea89ca0-8070-4f28-9851-994314f9d248.jpg?1764522003"
    }
}

private val AangAtTheCrossroadsFront = card("Aang, at the Crossroads") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Avatar Ally"
    oracleText = "Flying\n" +
        "When Aang enters, look at the top five cards of your library. You may put a creature card " +
        "with mana value 4 or less from among them onto the battlefield. Put the rest on the bottom " +
        "of your library in a random order.\n" +
        "When another creature you control leaves the battlefield, transform Aang at the beginning " +
        "of the next upkeep."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    // When Aang enters, look at the top five cards of your library. You may put a creature card
    // with mana value 4 or less from among them onto the battlefield. Put the rest on the bottom
    // of your library in a random order.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (chosen, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature.manaValueAtMost(4),
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on bottom"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    // When another creature you control leaves the battlefield, transform Aang at the beginning
    // of the next upkeep.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).leaves()
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            effect = Effects.Transform(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fea89ca0-8070-4f28-9851-994314f9d248.jpg?1764522003"
    }
}

val AangAtTheCrossroads: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AangAtTheCrossroadsFront,
    backFace = AangDestinedSavior,
)
