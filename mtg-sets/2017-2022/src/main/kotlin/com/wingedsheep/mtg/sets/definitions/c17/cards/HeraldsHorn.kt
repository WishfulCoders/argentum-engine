package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Herald's Horn — Commander 2017 #53 (canonical printing)
 * {3} · Artifact
 *
 * As this artifact enters, choose a creature type.
 * Creature spells you cast of the chosen type cost {1} less to cast.
 * At the beginning of your upkeep, look at the top card of your library. If it's a creature card
 * of the chosen type, you may reveal it and put it into your hand.
 *
 * Gathering Stone's shape narrowed to creature cards, without the graveyard option: the looked-at
 * card is split on "creature card of the chosen type"; a match may be revealed into hand, and
 * anything not taken stays on top of the library unrevealed. With no chosen type (the Horn entered
 * some other way) `withChosenSubtype()` matches nothing, so nothing gets cheaper and nothing is
 * ever put into hand, as the ruling says.
 */
val HeraldsHorn = card("Herald's Horn") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Creature spells you cast of the chosen type cost {1} less to cast.\n" +
        "At the beginning of your upkeep, look at the top card of your library. " +
        "If it's a creature card of the chosen type, you may reveal it and put it into your hand."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withChosenSubtype()),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val matching = filter(looked, GameObjectFilter.Creature.withChosenSubtype())
            val toHand = chooseUpTo(
                1,
                from = matching,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library"
            )
            move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07b06421-778a-4d23-862b-30fc5fa25928.jpg?1783935932"
        ruling("2017-08-25", "The effect of Herald's Horn reduces only generic mana in a spell's cost. If that cost has no generic mana, the cost isn't reduced.")
        ruling("2017-08-25", "If you somehow control a Herald's Horn with no chosen creature type, no spells will cost less to cast, not even creature spells with no creature type. You'll be able to look at the top card of your library at the beginning of each of your upkeeps, but you can never put it into your hand this way, even if it's a creature card with no creature type.")
        ruling("2017-08-25", "If you don't put the top card of your library into your hand, you put it back on top of your library without revealing it. You'll draw it in that turn's draw step.")
    }
}
