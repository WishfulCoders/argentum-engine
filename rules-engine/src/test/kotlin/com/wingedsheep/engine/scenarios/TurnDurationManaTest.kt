package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Turn-duration mana ([ManaExpiry.KEPT_UNTIL_END_OF_TURN]): "Until end of turn, you don't lose this
 * mana as steps and phases end" (Brazen Collector). Held as an AnySpend restricted entry; it survives
 * the emptying at every step/phase boundary until end-of-turn cleanup downgrades it to ordinary mana.
 */
class TurnDurationManaTest : FunSpec({

    fun pool() = ManaPoolComponent(red = 1).addRestricted(
        color = Color.RED, amount = 1, restriction = ManaRestriction.AnySpend,
        expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN
    )

    test("a step boundary empties ordinary mana and keeps turn-duration mana") {
        val after = pool().emptyAtBoundary(convertTo = null, retain = emptySet())
        after.red shouldBe 0
        after.restrictedMana.size shouldBe 1
        after.restrictedMana.single().expiry shouldBe ManaExpiry.KEPT_UNTIL_END_OF_TURN
    }

    test("once cleanup downgrades it, the next boundary empties turn-duration mana too") {
        val after = pool().expireTurnKeptMana().emptyAtBoundary(convertTo = null, retain = emptySet())
        after shouldBe ManaPoolComponent()
    }
})
