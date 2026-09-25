package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodroot Apothecary — Bloomburrow Commander #27
 * {2}{G} · Creature — Squirrel Druid · 3/3
 *
 * Toxic 2
 * When this creature enters, you and target opponent each create a Treasure token.
 * Whenever an opponent sacrifices a noncreature token, that player gets two poison counters.
 *
 * The gifted Treasure is the bait: an opponent who cracks it (or a Food, Clue, Blood, …) takes
 * two poison. "That player" is the sacrificing opponent, bound as the triggering player; the
 * per-permanent form fires once for each token sacrificed. If the target opponent is illegal on
 * resolution, the whole enters ability does nothing — neither player gets a Treasure.
 */
val BloodrootApothecary = card("Bloodroot Apothecary") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Squirrel Druid"
    power = 3
    toughness = 3
    oracleText = "Toxic 2 (Players dealt combat damage by this creature also get two poison counters. " +
        "A player with ten or more poison counters loses the game.)\n" +
        "When this creature enters, you and target opponent each create a Treasure token.\n" +
        "Whenever an opponent sacrifices a noncreature token, that player gets two poison counters."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 2))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateTreasure() then Effects.CreateTreasure(controller = opponent)
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.sacrifices(GameObjectFilter.NoncreaturePermanent.token())
        effect = Effects.AddCounters(
            counterType = CounterType.POISON,
            count = 2,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d39e0024-9a91-49d6-bfbf-9dbf7d228687.jpg?1783910729"
        ruling(
            "2024-07-26",
            "If the target player is an illegal target as Bloodroot Apothecary's second ability tries to " +
                "resolve, it won't resolve and none of its effects will happen. No player will create a Treasure token."
        )
    }
}
