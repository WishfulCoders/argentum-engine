package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ertai Resurrected
 * {2}{U}{B}
 * Legendary Creature — Phyrexian Human Wizard
 * 3/2
 *
 * Flash
 * When Ertai Resurrected enters, choose up to one —
 * • Counter target spell, activated ability, or triggered ability. Its controller draws a card.
 * • Destroy another target creature or planeswalker. Its controller draws a card.
 *
 * Counter mode: the draw is resolved while the spell/ability is still on the stack so
 * [EffectTarget.TargetController] reads its controller (as Undermine does) — countering first
 * removes the stack object. Both happen in one resolution, and the draw doesn't depend on the
 * counter succeeding (a can't-be-countered spell's controller still draws).
 *
 * Destroy mode: the draw follows the destroy and reads the creature's last-known controller
 * (CR 608.2h), so it still happens if the permanent regenerates or is indestructible.
 */
val ErtaiResurrected = card("Ertai Resurrected") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Phyrexian Human Wizard"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "When Ertai Resurrected enters, choose up to one —\n" +
        "• Counter target spell, activated ability, or triggered ability. Its controller draws a card.\n" +
        "• Destroy another target creature or planeswalker. Its controller draws a card."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Counter target spell, activated ability, or triggered ability. Its controller draws a card") {
                    target(TargetFilter.SpellOrAbilityOnStack)
                    effect = Effects.DrawCards(1, EffectTarget.TargetController) then
                        Effects.CounterSpellOrAbility()
                },
                mode("Destroy another target creature or planeswalker. Its controller draws a card") {
                    val permanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).other())
                    effect = Effects.Destroy(permanent) then
                        Effects.DrawCards(1, EffectTarget.TargetController)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f7e780e-fbc5-4dc0-b5c7-efcb8645c7c6.jpg?1783921288"
    }
}
