package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Brazen Collector — "First strike. Whenever this creature attacks, add {R}. Until end of turn, you
 * don't lose this mana as steps and phases end."
 *
 * Pools empty as every step and phase ends (CR 500.5), so the attack-trigger mana must be tagged
 * [ManaExpiry.KEPT_UNTIL_END_OF_TURN] to reach the postcombat main phase — and must still be lost
 * once the turn ends.
 */
class BrazenCollectorScenarioTest : FunSpec({

    fun GameTestDriver.keptRed(playerId: EntityId) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .count { it.color == Color.RED && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }

    fun GameTestDriver.attackWithCollector(): Pair<EntityId, EntityId> {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val attacker = activePlayer!!
        val defender = getOpponent(attacker)
        val collector = putCreatureOnBattlefield(attacker, "Brazen Collector")
        removeSummoningSickness(collector)

        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, listOf(collector), defender)
        bothPass() // the attack trigger resolves, adding {R}
        return attacker to defender
    }

    test("the attack-trigger mana survives combat into the postcombat main phase") {
        val d = GameTestDriver()
        val (attacker, _) = d.attackWithCollector()
        d.keptRed(attacker) shouldBe 1

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.keptRed(attacker) shouldBe 1
    }

    test("the kept mana pays for a spell after combat") {
        val d = GameTestDriver()
        val (attacker, defender) = d.attackWithCollector()
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        val bolt = d.putCardInHand(attacker, "Lightning Bolt")
        d.castSpell(attacker, bolt, listOf(defender)).error shouldBe null
        d.bothPass()

        d.keptRed(attacker) shouldBe 0
        d.getLifeTotal(defender) shouldBe 20 - 2 - 3 // first-strike combat damage, then the Bolt
    }

    test("the kept mana is lost once the turn ends") {
        val d = GameTestDriver()
        val (attacker, _) = d.attackWithCollector()

        d.passPriorityUntil(Step.END)
        d.keptRed(attacker) shouldBe 1

        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(attacker)?.get<ManaPoolComponent>()?.isEmpty shouldBe true
    }
})
