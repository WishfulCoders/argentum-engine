package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unexpectedly Absent (C13 #25) — {X}{W}{W} instant: "Put target nonland permanent into its owner's
 * library just beneath the top X cards of that library."
 */
class UnexpectedlyAbsentScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.library2() = state.getLibrary(player2Id).map {
            state.getEntity(it)!!.get<CardComponent>()!!.name
        }

        fun board() = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "Unexpectedly Absent")
            .withLandsOnBattlefield(1, "Plains", 6)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Forest")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Swamp")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("X = 2 tucks it beneath the top two cards of its owner's library") {
            val game = board().build()
            val r = game.castXSpell(1, "Unexpectedly Absent", 2, game.findPermanent("Hill Giant")!!)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.library2() shouldBe listOf("Island", "Swamp", "Hill Giant", "Mountain")
        }

        test("X = 0 puts it on top") {
            val game = board().build()
            game.castXSpell(1, "Unexpectedly Absent", 0, game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            game.library2().first() shouldBe "Hill Giant"
        }

        test("X larger than the library puts it on the bottom") {
            val game = board().build()
            game.castXSpell(1, "Unexpectedly Absent", 4, game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            game.library2().last() shouldBe "Hill Giant"
        }

        test("lands aren't legal targets") {
            val game = board().build()
            game.castXSpell(1, "Unexpectedly Absent", 0, game.findPermanent("Forest")!!).error shouldNotBe null
        }
    }
}
