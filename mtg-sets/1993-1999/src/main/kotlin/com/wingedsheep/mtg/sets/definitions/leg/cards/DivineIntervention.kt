package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Divine Intervention
 * {6}{W}{W}
 * Enchantment
 * This enchantment enters with two intervention counters on it.
 * At the beginning of your upkeep, remove an intervention counter from this enchantment.
 * When you remove the last intervention counter from this enchantment, the game is a draw.
 *
 * "When **you** remove" is `removedBy = Player.You`: per the ruling, the last counter coming off
 * through an opponent's effect doesn't trigger it (and nothing can afterwards).
 */
val DivineIntervention = card("Divine Intervention") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with two intervention counters on it.\n" +
        "At the beginning of your upkeep, remove an intervention counter from this enchantment.\n" +
        "When you remove the last intervention counter from this enchantment, the game is a draw."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.INTERVENTION,
            count = 2,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.RemoveCounters(CounterType.INTERVENTION, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, remove an intervention counter from this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.self.losesCounters(CounterType.INTERVENTION, lastRemoved = true, by = Player.You)
        effect = Effects.GameIsDraw()
        description = "When you remove the last intervention counter from this enchantment, the game is a draw."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eae0ba1-1383-4505-b4e7-4f17dd8f20c5.jpg?1783948087"
        ruling("2009-10-01", "When Divine Intervention's third ability resolves, the game ends immediately. The game is a draw, meaning neither player wins and neither player loses.")
        ruling("2009-10-01", "Divine Intervention's third ability triggers only if its controller removes the last intervention counter from it. It doesn't matter how that happens. For example, if you control Divine Intervention and the last intervention counter is removed as a result of a Clockspinning you control, the ability will trigger. On the other hand, if the last intervention counter is removed as a result of a Clockspinning another player controls, the ability won't trigger (and won't ever be able to).")
        ruling("2009-10-01", "In a multiplayer game played with the limited range of influence option, Divine Intervention won't necessarily end the entire game when its third ability resolves. All players within range of Divine Intervention will leave the game. They'll neither win nor lose; as far as they're concerned, the result of the game is a draw. All other players will continue playing.")
    }
}
