package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.clb.cards.MinscAndBooTimelessHeroes
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Minsc & Boo, Timeless Heroes (CLB #285).
 *
 * Pins the −2's reflexive trigger (CR 603.12) reading the sacrificed creature's last-known power
 * and creature types (CR 608.2h) — a Hamster pumped with +1/+1 counters deals and draws its pumped
 * power; a non-Hamster deals damage but draws nothing — on both the auto-picked and the chosen
 * (paused) sacrifice paths, and the "you may create Boo" enters trigger.
 */
class MinscAndBooTimelessHeroesScenarioTest : ScenarioTestBase() {
    init {
        val minusTwo = MinscAndBooTimelessHeroes.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == -2 }.id

        fun board(vararg creatures: String) = scenario()
            .withPlayers("Minsc", "Opponent")
            .withCardOnBattlefield(1, "Minsc & Boo, Timeless Heroes")
            .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
            .apply { repeat(6) { withCardInLibrary(1, "Forest") } }
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.pump(name: String, counters: Int) {
            val id = findPermanent(name)!!
            state = state.updateEntity(id) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to counters)))
            }
        }

        fun TestGame.minusTwo() =
            execute(ActivateAbility(player1Id, findPermanent("Minsc & Boo, Timeless Heroes")!!, minusTwo))

        test("enters: you may create Boo, a legendary 1/1 red Hamster with trample and haste") {
            val game = scenario()
                .withPlayers("Minsc", "Opponent")
                .withCardInHand(1, "Minsc & Boo, Timeless Heroes")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Minsc & Boo, Timeless Heroes").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            val boo = game.findPermanent("Boo")
            withClue("Boo was created") { boo shouldNotBe null }
            val projected = game.state.projectedState
            projected.isLegendary(boo!!) shouldBe true
            projected.getSubtypes(boo).contains("Hamster") shouldBe true
            projected.hasKeyword(boo, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(boo, Keyword.HASTE) shouldBe true
            projected.getPower(boo) shouldBe 1
        }

        test("−2 sacrificing a pumped Hamster deals its last-known power and draws that many") {
            val game = board("Jolly Gerbils")
            game.pump("Jolly Gerbils", 2)
            game.state.projectedState.getPower(game.findPermanent("Jolly Gerbils")!!) shouldBe 4
            val hand = game.handSize(1)

            game.minusTwo().error shouldBe null
            game.resolveStack()
            withClue("the only creature was sacrificed automatically, then the reflexive trigger asks for a target") {
                game.isInGraveyard(1, "Jolly Gerbils") shouldBe true
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
            game.handSize(1) shouldBe hand + 4
        }

        test("−2 sacrificing a non-Hamster deals damage but draws nothing") {
            val game = board("Grizzly Bears")
            val hand = game.handSize(1)
            game.minusTwo().error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe hand
        }

        test("−2 with a choice of creatures: the chosen Hamster's power is used after the pause") {
            val game = board("Jolly Gerbils", "Grizzly Bears")
            game.pump("Jolly Gerbils", 1)
            val hand = game.handSize(1)
            game.minusTwo().error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findPermanent("Jolly Gerbils")!!)).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 17
            game.handSize(1) shouldBe hand + 3
        }

        test("−2 with no creature to sacrifice does nothing") {
            val game = board()
            game.minusTwo().error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
