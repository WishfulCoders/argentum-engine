package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Recommission — return an artifact or creature card with mana value 3 or less from your graveyard;
 * only a *creature* entering this way gets the extra +1/+1 counter, judged as it lands (so March of
 * the Machines turning a noncreature artifact into a creature counts, per the 2022-10-14 ruling).
 */
class RecommissionScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.recommission(caster: EntityId, cardId: EntityId) =
        castSpellWithTargets(
            caster,
            putCardInHand(caster, "Recommission").also { giveMana(caster, Color.WHITE, 2) },
            listOf(ChosenTarget.Card(cardId = cardId, ownerId = caster, zone = Zone.GRAVEYARD)),
        )

    test("a creature card returns with an additional +1/+1 counter") {
        val (driver, caster) = setup()
        val bears = driver.putCardInGraveyard(caster, "Grizzly Bears")

        driver.recommission(caster, bears).error shouldBe null
        driver.bothPass()

        val returned = driver.findPermanent(caster, "Grizzly Bears")!!
        driver.plusOneCounters(returned) shouldBe 1
        driver.state.projectedState.getPower(returned) shouldBe 3
        driver.state.projectedState.getToughness(returned) shouldBe 3
    }

    test("a noncreature artifact returns without a counter") {
        val (driver, caster) = setup()
        val stone = driver.putCardInGraveyard(caster, "Mind Stone")

        driver.recommission(caster, stone).error shouldBe null
        driver.bothPass()

        val returned = driver.findPermanent(caster, "Mind Stone")!!
        driver.plusOneCounters(returned) shouldBe 0
    }

    test("a noncreature artifact that March of the Machines animates on arrival gets the counter") {
        val (driver, caster) = setup()
        driver.putPermanentOnBattlefield(caster, "March of the Machines")
        val stone = driver.putCardInGraveyard(caster, "Mind Stone")

        driver.recommission(caster, stone).error shouldBe null
        driver.bothPass()

        val returned = driver.findPermanent(caster, "Mind Stone")!!
        driver.plusOneCounters(returned) shouldBe 1
        // March makes it a 2/2 (mana value 2); the counter makes it 3/3.
        driver.state.projectedState.getPower(returned) shouldBe 3
    }

    test("a creature card with mana value 4 is not a legal target") {
        val (driver, caster) = setup()
        val big = driver.putCardInGraveyard(caster, "Hill Giant")

        driver.recommission(caster, big).error shouldNotBe null
    }
})
