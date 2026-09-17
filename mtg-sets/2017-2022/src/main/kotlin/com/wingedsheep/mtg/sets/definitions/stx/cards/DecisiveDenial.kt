package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Decisive Denial
 * {G}{U}
 * Instant
 * Choose one —
 * • Target creature you control fights target creature you don't control.
 * • Counter target noncreature spell unless its controller pays {3}.
 *
 * Savage Smash's fight without the pump, and Complicate's counter-unless-pays over a noncreature
 * spell.
 */
val DecisiveDenial = card("Decisive Denial") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)\n" +
        "• Counter target noncreature spell unless its controller pays {3}."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature you control fights target creature you don't control.") {
                val yours = target("target creature you control", TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.youControl())
                ))
                val theirs = target("target creature you don't control", TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
                ))
                effect = Effects.Fight(yours, theirs)
            }
            mode("Counter target noncreature spell unless its controller pays {3}.") {
                target("target noncreature spell", Targets.NoncreatureSpell)
                effect = Effects.CounterUnlessPays("{3}")
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"I've heard enough.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2e9d132-95f7-4ee7-9c91-be19e4ad7a5d.jpg?1783927318"
    }
}
