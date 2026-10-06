package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Omnath, Locus of Creation — Zendikar Rising #232
 * {R}{G}{W}{U} · Legendary Creature — Elemental · 4 / 4
 *
 * When Omnath enters, draw a card.
 * Landfall — Whenever a land you control enters, you gain 4 life if this is the first time this
 * ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. If it's the third
 * time, Omnath deals 4 damage to each opponent and each planeswalker you don't control.
 *
 * The landfall ability counts its own resolutions the Harvestrite Host / Soulbright Flamekin way:
 * [IncrementAbilityResolutionCountEffect] first, then one [Conditions.SourceAbilityResolvedNTimes]
 * branch per printed count — so a fourth and later resolution does nothing (ruling). The mana is
 * added by a non-mana ability: it always uses the stack (ruling). The third branch is Goblin
 * Chainwhirler's "each opponent and each … they control" damage; in a two-player game a
 * planeswalker you don't control is one an opponent controls.
 */
val OmnathLocusOfCreation = card("Omnath, Locus of Creation") {
    manaCost = "{R}{G}{W}{U}"
    colorIdentity = "WURG"
    typeLine = "Legendary Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "When Omnath enters, draw a card.\n" +
        "Landfall — Whenever a land you control enters, you gain 4 life if this is the first time this " +
        "ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. If it's the third " +
        "time, Omnath deals 4 damage to each opponent and each planeswalker you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(1),
                then = Effects.GainLife(4)
            ) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.AddMana(Color.RED) then
                    Effects.AddMana(Color.GREEN) then
                    Effects.AddMana(Color.WHITE) then
                    Effects.AddMana(Color.BLUE)
            ) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(3),
                then = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Patterns.Group.dealDamageToAll(4, GroupFilter(GameObjectFilter.Planeswalker.opponentControls()))
            )
        description = "Landfall — Whenever a land you control enters, you gain 4 life if this is the first " +
            "time this ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. If it's " +
            "the third time, Omnath deals 4 damage to each opponent and each planeswalker you don't control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "232"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e4fb50c-a81f-44d3-93c5-fa9a0b37f617.jpg?1783929320"

        ruling("2020-09-25", "Omnath's landfall ability has no effect each time beyond the third it resolves in a turn.")
        ruling(
            "2020-09-25",
            "Omnath's landfall ability always uses the stack and players may respond to it. It isn't a mana " +
                "ability because the event that causes it to trigger isn't a mana ability, even if it's the second " +
                "time the ability has resolved in a turn."
        )
        ruling(
            "2024-11-08",
            "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land."
        )
    }
}
