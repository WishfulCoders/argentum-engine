package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Turn-duration mana ([ManaExpiry.UNTIL_END_OF_TURN]): "Until end of turn, you don't lose this mana
 * as steps and phases end" (Brazen Collector). Held as an AnySpend restricted entry; it survives
 * the emptying at every step/phase boundary except the one that ends the turn.
 */
class TurnDurationManaTest : FunSpec({

    fun pool() = ManaPoolComponent(red = 1).addRestricted(
        color = Color.RED, amount = 1, restriction = ManaRestriction.AnySpend,
        expiry = ManaExpiry.UNTIL_END_OF_TURN
    )

    test("a step boundary empties ordinary mana and keeps turn-duration mana") {
        val after = pool().emptyAtBoundary(convertToRed = false, retain = emptySet())
        after.red shouldBe 0
        after.restrictedMana.size shouldBe 1
        after.restrictedMana.single().expiry shouldBe ManaExpiry.UNTIL_END_OF_TURN
    }

    test("the boundary that ends the turn empties turn-duration mana too") {
        val after = pool().emptyAtBoundary(convertToRed = false, retain = emptySet(), endOfTurn = true)
        after shouldBe ManaPoolComponent()
    }
})
