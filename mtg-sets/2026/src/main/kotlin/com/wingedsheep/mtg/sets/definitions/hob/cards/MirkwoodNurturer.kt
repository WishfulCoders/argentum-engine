package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirkwood Nurturer
 * {2}{G/U}
 * Creature — Elf Ranger
 * 3/2
 * When this creature enters, return up to one other target permanent you control to its owner's
 * hand. If you do, put a +1/+1 counter on this creature.
 *
 * "Up to one other target" is `optional = true` + `other()` — the controller may choose no target
 * at all, and the Nurturer itself is never a legal choice. The "if you do" rider is gated on a
 * permanent having actually been returned: gather the chosen target, move it, and branch on the
 * *moved* collection. Choosing nothing therefore skips the counter, and so does a chosen target
 * that never made it to hand.
 */
val MirkwoodNurturer = card("Mirkwood Nurturer") {
    manaCost = "{2}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Ranger"
    oracleText = "When this creature enters, return up to one other target permanent you control to its owner's hand. If you do, put a +1/+1 counter on this creature."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.PermanentYouControl.other(), optional = true)
        effect = Effects.Pipeline {
            val bounceTarget = gather(CardSource.ChosenTargets)
            val returned = moveTracked(bounceTarget, CardDestination.ToZone(Zone.HAND))
            run(Effects.If(
                condition = whenMatches(returned),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Irina Nordsol"
        flavorText = "\"Merry be the greenwood, while the world is yet young! And merry be all your folk!\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/704b45e4-566e-40f6-a33a-9151018b44e5.jpg?1785323302"
    }
}
