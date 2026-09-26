package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Toph, the Blind Bandit
 * {2}{G}
 * Legendary Creature — Human Warrior Ally
 * * /3
 *
 * When Toph enters, earthbend 2. (Target land you control becomes a 0/0 creature with haste
 * that's still a land. Put two +1/+1 counters on it. When it dies or is exiled, return it to
 * the battlefield tapped.)
 * Toph's power is equal to the number of +1/+1 counters on lands you control.
 *
 * Power is a characteristic-defining ability (printed *), recomputed continuously via
 * `dynamicPower(...)` as the total +1/+1 counters among lands you control (SUM over
 * the +1/+1 counters on each land). Toughness is the printed fixed value 3.
 */
val TophTheBlindBandit = card("Toph, the Blind Bandit") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warrior Ally"
    dynamicPower(
        DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land,
        ).totalCounters(CounterType.PLUS_ONE_PLUS_ONE),
    )
    toughness = 3
    oracleText = "When Toph enters, earthbend 2. (Target land you control becomes a 0/0 creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.)\n" +
        "Toph's power is equal to the number of +1/+1 counters on lands you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(2, land)
        description = "When Toph enters, earthbend 2."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Yueko"
        flavorText = "\"You think you're so tough?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff68fa7b-8065-407b-a8b4-bfbb14f1c99c.jpg?1764121345"
    }
}
