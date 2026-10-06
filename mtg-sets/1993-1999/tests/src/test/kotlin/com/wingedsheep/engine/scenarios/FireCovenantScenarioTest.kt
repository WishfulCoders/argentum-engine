package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fire Covenant — {1}{B}{R} instant (ICE): "As an additional cost to cast this spell, pay X life.
 * Fire Covenant deals X damage divided as you choose among any number of target creatures."
 *
 * The life paid is the X the spell announces, so it caps the targets (each must be dealt at least
 * 1 — CR 601.2d; X = 0 means no targets) and fixes the division's total at cast time; the division
 * is not redone when a target becomes illegal (ruling).
 */
class FireCovenantScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Fire Covenant")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.covenant(life: Int, targets: List<EntityId>, division: Map<EntityId, Int>? = null): ExecutionResult {
        val card = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Fire Covenant" }
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = card,
                targets = targets.map { ChosenTarget.Permanent(it) },
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = life),
                damageDistribution = division,
            )
        )
    }

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("pay 7 life to kill a 2/2 and a 6/4 with a 2 + 5 split") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.covenant(7, listOf(bears, wurm), mapOf(bears to 2, wurm to 5)).error shouldBe null
            game.getLifeTotal(1) shouldBe 13
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("X caps the target count: three targets need at least 3 life") {
            val game = board()
            val ids = listOf("Grizzly Bears", "Craw Wurm", "Hill Giant").map { game.findPermanent(it)!! }
            withClue("2 life can't be divided among three creatures") {
                game.covenant(2, ids, mapOf(ids[0] to 1, ids[1] to 1, ids[2] to 0)).error shouldNotBe null
            }
            game.covenant(3, ids, ids.associateWith { 1 }).error shouldBe null
        }

        test("the division is announced with the cast and must total X") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.covenant(5, listOf(bears, giant), mapOf(bears to 2, giant to 2)).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 20
        }

        test("a target that becomes illegal loses its share; the others keep theirs") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.covenant(4, listOf(bears, giant), mapOf(bears to 2, giant to 2)).error shouldBe null

            // The Bears leave before Fire Covenant resolves.
            game.state = game.zones.moveToZone(game.state, bears, com.wingedsheep.sdk.core.Zone.GRAVEYARD).state
            game.resolveStack()

            withClue("not re-divided: the Giant takes only its announced 2") {
                game.damageOn(giant) shouldBe 2
            }
        }

        test("the cast offer asks for the life first and divides the declared X") {
            val game = board()
            val offer = game.getLegalActions(1).firstOrNull { it.description.contains("Fire Covenant") }
            offer.shouldNotBeNull()
            offer.additionalCostInfo?.costType shouldBe "PayXLife"
            offer.damageTotalIsX shouldBe true
            offer.xConstrainsTargetCount shouldBe true
        }
    }
}
