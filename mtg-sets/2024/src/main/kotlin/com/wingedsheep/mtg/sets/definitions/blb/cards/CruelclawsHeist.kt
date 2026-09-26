package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruelclaw's Heist
 * {B}{B}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Target opponent reveals their hand. You choose a nonland card from it.
 * Exile that card. If the gift was promised, you may cast that card for as
 * long as it remains exiled, and mana of any type can be spent to cast it.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (exile to exile zone),
 * Mode 2 = gift (opponent draws, exile with permanent cast-from-exile permission).
 */
val CruelclawsHeist = card("Cruelclaw's Heist") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\n" +
        "Target opponent reveals their hand. You choose a nonland card from it. Exile that card. If the gift was promised, you may cast that card for as long as it remains exiled, and mana of any type can be spent to cast it."

    // Common pipeline for both modes: reveal hand, choose nonland, exile
    fun revealChooseExile(opponent: EffectTarget) = Patterns.Hand.revealHandAndExileChosen(target = opponent)

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — reveal, choose nonland, exile (can't cast it)
            mode("Don't promise a gift — exile a nonland card from target opponent's hand") {
                val opponent = target(Targets.Opponent)
                effect = revealChooseExile(opponent)
            },
            // Mode 2: Gift a card — opponent draws, then reveal, choose nonland, exile
            //         with permanent cast-from-exile permission
            mode("Promise a gift — an opponent draws a card, then exile a nonland card from target opponent's hand (you may cast it from exile)") {
                val opponent = target(Targets.Opponent)
                effect = Effects.DrawCards(1, opponent) then
                    revealChooseExile(opponent) then
                    Effects.GrantMayPlayFromExile(
                        from = "chosenCard",
                        expiry = MayPlayExpiry.Permanent,
                        // "and mana of any type can be spent to cast it"
                        withAnyManaType = true
                    ) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cab4539a-0157-4cbe-b50f-6e2575df74e9.jpg?1721426377"
        ruling("2024-07-26", "You pay all costs and follow all timing rules for a spell cast this way. For example, if the exiled card is a sorcery, you may cast it only during your main phase while the stack is empty.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
    }
}
