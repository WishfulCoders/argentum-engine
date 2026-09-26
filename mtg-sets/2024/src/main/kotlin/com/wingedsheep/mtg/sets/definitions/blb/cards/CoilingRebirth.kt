package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coiling Rebirth {3}{B}{B}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 * Return target creature card from your graveyard to the battlefield.
 * Then if the gift was promised and that creature isn't legendary,
 * create a token that's a copy of that creature, except it's 1/1.
 */
val CoilingRebirth = card("Coiling Rebirth") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nReturn target creature card from your graveyard to the battlefield. Then if the gift was promised and that creature isn't legendary, create a token that's a copy of that creature, except it's 1/1."

    fun returnToBattlefield(card: EffectTarget) = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — return target creature card from graveyard to battlefield
            mode("Don't promise a gift — return target creature card from your graveyard to the battlefield") {
                val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
                effect = returnToBattlefield(creatureCard)
            },
            // Mode 2: Gift a card — opponent draws, return creature, then if nonlegendary create 1/1 copy
            mode("Promise a gift — opponent draws a card, return target creature card from your graveyard to the battlefield, then if it isn't legendary create a 1/1 token copy") {
                val creatureInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ChosenOpponent)) then
                    returnToBattlefield(creatureInYourGraveyard) then
                    Effects.If(
                        condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.nonlegendary(), creatureInYourGraveyard),
                        then = Effects.CreateTokenCopyOfTarget(
                            creatureInYourGraveyard,
                            overridePower = 1,
                            overrideToughness = 1
                        )
                    ) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96d5de3e-0440-4dd1-899c-ab40c0752343.jpg?1721426359"

        ruling("2024-07-26", "Any \"enters\" abilities of the copied creature will trigger when the token enters. Any \"as [this creature] enters\" or \"[this creature] enters with\" abilities of the copied creature will also work.")
        ruling("2024-07-26", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
