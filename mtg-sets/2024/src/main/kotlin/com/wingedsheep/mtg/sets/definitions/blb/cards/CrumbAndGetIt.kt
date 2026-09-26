package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crumb and Get It
 * {W}
 * Instant
 *
 * Gift a Food (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a Food token before its other effects.
 * It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 *
 * Target creature you control gets +2/+2 until end of turn.
 * If the gift was promised, that creature also gains indestructible until end of turn.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (+2/+2),
 * Mode 2 = gift (Food for opponent + +2/+2 + indestructible).
 */
val CrumbAndGetIt = card("Crumb and Get It") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Gift a Food (You may promise an opponent a gift as you cast this spell. If you do, they create a Food token before its other effects. It's an artifact with \"{2}, {T}, Sacrifice this artifact: You gain 3 life.\")\nTarget creature you control gets +2/+2 until end of turn. If the gift was promised, that creature also gains indestructible until end of turn."

    fun pump(creature: EffectTarget) = Effects.ModifyStats(2, 2, creature)

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — +2/+2 until end of turn
            mode("Don't promise a gift — target creature you control gets +2/+2 until end of turn") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = pump(creatureYouControl)
            },
            // Mode 2: Gift a Food — opponent creates Food, +2/+2 and indestructible until end of turn
            mode("Promise a gift — opponent creates a Food token, target creature you control gets +2/+2 and gains indestructible until end of turn") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = Effects.CreateFood(1, EffectTarget.PlayerRef(Player.ChosenOpponent)) then
                    pump(creatureYouControl) then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creatureYouControl) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c7b3b25-d4b3-4451-9f5c-6eb369541175.jpg?1721429540"

        ruling("2024-07-26", "As an additional cost to cast a spell with gift, you can promise the listed gift to an opponent. That opponent is chosen as part of that additional cost.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
