package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val SWARM_TYPES = listOf(Subtype.INSECT, Subtype.RAT, Subtype.SPIDER, Subtype.SQUIRREL)

/**
 * Swarmyard Massacre — Bloomburrow Commander #20
 * {3}{B}{B} · Sorcery
 *
 * Create two 1/1 green Squirrel creature tokens. Then each creature that isn't an Insect, Rat,
 * Spider, or Squirrel gets -1/-1 until end of turn for each creature you control that's an Insect,
 * Rat, Spider, or Squirrel.
 *
 * The two Squirrels are created first, so they count. A creature with several of the listed
 * types (a Rat Squirrel) counts once — the count is over creatures, not types.
 */
val SwarmyardMassacre = card("Swarmyard Massacre") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 green Squirrel creature tokens. Then each creature that isn't an Insect, Rat, " +
        "Spider, or Squirrel gets -1/-1 until end of turn for each creature you control that's an Insect, Rat, " +
        "Spider, or Squirrel."

    spell {
        val swarm = DynamicAmounts.count(
            Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withAnyOfSubtypes(SWARM_TYPES)
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/5/a/5a6ec62e-0e9b-4312-bfe8-cc85d76fd9e0.jpg?1783909765"
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.notAnyOfSubtypes(SWARM_TYPES)),
            Effects.ModifyStats(-swarm, -swarm, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Michael Phillippi"
        flavorText = "\"Life and death are a cycle, and it is I who keeps it spinning.\"\n—Decai, squirrelfolk rotmaster"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8486f472-afad-4fdd-a57a-d20d8871b543.jpg?1783910732"
        ruling(
            "2024-07-26",
            "If you control a creature that has more than one of the listed types (for example, Daggerfang Duo, " +
                "which is a Rat Squirrel), it's only counted once for Swarmyard Massacre."
        )
    }
}
