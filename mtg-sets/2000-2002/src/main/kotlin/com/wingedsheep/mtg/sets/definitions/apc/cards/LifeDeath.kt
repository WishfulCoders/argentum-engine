package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Life // Death (APC #130) — split-layout spell (CR 709).
 *
 * Life {G} — Sorcery
 *   All lands you control become 1/1 creatures until end of turn. They're still lands.
 *
 * Death {1}{B} — Sorcery
 *   Return target creature card from your graveyard to the battlefield. You lose life equal to its
 *   mana value.
 *
 * Life animates the lands you control as it resolves (CR 611.2c — a land played afterwards is not
 * affected), Thelonite Druid's `ForEachInGroup` + `BecomeCreature` shape, which keeps the land types
 * and abilities. Death reads the returned card's mana value after the move; if the target is gone on
 * resolution the spell fizzles and no life is lost.
 */
val LifeDeath = card("Life // Death") {
    layout = CardLayout.SPLIT
    colorIdentity = "BG"

    face("Life") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "All lands you control become 1/1 creatures until end of turn. They're still lands."

        spell {
            effect = Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Land.youControl()),
                effect = Effects.BecomeCreature(
                    target = EffectTarget.IterationEntity,
                    power = 1,
                    toughness = 1,
                    duration = Duration.EndOfTurn,
                ),
            )
        }
    }

    face("Death") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery"
        oracleText = "Return target creature card from your graveyard to the battlefield. " +
            "You lose life equal to its mana value."

        spell {
            val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard) then
                Effects.LoseLife(DynamicAmounts.manaValueOf(creatureCard), EffectTarget.Controller)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Anthony S. Waters & Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ab75cdb-93a1-4f78-b404-37566295c321.jpg?1783945328"
        ruling("2022-12-08", "A noncreature permanent that becomes a creature can attack, and its {T} abilities can be activated, only if its controller has continuously controlled that permanent since the beginning of their most recent turn. It doesn't matter how long the permanent has been a creature. Notably, if you turn a land into a creature on the turn it entered the battlefield, you won't be able to tap it for mana.")
    }
}
