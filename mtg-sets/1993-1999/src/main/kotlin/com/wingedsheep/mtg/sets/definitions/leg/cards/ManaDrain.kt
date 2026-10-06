package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mana Drain
 * {U}{U}
 * Instant
 * Counter target spell. At the beginning of your next main phase, add an amount of {C} equal to
 * that spell's mana value.
 *
 * "Your next main phase" is whichever of your main phases begins first
 * ([Effects.AtBeginningOfYourNextMainPhase]): cast during your precombat main phase or combat, the
 * mana arrives in that turn's postcombat main phase; otherwise in your next precombat main phase.
 *
 * "That spell" is gone by the time the delayed trigger fires, so the delayed trigger is created
 * **before** the counter: `CreateDelayedTriggerExecutor` snapshots the
 * `AddColorlessMana(manaValueOf(target))` amount into a literal while the spell is still on the
 * stack. If the target is illegal on resolution Mana Drain doesn't resolve at all, so no trigger is
 * created; if the spell merely can't be countered, the mana still comes (both per the rulings).
 */
val ManaDrain = card("Mana Drain") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. At the beginning of your next main phase, add an amount of {C} equal to that spell's mana value."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.AtBeginningOfYourNextMainPhase(
            Effects.AddColorlessMana(DynamicAmounts.manaValueOf(spell))
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e691adef-3027-4e6a-889f-9f4e2df36a7c.jpg?1783948074"
        ruling("2020-11-10", "If the target spell is an illegal target by the time Mana Drain tries to resolve, Mana Drain doesn't resolve. You don't add mana at the beginning of your next main phase. If the target is legal but not countered (most likely because an effect says that the spell can't be countered), you do add mana.")
        ruling("2020-11-10", "Mana Drain's delayed triggered ability will usually trigger at the beginning of your precombat main phase. However, if you cast Mana Drain during your precombat main phase or during your combat phase, its delayed triggered ability will trigger at the beginning of that turn's postcombat main phase.")
    }
}
