package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Savage Ventmaw — "{4}{R}{G} 4/4 Flying Dragon. Whenever this creature attacks, add
 * {R}{R}{R}{G}{G}{G}. Until end of turn, you don't lose this mana as steps and phases end."
 * Pools empty as each step ends, so the mana is turn-duration mana (`ManaExpiry.UNTIL_END_OF_TURN`,
 * held as restricted entries) and must still be there in the second main phase.
 */
class SavageVentmawScenarioTest : FunSpec({

    fun GameTestDriver.floating(player: com.wingedsheep.sdk.model.EntityId, color: Color): Int {
        val pool = state.getEntity(player)?.get<ManaPoolComponent>() ?: return 0
        val plain = when (color) {
            Color.RED -> pool.red
            Color.GREEN -> pool.green
            else -> 0
        }
        return plain + pool.restrictedMana.count { it.color == color }
    }

    test("attacking adds three red and three green, which last into the second main phase") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)

        val attacker = d.activePlayer!!
        val defender = d.getOpponent(attacker)

        val ventmaw = d.putCreatureOnBattlefield(attacker, "Savage Ventmaw")
        d.removeSummoningSickness(ventmaw)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(attacker, listOf(ventmaw), defender)
        d.bothPass() // the attack trigger resolves, adding the mana

        d.floating(attacker, Color.RED) shouldBe 3
        d.floating(attacker, Color.GREEN) shouldBe 3

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.floating(attacker, Color.RED) shouldBe 3
        d.floating(attacker, Color.GREEN) shouldBe 3
    }
})
