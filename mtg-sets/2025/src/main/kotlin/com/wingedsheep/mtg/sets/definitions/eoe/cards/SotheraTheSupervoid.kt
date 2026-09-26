package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Step

/**
 * Sothera, the Supervoid
 * {2}{B}{B}
 * Legendary Enchantment
 *
 * Whenever a creature you control dies, each opponent chooses a creature they control and exiles it.
 * At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put
 * a creature card exiled with it onto the battlefield under your control with two additional
 * +1/+1 counters on it.
 *
 * Ability 2: Linked exile is gathered BEFORE the sacrifice so entity IDs are captured before
 * Rule 400.7 strips LinkedExileComponent from Sothera when it moves to the graveyard.
 */
val SotheraTheSupervoid = card("Sothera, the Supervoid") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "Whenever a creature you control dies, each opponent chooses a creature they control and exiles it.\n" +
        "At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put a creature card exiled with it onto the battlefield under your control with two additional +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val opponentCreatures = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.You
                    )
                )
                val chosenCreature = chooseExactly(1, from = opponentCreatures, prompt = "Choose a creature to exile")
                exile(chosenCreature, linkToSource = true)
            }
        )
        description = "Whenever a creature you control dies, each opponent chooses a creature they control and exiles it."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = AnyCondition(
            listOf(
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true),
                Exists(Player.EachOpponent, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true)
            )
        )
        effect = Effects.Pipeline {
            val exiledCreatures = gather(CardSource.FromLinkedExile())
            run(SacrificeSelfEffect)
            val chosenCreature = chooseExactly(
                1,
                from = exiledCreatures,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card to put onto the battlefield"
            )
            val returnedCreature = moveTracked(chosenCreature, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.AddCountersToCollection(
                collection = returnedCreature,
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 2
            ))
        }
        description = "At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put a creature card exiled with it onto the battlefield under your control with two additional +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e99d6fc0-dcf2-4b25-81c2-02c230a36246.jpg?1752947018"
    }
}
