package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hard Evidence (MH2 #46) — {U} Sorcery.
 *
 * "Create a 0/3 blue Crab creature token.
 *  Investigate. (Create a Clue token. It's an artifact with '{2}, Sacrifice this token: Draw a
 *  card.')"
 *
 * Covers both halves of the spell (the Crab token's stats and the Clue token's creation), plus the
 * Clue's own activated ability actually drawing a card when later sacrificed.
 */
class HardEvidenceScenarioTest : ScenarioTestBase() {

    private val clueDrawAbilityId = cardRegistry.getCard("Clue")!!.activatedAbilities[0].id

    init {
        context("Hard Evidence") {

            test("creates a 0/3 blue Crab and investigates for a Clue token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hard Evidence")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hard Evidence").error shouldBe null
                game.resolveStack()

                val crab = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes
                        ?.any { it.value == "Crab" } == true
                }
                withClue("a Crab creature token entered the battlefield") {
                    crab shouldNotBe null
                }
                val crabId = crab!!
                withClue("the Crab is 0/3 and blue") {
                    game.state.projectedState.getPower(crabId) shouldBe 0
                    game.state.projectedState.getToughness(crabId) shouldBe 3
                    game.state.getEntity(crabId)?.get<CardComponent>()?.colors shouldBe setOf(Color.BLUE)
                }
                withClue("a Clue token was created") {
                    game.findAllPermanents("Clue").size shouldBe 1
                }
            }

            test("the Clue token can be sacrificed for {2} to draw a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hard Evidence")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hard Evidence").error shouldBe null
                game.resolveStack()

                val clue = game.findPermanent("Clue")!!
                val handBefore = game.handSize(1)

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = clue,
                        abilityId = clueDrawAbilityId,
                    )
                )
                withClue("activating the Clue's ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("the Clue is sacrificed and a card is drawn") {
                    game.isOnBattlefield("Clue") shouldBe false
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
