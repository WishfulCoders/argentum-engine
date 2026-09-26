package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fight Rigging
 * {2}{G}
 * Enchantment
 *
 * Hideaway 5 (When this enchantment enters, look at the top five cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.
 * Then if you control a creature with power 7 or greater, you may play the exiled card without
 * paying its mana cost.
 *
 * Hideaway is modeled compositionally, same shape as Mosswort Bridge / Evercoat Ursine: the ETB
 * trigger gathers the top five cards, prompts the controller to exile one face down (linked to
 * this enchantment), and bottom-randomizes the rest. The beginning-of-combat trigger targets a
 * creature you control for the mandatory +1/+1 counter, then gates the immediate free cast behind
 * [Effects.If] testing [Conditions.YouControlAtLeast] on a power-7-or-greater creature —
 * only the "may play" half is conditional; the counter always happens if there's a legal target.
 */
val FightRigging = card("Fight Rigging") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Hideaway 5 (When this enchantment enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "At the beginning of combat on your turn, put a +1/+1 counter on target creature you " +
        "control. Then if you control a creature with power 7 or greater, you may play the " +
        "exiled card without paying its mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val fightRiggingTop = gather(
                CardSource.TopOfLibrary(
                    count = 5,
                    player = Player.You
                )
            )
            val (fightRiggingPicked, fightRiggingRest) = chooseExactlySplit(
                1,
                from = fightRiggingTop,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            exile(fightRiggingPicked, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            toLibraryBottom(fightRiggingRest, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t) then
            Effects.If(
                condition = Conditions.YouControlAtLeast(1, GameObjectFilter.Creature.powerAtLeast(7)),
                then = Effects.May(
                    Effects.Pipeline {
                        val fightRiggingLinked = gather(CardSource.FromLinkedExile())
                        run(Effects.PlayFromCollectionWithoutPayingCost(fightRiggingLinked))
                    },
                    descriptionOverride = "Play the exiled card without paying its mana cost"
                )
            )
        description = "At the beginning of combat on your turn, put a +1/+1 counter on target " +
            "creature you control. Then if you control a creature with power 7 or greater, you " +
            "may play the exiled card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86fd3454-efe7-4feb-a6ad-069ae2fdbd18.jpg?1783923103"
        ruling(
            "2022-04-29",
            "\"Hideaway N\" means \"When this permanent enters the battlefield, look at the top N " +
                "cards of your library. Exile one of them face down and put the rest on the bottom " +
                "of your library in a random order. The exiled card gains 'The player who controls " +
                "the permanent that exiled this card may look at this card in the exile zone.'\""
        )
        ruling(
            "2022-04-29",
            "Any player who has controlled a permanent with a hideaway ability since a card was " +
                "exiled with it may look at that card."
        )
        ruling(
            "2022-04-29",
            "Hideaway now causes you to put the rest of the cards on the bottom of your library " +
                "in a random order instead of any order."
        )
    }
}
