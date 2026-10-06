package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gut, True Soul Zealot (CLB #180) — "Whenever you attack, you may sacrifice another creature or an
 * artifact. If you do, create a 4/1 black Skeleton creature token with menace that's tapped and
 * attacking."
 */
class GutTrueSoulZealotScenarioTest : ScenarioTestBase() {
    private val gut = "Gut, True Soul Zealot"

    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, gut, summoningSickness = false)
            .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")

        fun skeletons(game: TestGame) = game.state.getBattlefield().filter { id ->
            game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.typeLine?.subtypes?.any { it.value == "Skeleton" } == true
        }

        test("sacrificing another creature makes a 4/1 menace Skeleton that's tapped and attacking") {
            val game = base().withCardOnBattlefield(1, "Grizzly Bears").build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(gut to 2)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            (game.getPendingDecision() as? SelectCardsDecision)?.let {
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            val token = skeletons(game).single()
            val entity = game.state.getEntity(token)!!
            entity.has<TappedComponent>() shouldBe true
            entity.get<AttackingComponent>()?.defenderId shouldBe game.player2Id
            game.state.projectedState.getPower(token) shouldBe 4
            game.state.projectedState.getToughness(token) shouldBe 1
            game.state.projectedState.hasKeyword(token, Keyword.MENACE) shouldBe true
            game.state.projectedState.getColors(token) shouldBe setOf("BLACK")

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            withClue("Gut's 2 plus the Skeleton's 4") { game.getLifeTotal(2) shouldBe 14 }
        }

        test("an artifact is fodder too, and Gut needn't attack") {
            val game = base().withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Sol Ring").build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Hill Giant and Sol Ring, but not Gut itself") {
                decision.options.toSet() shouldBe setOf(game.findPermanent("Hill Giant")!!, game.findPermanent("Sol Ring")!!)
            }
            game.selectCards(listOf(game.findPermanent("Sol Ring")!!)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Sol Ring") shouldBe null
            skeletons(game).size shouldBe 1
        }

        test("declining sacrifices nothing and makes no token") {
            val game = base().withCardOnBattlefield(1, "Grizzly Bears").build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(gut to 2)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            skeletons(game).size shouldBe 0
        }

        test("with nothing else to sacrifice there is no prompt and no token") {
            val game = base().build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(gut to 2)).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield(gut) shouldBe true
            skeletons(game).size shouldBe 0
        }
    }
}
