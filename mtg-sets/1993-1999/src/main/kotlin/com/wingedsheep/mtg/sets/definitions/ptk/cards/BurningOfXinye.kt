package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burning of Xinye
 * {4}{R}{R}
 * Sorcery
 * You destroy four lands you control, then target opponent destroys four lands they control.
 * Then Burning of Xinye deals 4 damage to each creature.
 *
 * Each player picks their own lands: the controller first, then the target opponent
 * ([Chooser.TargetPlayer]). Destruction, not sacrifice — regeneration/indestructible apply.
 */
val BurningOfXinye = card("Burning of Xinye") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You destroy four lands you control, then target opponent destroys four lands they control. Then Burning of Xinye deals 4 damage to each creature."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val mine = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land))
            destroy(chooseExactly(4, mine, prompt = "Choose four lands you control to destroy", useTargetingUI = true))
            val theirs = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Land))
            destroy(
                chooseExactly(
                    4, theirs, chooser = Chooser.TargetPlayer,
                    prompt = "Choose four lands you control to destroy", useTargetingUI = true
                )
            )
        } then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.DealDamage(4, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Yang Hong"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33a1fe45-52d2-4c50-bedc-eee156ab69c8.jpg?1783946108"
    }
}
