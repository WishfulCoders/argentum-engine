package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A "pay X life" additional cost announces the spell's X (CR 107.3a, 601.2b), and everything X
 * drives at cast time reads that announced X — Fire Covenant's shape: "As an additional cost to cast
 * this spell, pay X life. This spell deals X damage divided as you choose among any number of target
 * creatures."
 *
 *  - the target count is capped at X, so each target can be dealt at least 1 (CR 601.2d); X = 0
 *    means no targets (Fire Covenant ruling);
 *  - the division is announced with the cast and must total exactly X, at least 1 per target;
 *  - one target takes all X with no division;
 *  - you can't pay more life than you have (CR 119.4);
 *  - the cast offer marks its division total as X, so a client divides the declared life.
 */
class PayXLifeAnnouncedXTest : ScenarioTestBase() {

    private val covenant = card("Test Life Covenant") {
        manaCost = "{0}"
        typeLine = "Instant"
        additionalCost(Costs.additional.PayXLife())
        spell {
            targets(
                TargetFilter(GameObjectFilter.Creature),
                minCount = 0,
                unlimited = true,
                dynamicMaxCount = DynamicAmounts.xValue(),
            )
            effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue())
        }
    }

    init {
        cardRegistry.register(listOf(covenant))

        fun board() = scenario()
            .withPlayers()
            .withCardInHand(1, "Test Life Covenant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Craw Wurm")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cast(life: Int, targets: List<EntityId>, division: Map<EntityId, Int>? = null): ExecutionResult {
            val cardId = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Test Life Covenant"
            }
            return execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = cardId,
                    targets = targets.map { ChosenTarget.Permanent(it) },
                    additionalCostPayment = AdditionalCostPayment(payXLifeAmount = life),
                    damageDistribution = division,
                )
            )
        }

        fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        test("pay 5 life, divide 2 and 3 between two creatures") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.cast(5, listOf(bears, wurm), mapOf(bears to 2, wurm to 3)).error shouldBe null
            game.getLifeTotal(1) shouldBe 15
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.damageOn(wurm) shouldBe 3
        }

        test("one target takes all X with no division") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.cast(3, listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 17
        }

        test("more targets than the life paid is rejected — each target needs at least 1") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            withClue("X = 1 can't be divided among two targets") {
                game.cast(1, listOf(bears, giant), mapOf(bears to 1, giant to 0)).error shouldNotBe null
            }
            game.getLifeTotal(1) shouldBe 20
        }

        test("X = 0 allows no targets") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(0, listOf(bears)).error shouldNotBe null
            game.cast(0, emptyList()).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("the division must total the life paid") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.cast(3, listOf(bears, wurm), mapOf(bears to 2, wurm to 2)).error shouldNotBe null
            game.cast(3, listOf(bears, wurm), mapOf(bears to 2, wurm to 1)).error shouldBe null
        }

        test("can't pay more life than you have") {
            val game = board()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.cast(21, listOf(wurm)).error shouldNotBe null
        }

        test("the cast offer marks its divided total as the announced X") {
            val game = board()
            val offer = game.getLegalActions(1).firstOrNull { it.description.contains("Test Life Covenant") }
            offer.shouldNotBeNull()
            offer.requiresDamageDistribution shouldBe true
            offer.damageTotalIsX shouldBe true
            offer.additionalCostInfo?.costType shouldBe "PayXLife"
            offer.additionalCostInfo?.payXLifeMaxX shouldBe 20
        }
    }
}
