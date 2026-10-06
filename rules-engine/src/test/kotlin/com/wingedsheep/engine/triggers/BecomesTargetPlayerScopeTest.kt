package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.BecomesTargetEvent.targetPlayer` — the player half of a becomes-target trigger,
 * narrowed relative to the trigger's controller and kept apart from the object half
 * (`targetFilter`). The wording is Leovold, Emissary of Trest's "Whenever **you or a permanent you
 * control** becomes the target of a spell or ability an opponent controls" (CR 115.1: objects
 * *and/or players* are targets; CR 603.2: the trigger fires once per matching event, so "you and a
 * permanent you control" targeted by the same spell triggers twice — the Leovold ruling).
 */
class BecomesTargetPlayerScopeTest : FunSpec({

    val leovoldLike = card("You Or Your Permanent Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Elf Advisor"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).becomesTarget(
                byOpponent = true,
                includePlayerTargets = true,
                targetPlayer = Player.You,
            )
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + leovoldLike)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun event(targetId: EntityId, by: EntityId, isPlayer: Boolean) = BecomesTargetEvent(
        targetEntityId = targetId,
        targetName = "",
        sourceEntityId = EntityId.generate(),
        controllerId = by,
        firstTimeByThisController = true,
        targetIsSpell = false,
        sourceIsSpell = true,
        targetIsPlayer = isPlayer,
    )

    fun firings(driver: GameTestDriver, events: List<BecomesTargetEvent>, observerId: EntityId) =
        TriggerDetector(
            driver.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = null),
            conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions
        ).detectTriggers(driver.state, events)
            .filter { it.ability.trigger is EventPattern.BecomesTargetEvent && it.sourceId == observerId }

    test("fires when an opponent's spell targets you") {
        val driver = createDriver()
        val observer = driver.putCreatureOnBattlefield(driver.player1, "You Or Your Permanent Observer")
        firings(driver, listOf(event(driver.player1, driver.player2, isPlayer = true)), observer) shouldHaveSize 1
    }

    test("does not fire when an opponent targets themselves — the player half is \"you\" only") {
        val driver = createDriver()
        val observer = driver.putCreatureOnBattlefield(driver.player1, "You Or Your Permanent Observer")
        firings(driver, listOf(event(driver.player2, driver.player2, isPlayer = true)), observer) shouldHaveSize 0
    }

    test("fires for a permanent you control, not for an opponent's permanent") {
        val driver = createDriver()
        val observer = driver.putCreatureOnBattlefield(driver.player1, "You Or Your Permanent Observer")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        firings(driver, listOf(event(mine, driver.player2, isPlayer = false)), observer) shouldHaveSize 1
        firings(driver, listOf(event(theirs, driver.player2, isPlayer = false)), observer) shouldHaveSize 0
    }

    test("byOpponent still applies to both halves") {
        val driver = createDriver()
        val observer = driver.putCreatureOnBattlefield(driver.player1, "You Or Your Permanent Observer")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        firings(driver, listOf(event(driver.player1, driver.player1, isPlayer = true)), observer) shouldHaveSize 0
        firings(driver, listOf(event(mine, driver.player1, isPlayer = false)), observer) shouldHaveSize 0
    }

    test("you and a permanent you control targeted by the same spell trigger it twice (CR 603.2)") {
        val driver = createDriver()
        val observer = driver.putCreatureOnBattlefield(driver.player1, "You Or Your Permanent Observer")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val both = listOf(
            event(driver.player1, driver.player2, isPlayer = true),
            event(mine, driver.player2, isPlayer = false),
        )
        firings(driver, both, observer) shouldHaveSize 2
    }

    test("pattern data: description, and targetPlayer needs the player opt-in") {
        EventPattern.BecomesTargetEvent(
            targetFilter = GameObjectFilter.Permanent.youControl(),
            byOpponent = true,
            includePlayerTargets = true,
            targetPlayer = Player.You,
        ).description shouldBe
            "you or a permanent you control becomes the target of a spell or ability an opponent controls"
        withClue("Loki's rendering is unchanged") {
            EventPattern.BecomesTargetEvent(includePlayerTargets = true).description shouldBe
                "a player or permanent becomes the target of a spell or ability"
        }
        shouldThrow<IllegalArgumentException> {
            EventPattern.BecomesTargetEvent(targetPlayer = Player.You)
        }
    }
})
