package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.KytheonHeroOfAkros
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kytheon, Hero of Akros // Gideon, Battle-Forged (ORI #23).
 *
 * Front: transforms at end of combat only if Kytheon and at least two other creatures attacked this
 * combat — an attacker that died in combat still counts (ruling 2015-06-22). Back: the +2 makes an
 * opponent's creature attack Gideon during its controller's next turn if able.
 */
class KytheonHeroOfAkrosScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Kytheon", "Opponent")
            .withCardOnBattlefield(1, "Kytheon, Hero of Akros")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cardNameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

        test("Kytheon and two others attack: transforms at end of combat, even if one of them died") {
            val game = board()
            val kytheon = game.findPermanent("Kytheon, Hero of Akros")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Kytheon, Hero of Akros" to 2, "Savannah Lions" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.resolveStack()

            val gideon = game.findPermanent("Gideon, Battle-Forged")
            withClue("Kytheon returned transformed") { gideon shouldNotBe null }
            game.state.getEntity(gideon!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 3
            game.cardNameOf(kytheon) shouldBe "Gideon, Battle-Forged"
        }

        test("Kytheon and only one other attacker: no transform") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kytheon, Hero of Akros" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()
            game.findPermanent("Kytheon, Hero of Akros") shouldNotBe null
            game.findPermanent("Gideon, Battle-Forged") shouldBe null
        }

        test("Gideon's +2: the opponent's creature must attack Gideon during its controller's next turn") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Kytheon, Hero of Akros" to 2, "Savannah Lions" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            val gideon = game.findPermanent("Gideon, Battle-Forged")!!
            val plusTwo = KytheonHeroOfAkros.backFace!!.activatedAbilities
                .single { (it.cost as? AbilityCost.Loyalty)?.change == 2 }.id
            game.execute(
                ActivateAbility(
                    game.player1Id, gideon, plusTwo,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("Hill Giant must attack Gideon") {
                game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldNotBe null
            }
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Hill Giant" to "Gideon, Battle-Forged")
            ).error shouldBe null
        }
    }
}
