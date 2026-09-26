package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crackling Doom
 * {R}{W}{B}
 * Instant
 * Crackling Doom deals 2 damage to each opponent. Each opponent sacrifices a creature
 * with the greatest power among creatures that player controls.
 */
val CracklingDoom = card("Crackling Doom") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Instant"
    oracleText = "Crackling Doom deals 2 damage to each opponent. Each opponent sacrifices a creature with the greatest power among creatures that player controls."

    spell {
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                Effects.ForEachPlayer(
                    players = Player.EachOpponent,
                    Effects.Pipeline {
                        val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                        val greatest = filter(creatures, CollectionFilter.GreatestPower)
                        val toSacrifice = chooseExactly(
                            1,
                            from = greatest,
                            chooser = Chooser.Controller,
                            prompt = "Choose a creature to sacrifice"
                        )
                        sacrifice(toSacrifice)
                    }
                )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Yohann Schepacz"
        flavorText = "Do not fear the lightning. Fear the one it obeys."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f83c7d53-2599-42a9-ae96-a2699c5164cb.jpg?1562796251"
    }
}
