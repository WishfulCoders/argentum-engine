package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.DrudgeSkeletons
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Damn (MH2 #80) — "Destroy target creature. A creature destroyed this way can't be regenerated.
 * Overload {2}{W}{W}".
 */
class DamnScenarioTest : ScenarioTestBase() {
    init {
        val regenerateId = DrudgeSkeletons.activatedAbilities[0].id

        fun TestGame.shieldSkeletons() {
            val skeletons = findPermanent("Drudge Skeletons")!!
            execute(ActivateAbility(player1Id, skeletons, regenerateId)).error shouldBe null
            resolveStack()
        }

        test("cast for {B}{B}: destroys the target, and a regeneration shield doesn't save it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Damn")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Drudge Skeletons")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.shieldSkeletons()
            val skeletons = game.findPermanent("Drudge Skeletons")!!

            val r = game.castSpell(1, "Damn", targetId = skeletons)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Drudge Skeletons") shouldBe true
            // Single target: the other creature is untouched.
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("cast for {B}{B}: a hexproof creature can't be targeted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Damn")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Gladecover Scout")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val scout = game.findPermanent("Gladecover Scout")!!
            game.castSpell(1, "Damn", targetId = scout).error shouldNotBe null
        }

        test("overloaded: destroys each creature, ignoring hexproof and regeneration") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Damn")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Drudge Skeletons")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Gladecover Scout")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.shieldSkeletons()

            val r = game.castSpellWithOverload(1, "Damn")
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Drudge Skeletons") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Gladecover Scout") shouldBe true
        }

        test("the overload cost needs its white mana") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Damn")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellWithOverload(1, "Damn").error shouldNotBe null
        }
    }
}
