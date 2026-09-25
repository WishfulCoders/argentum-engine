package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Odd Acorn Gang — Bloomburrow Commander #7
 * {3}{B}{G} · Legendary Creature — Squirrel Warrior · 5/5
 *
 * Reach, menace, trample
 * Squirrels you control have "{T}: Target Squirrel gets +2/+2 and gains trample until end of
 * turn. Activate only as a sorcery."
 * Whenever one or more Squirrels you control deal combat damage to a player, draw a card.
 *
 * "Squirrels" and "target Squirrel" are bare tribal nouns, so they name Squirrel *permanents*
 * (a kindred noncreature Squirrel still gets the ability). The Gang is a Squirrel itself, so it
 * has the granted ability too. The draw is a batch
 * trigger: one card per combat-damage step however many Squirrels connect.
 */
val TheOddAcornGang = card("The Odd Acorn Gang") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Squirrel Warrior"
    power = 5
    toughness = 5
    oracleText = "Reach, menace, trample\n" +
        "Squirrels you control have \"{T}: Target Squirrel gets +2/+2 and gains trample until end of turn. " +
        "Activate only as a sorcery.\"\n" +
        "Whenever one or more Squirrels you control deal combat damage to a player, draw a card."

    keywords(Keyword.REACH, Keyword.MENACE, Keyword.TRAMPLE)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val squirrel = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SQUIRREL)))
                effect = Effects.ModifyStats(2, 2, squirrel) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, squirrel)
                timing = TimingRule.SorcerySpeed
            },
            filter = GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype(Subtype.SQUIRREL))
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Permanent.withSubtype(Subtype.SQUIRREL))
            .dealCombatDamageToAPlayer()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Omar Rayyan"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/791d9ea0-c70a-47a6-b8a3-0f5d36a5c44d.jpg?1783910735"
    }
}
