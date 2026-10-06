package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Displacer Kitten (CLB #63) — whenever you cast a noncreature spell, exile up to one target
 * nonland permanent you control, then return it. The blinked permanent is a new object, so its
 * enters trigger fires again; the trigger resolves before the spell that caused it.
 */
class DisplacerKittenScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Displacer Kitten")
            .withCardOnBattlefield(1, "Elvish Visionary", tapped = true)
            .withCardInHand(1, "Divination")
            .withLandsOnBattlefield(1, "Island", 3)
            .apply { repeat(5) { withCardInLibrary(1, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a noncreature spell blinks the target, re-triggering its enters ability") {
            val game = base().build()
            val visionary = game.findPermanent("Elvish Visionary")!!

            game.castSpell(1, "Divination").error shouldBe null
            withClue("the Kitten's trigger asks for its optional target") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(visionary)).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Elvish Visionary")
            withClue("the Visionary is back untapped, a new object") {
                returned shouldNotBe null
                game.state.getEntity(returned!!)!!.has<TappedComponent>() shouldBe false
            }
            withClue("Visionary's draw plus Divination's two") {
                game.handSize(1) shouldBe 3
            }
        }

        test("up to one: choosing no target blinks nothing") {
            val game = base().build()
            val visionary = game.findPermanent("Elvish Visionary")!!

            game.castSpell(1, "Divination").error shouldBe null
            if (game.hasPendingDecision()) game.skipTargets().error shouldBe null
            game.resolveStack()

            game.findPermanent("Elvish Visionary") shouldBe visionary
            game.state.getEntity(visionary)!!.has<TappedComponent>() shouldBe true
            game.handSize(1) shouldBe 2
        }
    }
}
