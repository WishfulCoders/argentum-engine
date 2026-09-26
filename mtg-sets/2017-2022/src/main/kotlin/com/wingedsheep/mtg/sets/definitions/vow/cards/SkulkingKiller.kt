package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skulking Killer — Innistrad: Crimson Vow #130
 * {3}{B} · Creature — Vampire Assassin · Uncommon · 4/2
 * Artist: Alex Brock
 *
 * When this creature enters, target creature an opponent controls gets -2/-2 until end of turn if
 * that opponent controls no other creatures.
 *
 * A targeted ETB trigger with a **resolution-time** intervening condition (the "if" is checked as
 * the ability resolves, not as it triggers, so a [Effects.If] rather than a
 * `interveningIf`). "That opponent controls no other creatures" is expressed as: the target's
 * controller controls exactly one creature — the target itself — via
 * `AggregateBattlefield(Player.ControllerOf("target creature"), Creature) == 1`. If they control any
 * other creature the count is ≥ 2 and the -2/-2 does not apply, matching "gets -2/-2 … if that
 * opponent controls no other creatures."
 */
val SkulkingKiller = card("Skulking Killer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Assassin"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, target creature an opponent controls gets -2/-2 until " +
        "end of turn if that opponent controls no other creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.battlefield(
                    Player.ControllerOf("target creature an opponent controls"),
                    GameObjectFilter.Creature
                ).count(),
                ComparisonOperator.EQ,
                1
            ),
            then = Effects.ModifyStats(-2, -2, creature)
        )
        description = "When this creature enters, target creature an opponent controls gets -2/-2 " +
            "until end of turn if that opponent controls no other creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Alex Brock"
        flavorText = "Olivia set a few villagers loose in the hedge maze as part of the evening's " +
            "\"entertainment.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/497b94b9-c5d7-4e94-87c2-ca1342588d79.jpg?1783924852"
    }
}
