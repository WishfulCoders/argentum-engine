package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Leyline Binding (DMU #24) — flash; domain cost reduction; "When this enchantment enters, exile
 * target nonland permanent an opponent controls until this enchantment leaves the battlefield."
 */
class LeylineBindingScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Removal") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.Destroy(target(TargetFilter.Permanent)) }
        })

        test("three basic land types make it cost {2}{W}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Leyline Binding")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val r = game.castSpell(1, "Leyline Binding")
            withClue("${r.error}") { r.error shouldBe null }
        }

        test("domain counts types, not lands: three Plains only take off {1}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Leyline Binding")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // {5}{W} - 1 = five mana; four Plains can't pay it.
            game.castSpell(1, "Leyline Binding").error shouldNotBe null
        }

        test("a dual land counts both of its basic land types") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Leyline Binding")
                .withCardOnBattlefield(1, "Tundra")
                .withCardOnBattlefield(1, "Badlands")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // Plains, Island, Swamp, Mountain = 4 types → {1}{W}.
            val r = game.castSpell(1, "Leyline Binding")
            withClue("${r.error}") { r.error shouldBe null }
        }

        test("flash: castable on the opponent's turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Leyline Binding")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.ENDING, Step.END)
                .build()
            val r = game.castSpell(1, "Leyline Binding")
            withClue("${r.error}") { r.error shouldBe null }
        }

        test("exiles an opposing nonland permanent and returns it when Leyline Binding leaves") {
            val game = board().build()
            castBinding(game)
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets.values.flatten()
            legal shouldContain game.findPermanent("Grizzly Bears")!!
            legal shouldNotContain game.findPermanent("Hill Giant")!! // its controller's own creature
            legal shouldNotContain game.findPermanent("Forest")!! // a land

            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true

            game.castSpell(1, "Test Removal", game.findPermanent("Leyline Binding")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Leyline Binding") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.stack.size shouldBe 0
        }

        test("if it leaves before its trigger resolves, the target is never exiled") {
            val game = board().build()
            castBinding(game)
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.castSpell(1, "Test Removal", game.findPermanent("Leyline Binding")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.zoneReturns.size shouldBe 0
        }
    }

    private fun board() = scenario().withPlayers("P1", "P2")
        .withCardInHand(1, "Leyline Binding")
        .withCardInHand(1, "Test Removal")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun castBinding(game: TestGame) {
        val r = game.castSpell(1, "Leyline Binding")
        withClue("${r.error}") { r.error shouldBe null }
        game.resolveStack()
        game.state.pendingDecision.shouldNotBeNull()
    }
}
