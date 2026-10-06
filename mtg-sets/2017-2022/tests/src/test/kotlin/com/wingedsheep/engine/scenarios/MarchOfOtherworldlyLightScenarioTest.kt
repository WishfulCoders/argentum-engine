package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * March of Otherworldly Light (NEO #28) — {X}{W} instant: "As an additional cost to cast this spell,
 * you may exile any number of white cards from your hand. This spell costs {2} less to cast for each
 * card exiled this way. Exile target artifact, creature, or enchantment with mana value X or less."
 */
class MarchOfOtherworldlyLightScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.march(x: Int, target: EntityId, exiled: List<EntityId> = emptyList()) = execute(
            CastSpell(
                player1Id, findCardsInHand(1, "March of Otherworldly Light").single(),
                targets = listOf(ChosenTarget.Permanent(target)),
                xValue = x,
                additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
            )
        )

        fun board(plains: Int) = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "March of Otherworldly Light")
            .withCardInHand(1, "Savannah Lions")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", plains)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Bonesplitter")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("X = 4 with one white card exiled costs {2}{W} and exiles a mana value 4 creature") {
            val game = board(3).build()
            val r = game.march(4, game.findPermanent("Hill Giant")!!, game.findCardsInHand(1, "Savannah Lions"))
            withClue("${r.error}") { r.error shouldBe null }
            game.isInExile(1, "Savannah Lions") shouldBe true
            game.resolveStack()
            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("a nonwhite card can't be exiled to pay") {
            val game = board(3).build()
            game.march(4, game.findPermanent("Hill Giant")!!, game.findCardsInHand(1, "Grizzly Bears"))
                .error shouldNotBe null
        }

        test("the target's mana value must be X or less") {
            val game = board(5).build()
            game.march(3, game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }

        test("artifacts are legal targets; with nothing exiled it costs {X}{W}") {
            val game = board(2).build()
            val r = game.march(1, game.findPermanent("Bonesplitter")!!)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.isInExile(2, "Bonesplitter") shouldBe true
        }
    }
}
