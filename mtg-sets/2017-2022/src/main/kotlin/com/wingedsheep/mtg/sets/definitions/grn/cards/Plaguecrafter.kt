package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plaguecrafter
 * {2}{B}
 * Creature — Human Shaman
 * 3/2
 * When this creature enters, each player sacrifices a creature or planeswalker of their choice.
 * Each player who can't discards a card.
 *
 * "Each player who can't" is fixed before anyone sacrifices: the players controlling no creature
 * or planeswalker are recorded first (`storePlayer(onlyIf = …)` per player), then every player
 * sacrifices, then only the recorded players discard. Deciding it afterwards would also make a
 * player who sacrificed their last creature discard. Plaguecrafter itself is a legal sacrifice.
 */
val Plaguecrafter = card("Plaguecrafter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    oracleText = "When this creature enters, each player sacrifices a creature or planeswalker of their choice. " +
        "Each player who can't discards a card."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val cant = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                listOf(storePlayer(onlyIf = Conditions.YouControl(GameObjectFilter.CreatureOrPlaneswalker, negate = true)))
            }.single()
            run(Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, 1, EffectTarget.PlayerRef(Player.Each)))
            run(Effects.ForEachPlayer(cant.asPlayers, Patterns.Hand.discardCards(1)))
        }
        description = "When this creature enters, each player sacrifices a creature or planeswalker of their choice. " +
            "Each player who can't discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Anna Steinbauer"
        flavorText = "\"My power is generosity, in a way. I give my survivors an appreciation for their lives.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8682fb87-df14-4277-aaa0-0d53d766c406.jpg?1783934171"
        ruling("2018-10-05", "As Plaguecrafter's ability resolves, first the player whose turn it is chooses a creature or planeswalker they control, then each other player in turn order does the same, knowing the choices made before them. Then all the chosen permanents are sacrificed at the same time. Next, each player in the same order who couldn't sacrifice a permanent chooses a card in hand without revealing it, then the chosen cards are discarded at the same time.")
        ruling("2018-10-05", "Each player chooses a permanent to sacrifice from among the creatures and planeswalkers they control. You don't choose which type of permanent any other player has to sacrifice.")
        ruling("2018-10-05", "Plaguecrafter can be the creature its controller sacrifices for its own ability.")
    }
}
