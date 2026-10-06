package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Drain Life (LEA #105) — {X}{1}{B} Sorcery.
 * "Spend only black mana on X. Drain Life deals X damage to any target. You gain life equal to the
 * damage dealt, but not more life than the player's life total before the damage was dealt, the
 * planeswalker's loyalty before the damage was dealt, or the creature's toughness."
 *
 * The cap is snapshotted before the damage; the gain is min(cap, damage actually dealt).
 */
class DrainLifeScenarioTest : ScenarioTestBase() {

    private fun TestGame.castDrainLifeAtPlayer(x: Int) {
        val cardId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Drain Life"
        }
        val cast = execute(CastSpell(player1Id, cardId, listOf(ChosenTarget.Player(player2Id)), x))
        withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("X damage to a player with plenty of life gains X") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Drain Life")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withLifeTotal(1, 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castDrainLifeAtPlayer(4)

            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 14
        }

        test("gain is capped at the player's life total before the damage") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Drain Life")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withLifeTotal(1, 10)
                .withLifeTotal(2, 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castDrainLifeAtPlayer(5)

            withClue("only 3 life gained — the victim had 3 life before the damage") {
                game.getLifeTotal(1) shouldBe 13
            }
        }

        test("gain is capped at the creature's toughness") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Drain Life")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLifeTotal(1, 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castXSpell(1, "Drain Life", xValue = 5, targetId = bears)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            withClue("only 2 life gained — Grizzly Bears has toughness 2") {
                game.getLifeTotal(1) shouldBe 12
            }
        }

        test("X can't be paid with non-black mana") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Drain Life")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Drain Life"
            }
            val cast = game.execute(
                CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Player(game.player2Id)), 2)
            )
            withClue("X=2 needs three black sources in total; only two Swamps are available") {
                (cast.error != null) shouldBe true
            }
        }
    }
}
