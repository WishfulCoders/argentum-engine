package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gideon, Ally of Zendikar (BFZ #29), loyalty 4.
 *
 *   +1: Until end of turn, Gideon becomes a 5/5 Human Soldier Ally creature with indestructible
 *       that's still a planeswalker. Prevent all damage that would be dealt to him this turn.
 *   0: Create a 2/2 white Knight Ally creature token.
 *   −4: You get an emblem with "Creatures you control get +1/+1."
 */
class GideonAllyOfZendikarScenarioTest : ScenarioTestBase() {

    private fun abilityId(change: Int) = cardRegistry.getCard("Gideon, Ally of Zendikar")!!.script.activatedAbilities
        .single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun board() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Gideon, Ally of Zendikar")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("+1: a 5/5 indestructible Human Soldier Ally creature that is still a planeswalker, and damage to him is prevented") {
            val game = board()
            val gideon = game.findPermanent("Gideon, Ally of Zendikar")!!

            game.execute(ActivateAbility(game.player1Id, gideon, abilityId(1))).error shouldBe null
            game.resolveStack()

            val p = game.state.projectedState
            p.isCreature(gideon) shouldBe true
            p.hasType(gideon, "PLANESWALKER") shouldBe true
            p.getPower(gideon) shouldBe 5
            p.getToughness(gideon) shouldBe 5
            p.hasKeyword(gideon, Keyword.INDESTRUCTIBLE) shouldBe true
            p.getSubtypes(gideon).containsAll(setOf("Human", "Soldier", "Ally")) shouldBe true
            game.loyalty(gideon) shouldBe 5

            game.castSpell(1, "Lightning Bolt", gideon).error shouldBe null
            game.resolveStack()
            withClue("the Bolt's damage is prevented: no loyalty lost") {
                game.loyalty(gideon) shouldBe 5
                game.isOnBattlefield("Gideon, Ally of Zendikar") shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the creature effect ends with the turn") {
                game.state.projectedState.isCreature(gideon) shouldBe false
            }
        }

        test("0: creates a 2/2 white Knight Ally token") {
            val game = board()
            val gideon = game.findPermanent("Gideon, Ally of Zendikar")!!

            game.execute(ActivateAbility(game.player1Id, gideon, abilityId(0))).error shouldBe null
            game.resolveStack()

            val p = game.state.projectedState
            val knights = game.state.getBattlefield().filter { p.hasSubtype(it, "Knight") }
            knights.size shouldBe 1
            val knight = knights.single()
            p.getPower(knight) shouldBe 2
            p.getToughness(knight) shouldBe 2
            p.hasSubtype(knight, "Ally") shouldBe true
            game.loyalty(gideon) shouldBe 4
        }

        test("−4: emblem gives creatures you control +1/+1") {
            val game = board()
            val gideon = game.findPermanent("Gideon, Ally of Zendikar")!!

            game.execute(ActivateAbility(game.player1Id, gideon, abilityId(-4))).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            withClue("Gideon had exactly 4 loyalty and is gone") {
                game.isOnBattlefield("Gideon, Ally of Zendikar") shouldBe false
            }
        }
    }
}
