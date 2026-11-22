package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The declare attackers step happens even when the active player has no creature able to attack.
 *
 * CR 508.8 skips the declare blockers and combat damage steps after an empty declaration, not the
 * declare attackers step itself: the active player declares no attackers (CR 508.1) and then gets
 * priority (CR 508.2). The engine makes the empty declaration as the turn-based action, so the
 * window costs the active player nothing but a pass, and "at the beginning of the declare attackers
 * step" abilities see the step. Issue #2704.
 */
class DeclareAttackersStepWithoutAttackersTest : FunSpec({

    /** Forests only: neither player ever controls a creature. */
    fun creaturelessGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        return driver
    }

    test("the step is entered with the empty declaration made and the active player holding priority") {
        val driver = creaturelessGame()
        val active = driver.activePlayer!!
        val turn = driver.state.turnNumber

        driver.bothPass()

        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.state.turnNumber shouldBe turn
        driver.priorityPlayer shouldBe active
        driver.state.getEntity(active)?.has<AttackersDeclaredThisCombatComponent>() shouldBe true
        driver.state.getEntity(active)?.has<AttackersDeclaredThisTurnComponent>() shouldBe true
    }

    test("both players get priority in the step, then blockers and damage are skipped (CR 508.8)") {
        val driver = creaturelessGame()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.bothPass()

        // The active player passes straight away: there is no declaration left to make.
        driver.submit(PassPriority(active)).outcome shouldBe Outcome.Done
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.priorityPlayer shouldBe defender

        driver.submit(PassPriority(defender)).outcome shouldBe Outcome.Done
        driver.currentStep shouldBe Step.END_COMBAT
    }

    test("a second declaration is refused — the turn-based action already made the only legal one") {
        val driver = creaturelessGame()
        val active = driver.activePlayer!!
        driver.bothPass()

        driver.submitExpectFailure(DeclareAttackers(active, emptyMap())).outcome shouldNotBe Outcome.Done
    }

    test("an 'at the beginning of the declare attackers step' ability triggers") {
        val driver = creaturelessGame()
        val active = driver.activePlayer!!
        val ability = TriggeredAbility.create(
            id = AbilityId("DeclareAttackersStepWithoutAttackersTest_1"),
            trigger = EventPattern.StepEvent(Step.DECLARE_ATTACKERS, Player.You),
            effect = GainLifeEffect(1),
        )
        driver.replaceState(
            driver.state.copy(
                globalGrantedTriggeredAbilities = listOf(
                    GlobalGrantedTriggeredAbility(
                        ability = ability,
                        controllerId = active,
                        sourceId = active,
                        sourceName = "Test Declare Attackers Step Ability",
                        duration = Duration.Permanent,
                    )
                )
            )
        )
        val lifeBefore = driver.getLifeTotal(active)

        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.stackSize shouldBe 1

        driver.bothPass()
        driver.getLifeTotal(active) shouldBe lifeBefore + 1
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
    }
})
