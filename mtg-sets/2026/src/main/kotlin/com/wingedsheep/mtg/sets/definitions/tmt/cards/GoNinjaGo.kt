package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Go Ninja Go
 * {R}{W}
 * Sorcery
 *
 * Choose one or both —
 * • Exile target creature you control, then return it to the battlefield under
 *   its owner's control.
 * • Go Ninja Go deals damage equal to the greatest power among creatures you
 *   control to target creature an opponent controls.
 */
val GoNinjaGo = card("Go Ninja Go") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n• Exile target creature you control, then return it to the battlefield under its owner's control.\n• Go Ninja Go deals damage equal to the greatest power among creatures you control to target creature an opponent controls."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Exile target creature you control, then return it to the battlefield under its owner's control") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.Move(creature, Zone.EXILE) then Effects.Move(creature, Zone.BATTLEFIELD)
            }
            mode("Go Ninja Go deals damage equal to the greatest power among creatures you control to target creature an opponent controls") {
                val opponentCreature = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.DealDamage(
                    DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature
                    ).maxPower(),
                    opponentCreature
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Patrick Gañas"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90e68cd8-ea76-4bd1-9199-474da9c3e8bf.jpg?1771587007"
    }
}
