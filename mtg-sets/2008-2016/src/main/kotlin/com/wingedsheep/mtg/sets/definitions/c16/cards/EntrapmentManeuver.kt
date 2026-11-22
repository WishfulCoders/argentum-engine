package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Entrapment Maneuver — Commander 2016 #2 (canonical printing)
 * {3}{W} · Instant
 *
 * Target player sacrifices an attacking creature of their choice. You create X 1/1 white Soldier
 * creature tokens, where X is that creature's toughness.
 *
 * Wing Shards' edict plus Tribute to Hunger's payoff: the edict records the sacrificed
 * permanent's last-known snapshot, so the token count reads its toughness as it last existed on
 * the battlefield (2016-11-08 ruling). No attacking creature → nothing sacrificed, no tokens.
 */
val EntrapmentManeuver = card("Entrapment Maneuver") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target player sacrifices an attacking creature of their choice. " +
        "You create X 1/1 white Soldier creature tokens, where X is that creature's toughness."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.Creature.attacking(), 1, player) then
            Effects.CreateToken(
                count = DynamicAmounts.sacrificedToughness(0),
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                imageUri = "https://cards.scryfall.io/normal/front/0/a/0a47f751-52f1-4042-85dd-ea222e5d969d.jpg?1783937015",
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Filip Burburan"
        flavorText = "\"Greed, rage, and bloodlust all lead to death at the hands of those who keep their heads.\"\n—Sergeant Eyra"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4dcebe1-bfc7-4f7d-ad8c-91a4b9f23eef.jpg?1783937091"
        ruling("2016-11-08", "Entrapment Maneuver doesn't target the creature to be sacrificed. The target player chooses one as it resolves. No player may take any other actions between the target player choosing the creature and you creating Soldier tokens.")
        ruling("2016-11-08", "To determine how many Soldier tokens are created, use the toughness of the sacrificed creature as it last existed on the battlefield.")
    }
}
