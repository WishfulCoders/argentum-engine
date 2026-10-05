package com.wingedsheep.engine.loop

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card

/**
 * A test-only loop piece: "{0}: Create a 1/1 Servo token." Repeating it is a loop by itself; with
 * Impact Tremors on the battlefield every repetition also deals 1 damage to each opponent. (This
 * branch's card pool predates Kiki-Jiki, the real-card loop the main-line tests use.)
 */
val LoopEngine = card("Loop Engine") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{0}: Create a 1/1 colorless Servo artifact creature token."
    activatedAbility {
        cost = Costs.Mana("{0}")
        effect = Effects.CreateToken(power = 1, toughness = 1, colors = emptySet(), creatureTypes = setOf("Servo"))
    }
}
