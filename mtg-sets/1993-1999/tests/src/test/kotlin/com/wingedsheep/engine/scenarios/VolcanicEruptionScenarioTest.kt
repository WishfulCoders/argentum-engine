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
 * Volcanic Eruption (LEA #88) — "Destroy X target Mountains. Volcanic Eruption deals damage to
 * each creature and each player equal to the number of Mountains put into a graveyard this way."
 *
 * Proves the damage reads the count of Mountains actually destroyed (stored before the damage
 * passes) and reaches every creature and every player; and that X = 0 is a legal, harmless cast.
 */
class VolcanicEruptionScenarioTest : ScenarioTestBase() {

    init {
        test("destroys X target Mountains and deals that much damage to each creature and each player") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Volcanic Eruption")
                .withLandsOnBattlefield(1, "Island", 5)
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mountains = game.findAllPermanents("Mountain")
            mountains.size shouldBe 3
            val spellId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Volcanic Eruption"
            }
            val cast = game.execute(
                CastSpell(
                    game.player1Id,
                    spellId,
                    mountains.take(2).map { ChosenTarget.Permanent(it) },
                    2
                )
            )
            withClue("Casting Volcanic Eruption (X=2) at two Mountains should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("exactly the two targeted Mountains are destroyed") {
                game.findAllPermanents("Mountain").size shouldBe 1
            }
            withClue("2 damage kills Grizzly Bears") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            withClue("Craw Wurm (6/4) survives 2 damage") {
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }
            withClue("each player takes 2") {
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("X = 0 with no targets destroys nothing and deals no damage") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Volcanic Eruption")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castXSpell(1, "Volcanic Eruption", xValue = 0)
            withClue("Casting Volcanic Eruption with X=0 should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Mountain") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
