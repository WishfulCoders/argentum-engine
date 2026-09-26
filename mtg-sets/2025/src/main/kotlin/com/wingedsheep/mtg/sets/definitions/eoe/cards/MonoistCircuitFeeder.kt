package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monoist Circuit-Feeder
 * {4}{B}{B}
 * Artifact Creature — Nautilus
 * Flying
 * When this creature enters, until end of turn, target creature you control gets +X/+0 and target creature an opponent controls gets -0/-X, where X is the number of artifacts you control.
 * 4/4
 */
val MonoistCircuitFeeder = card("Monoist Circuit-Feeder") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Nautilus"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, until end of turn, target creature you control gets +X/+0 and target creature an opponent controls gets -0/-X, where X is the number of artifacts you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()

        val ally = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val enemy = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))

        val artifactCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()

        effect = Effects.ModifyStats(artifactCount, DynamicAmounts.fixed(0), ally) then
            Effects.ModifyStats(DynamicAmounts.fixed(0), 0 - artifactCount, enemy)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Quintin Gleim"
        flavorText = "It does to ships what the Monoist faith does to stars."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/957ae7aa-98d6-402a-9b20-e3e5b7e8dfe3.jpg?1752947001"
    }
}
