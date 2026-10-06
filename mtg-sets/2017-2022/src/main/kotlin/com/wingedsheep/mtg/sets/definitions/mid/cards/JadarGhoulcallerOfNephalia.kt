package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jadar, Ghoulcaller of Nephalia
 * {1}{B}
 * Legendary Creature — Human Wizard
 * 1/1
 *
 * At the beginning of your end step, if you control no creatures with decayed, create a 2/2 black
 * Zombie creature token with decayed.
 *
 * The intervening-if (CR 603.4) is checked both when the end step begins and on resolution. "Creatures
 * with decayed" reads the projected [Keyword.DECAYED], which the state projector grants to any
 * permanent carrying a [CounterType.DECAYED] counter — the same spelling Ghoulish Procession uses for
 * its token, so Jadar's own Zombies are seen by the condition.
 */
val JadarGhoulcallerOfNephalia = card("Jadar, Ghoulcaller of Nephalia") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your end step, if you control no creatures with decayed, create a " +
        "2/2 black Zombie creature token with decayed. (It can't block. When it attacks, sacrifice it at " +
        "end of combat.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.withKeyword(Keyword.DECAYED),
            negate = true,
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            initialCounters = mapOf(CounterType.DECAYED to 1),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
        description = "At the beginning of your end step, if you control no creatures with decayed, create a " +
            "2/2 black Zombie creature token with decayed."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Yongjae Choi"
        flavorText = "\"Rise, my pretty thing. Why rot in the river when you can serve at my bidding?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff971ba7-68b8-482a-9cb1-741f6893550c.jpg?1783925615"
        ruling("2021-09-24", "Decayed does not grant haste. Creatures with decayed that enter the battlefield during your turn may not attack until your next turn.")
        ruling("2021-09-24", "Once a creature with decayed attacks, it will be sacrificed at end of combat, even if it no longer has decayed at that time.")
    }
}
