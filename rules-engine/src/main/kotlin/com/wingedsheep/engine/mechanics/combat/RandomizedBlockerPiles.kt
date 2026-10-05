package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState

object RandomizedBlockerPiles {
    fun isActive(state: GameState): Boolean = state.floatingEffects.any {
        it.effect.modification is SerializableModification.RandomizedBlockerPiles
    }
}
