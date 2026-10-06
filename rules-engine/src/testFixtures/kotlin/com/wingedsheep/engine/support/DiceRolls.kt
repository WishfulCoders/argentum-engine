package com.wingedsheep.engine.support

import com.wingedsheep.sdk.model.GameRng

/**
 * Forcing a die roll in a test. Die rolls draw from the game's seeded RNG (`GameState.rng`), so a
 * test that wants a particular face installs a generator whose next draw is that face, right
 * before the roll resolves:
 *
 * ```kotlin
 * castSpell(1, "Djinni Windseer")
 * state = state.copy(rng = DiceRolls.rngRolling(20, 20))   // the ETB trigger's d20 comes up 20
 * resolveStack()
 * ```
 */
object DiceRolls {

    /** A generator whose first roll of a [sides]-sided die is [natural] (`nextInt(sides) + 1`). */
    fun rngRolling(sides: Int, natural: Int): GameRng {
        require(natural in 1..sides) { "a d$sides cannot roll $natural" }
        return generateSequence(0L) { it + 1 }
            .map { GameRng.seeded(it) }
            .first { it.nextInt(sides).first + 1 == natural }
    }
}
