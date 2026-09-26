package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Regression for the gap where [TriggerMatcher.matchesCardPredicate] had no branch for
 * [CardPredicate.NotSubtype] and fell into the `error(...)` guard. Any death/leave trigger
 * gated on a "non-<subtype>" event filter would crash trigger detection rather than match.
 * No registered card hits this path yet — every current `NotSubtype` user puts it in a spell
 * target or effect filter (evaluated elsewhere), not a trigger's event filter — so the guard
 * turns the missing branch into a hard failure instead of a silent miss. The branch is needed
 * to support future "whenever a non-<subtype> creature dies/leaves" triggers. The guard exists
 * precisely because a silent `else -> true` pass-through once hid the IsNontoken bug for months.
 *
 * The trigger shape here is a death trigger filtered on `IsCreature + NotSubtype(Zombie)`:
 * a non-Zombie creature dying must fire it; a Zombie dying must not.
 */
class TriggerMatcherNotSubtypeTest : FunSpec({

    val nonZombieDeathWatcher = card("Non-Zombie Death Watcher") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Enchantment"
        oracleText = "Whenever a non-Zombie creature dies, draw a card."

        spell {}

        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.NotSubtype(Subtype("Zombie")),
                        ),
                    )).dies()
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + nonZombieDeathWatcher)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.putPermanentOnBattlefield(driver.player1, "Non-Zombie Death Watcher")
        return driver
    }

    fun detectorFor(driver: GameTestDriver): TriggerDetector =
        TriggerDetector(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null), conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)

    test("non-Zombie creature dying fires the non-Zombie death trigger") {
        val driver = createDriver()
        // Goblin Guide is a Goblin/Scout — a non-Zombie creature.
        val dead = driver.putCardInGraveyard(driver.player1, "Goblin Guide")
        val event = ZoneChangeEvent(
            entityId = dead,
            entityName = "Goblin Guide",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.GRAVEYARD,
            ownerId = driver.player1,
        )

        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

        triggers.filter {
            it.ability.trigger is EventPattern.ZoneChangeEvent
        } shouldHaveSize 1
    }

    test("Zombie creature dying does NOT fire the non-Zombie death trigger") {
        val driver = createDriver()
        // Fear Creature is a Zombie — the NotSubtype(Zombie) predicate must reject it.
        val dead = driver.putCardInGraveyard(driver.player1, "Fear Creature")
        val event = ZoneChangeEvent(
            entityId = dead,
            entityName = "Fear Creature",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.GRAVEYARD,
            ownerId = driver.player1,
        )

        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

        triggers.filter {
            it.ability.trigger is EventPattern.ZoneChangeEvent
        }.shouldBeEmpty()
    }
})
