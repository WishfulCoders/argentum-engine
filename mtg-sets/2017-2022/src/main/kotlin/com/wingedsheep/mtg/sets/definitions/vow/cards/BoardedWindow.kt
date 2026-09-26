package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Boarded Window
 * {3}
 * Artifact
 * Creatures attacking you get -1/-0.
 * At the beginning of each end step, if you were dealt 4 or more damage this turn, exile this artifact.
 *
 * "Creatures attacking you" is modeled with the engine's established idiom of opponent-controlled
 * attacking creatures (see DynamicAmounts.creaturesAttackingYou). The end-step self-exile is an
 * intervening-if trigger reading the DAMAGE_RECEIVED turn tracker for the controller.
 */
val BoardedWindow = card("Boarded Window") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Creatures attacking you get -1/-0.\n" +
        "At the beginning of each end step, if you were dealt 4 or more damage this turn, exile this artifact."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().opponentControls())
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.damageReceivedThisTurn(Player.You),
            ComparisonOperator.GTE,
            4
        )
        effect = Effects.Exile(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f02cf94c-94d3-4c4c-a7ae-b4d48ef5b14e.jpg?1782703017"
    }
}
