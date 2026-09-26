package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Banshee
 * {2}{B}{B}
 * Creature — Spirit
 * 0/1
 * {X}, {T}: This creature deals half X damage, rounded down, to any target, and half X damage,
 * rounded up, to you.
 *
 * The two halves round in opposite directions, which is the whole cost of the card: at X=5 the
 * victim takes 2 and you take 3. Both are `Divide(XValue, 2)` differing only in `roundUp`, so the
 * asymmetry lives in one flag rather than in hand-computed numbers, and an odd X can never be lost
 * or double-counted.
 *
 * X is announced on activation (CR 601.2b as applied to abilities), so the amount is fixed even if
 * something changes between activation and resolution.
 */
val Banshee = card("Banshee") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 0
    toughness = 1
    oracleText = "{X}, {T}: This creature deals half X damage, rounded down, to any target, and " +
        "half X damage, rounded up, to you."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.xValue() / 2,
            victim,
        ) then
            Effects.DealDamage(
                DynamicAmounts.xValue() divRoundedUp 2,
                EffectTarget.PlayerRef(Player.You),
            )
        description = "{X}, {T}: This creature deals half X damage, rounded down, to any target, " +
            "and half X damage, rounded up, to you."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Jesper Myrfors"
        flavorText = "Some say Banshees are the hounds of Death, baying to herd their prey into " +
            "the arms of their master."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66eaa7d6-48b2-4b35-a834-790edd679e0e.jpg?1783947941"
    }
}
