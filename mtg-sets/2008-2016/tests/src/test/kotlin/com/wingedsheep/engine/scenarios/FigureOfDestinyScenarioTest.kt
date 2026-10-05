package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.FigureOfDestiny
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Figure of Destiny (EVE #139): three level-up abilities, the second and third gated on the
 * creature's subtype *at resolution*, all permanent.
 */
class FigureOfDestinyScenarioTest : FunSpec({

    val abilities = FigureOfDestiny.activatedAbilities
    val spiritId = abilities[0].id
    val warriorId = abilities[1].id
    val avatarId = abilities[2].id

    fun setup(): Pair<GameTestDriver, Pair<EntityId, EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FigureOfDestiny)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val figure = driver.putCreatureOnBattlefield(me, "Figure of Destiny")
        return driver to (me to figure)
    }

    fun GameTestDriver.activate(me: EntityId, figure: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId, red: Int) {
        giveMana(me, Color.RED, red)
        submit(ActivateAbility(playerId = me, sourceId = figure, abilityId = abilityId)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("levels up 1/1 -> 2/2 Spirit -> 4/4 Spirit Warrior -> 8/8 flying first strike Avatar, permanently") {
        val (driver, ids) = setup()
        val (me, figure) = ids
        val p = { driver.state.projectedState }

        p().getPower(figure) shouldBe 1

        driver.activate(me, figure, spiritId, 1)
        p().getPower(figure) shouldBe 2
        p().getToughness(figure) shouldBe 2
        p().getSubtypes(figure) shouldBe setOf("Kithkin", "Spirit")

        driver.activate(me, figure, warriorId, 3)
        p().getPower(figure) shouldBe 4
        p().getToughness(figure) shouldBe 4
        p().getSubtypes(figure) shouldBe setOf("Kithkin", "Spirit", "Warrior")

        driver.activate(me, figure, avatarId, 6)
        p().getPower(figure) shouldBe 8
        p().getToughness(figure) shouldBe 8
        p().getSubtypes(figure) shouldBe setOf("Kithkin", "Spirit", "Warrior", "Avatar")
        p().hasKeyword(figure, Keyword.FLYING) shouldBe true
        p().hasKeyword(figure, Keyword.FIRST_STRIKE) shouldBe true

        // No duration: still an 8/8 on the next turn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        p().getPower(figure) shouldBe 8
        p().hasKeyword(figure, Keyword.FLYING) shouldBe true
    }

    test("the second and third abilities do nothing unless it is already the prerequisite type") {
        val (driver, ids) = setup()
        val (me, figure) = ids
        val p = { driver.state.projectedState }

        driver.activate(me, figure, warriorId, 3) // not a Spirit
        p().getPower(figure) shouldBe 1
        p().getSubtypes(figure) shouldBe setOf("Kithkin")

        driver.activate(me, figure, avatarId, 6) // not a Warrior
        p().getPower(figure) shouldBe 1
        p().hasKeyword(figure, Keyword.FLYING) shouldBe false
    }

    test("re-activating the first ability on the Avatar makes it a 2/2 Kithkin Spirit that keeps flying and first strike") {
        val (driver, ids) = setup()
        val (me, figure) = ids
        val p = { driver.state.projectedState }

        driver.activate(me, figure, spiritId, 1)
        driver.activate(me, figure, warriorId, 3)
        driver.activate(me, figure, avatarId, 6)
        driver.activate(me, figure, spiritId, 1)

        p().getPower(figure) shouldBe 2
        p().getToughness(figure) shouldBe 2
        p().getSubtypes(figure) shouldBe setOf("Kithkin", "Spirit")
        p().hasKeyword(figure, Keyword.FLYING) shouldBe true
        p().hasKeyword(figure, Keyword.FIRST_STRIKE) shouldBe true
    }
})
