package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Savage Ventmaw — "{4}{R}{G} 4/4 Flying Dragon. Whenever this creature attacks, add
 * {R}{R}{R}{G}{G}{G}. Until end of turn, you don't lose this mana as steps and phases end." The mana
 * is [ManaExpiry.KEPT_UNTIL_END_OF_TURN], so it survives combat into the postcombat main phase.
 */
class SavageVentmawScenarioTest : FunSpec({

    test("attacking adds three red and three green that last through combat") {
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

        fun kept(color: Color) = d.state.getEntity(attacker)!!.get<ManaPoolComponent>()!!.restrictedMana
            .count { it.color == color && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }
        kept(Color.RED) shouldBe 3
        kept(Color.GREEN) shouldBe 3

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        kept(Color.RED) shouldBe 3
        kept(Color.GREEN) shouldBe 3
    }
})
