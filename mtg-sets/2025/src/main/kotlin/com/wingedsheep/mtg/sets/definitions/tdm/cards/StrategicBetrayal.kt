package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Effects

/**
 * Strategic Betrayal — Tarkir: Dragonstorm #94
 * {1}{B} · Sorcery
 *
 * Target opponent exiles a creature they control and their graveyard.
 *
 * Ruling: the opponent chooses which creature. If they control no creatures,
 * they simply exile their graveyard.
 */
val StrategicBetrayal = card("Strategic Betrayal") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles a creature they control and their graveyard."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val creaturesCanExile = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = opponent.asPlayer
                )
            )
            ifNotEmpty(creaturesCanExile) {
                val chosenCreature = chooseExactly(
                    1,
                    from = creaturesCanExile,
                    chooser = Chooser.TargetPlayer,
                    prompt = "Choose a creature to exile",
                    useTargetingUI = true
                )
                exile(chosenCreature, opponent.asPlayer)
            }
            val opponentGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
            exile(opponentGraveyard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Flavio Greco Paglia"
        flavorText = "As Sultai blades threatened Qatros city walls, Mehtma of House Fenzala turned her weapons on her political rivals."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95617742-548d-464a-bb89-a858ffa9018f.jpg?1743204340"
        ruling("2025-04-04", "The opponent chooses which creature to exile as Strategic Betrayal resolves. If they don't control any creatures at that time, they simply exile their graveyard.")
    }
}
