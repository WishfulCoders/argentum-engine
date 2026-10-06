package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.Fading
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mechanic-level tests for Fading N (CR 702.32).
 *
 * CR 702.32a: "Fading N" means "This permanent enters with N fade counters on it" and "At the
 * beginning of your upkeep, remove a fade counter from this permanent. If you can't, sacrifice the
 * permanent." The engine supplies both from [Fading] — the replacement at the entry seam, the
 * upkeep ability as a projected-keyword-derived trigger.
 *
 * What separates it from vanishing, and is pinned here:
 *  - the permanent survives the upkeep that removes its *last* counter and is sacrificed at the
 *    *next* one, when there is nothing left to remove;
 *  - stripping its counters off-turn does not sacrifice it on the spot;
 *  - "if you can't" is judged on resolution, so draining the counter with the trigger on the stack
 *    turns that upkeep's removal into a sacrifice;
 *  - only fade counters count (time counters are not fuel);
 *  - the countdown is the controller's upkeep only, and gained fading fades too.
 */
class FadingKeywordTest : FunSpec({

    val fadingBear = card("Fading Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Fading 1"
        keywordAbility(KeywordAbility.fading(1))
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    /** Removes a fade counter at instant speed. */
    val fadeSiphon = card("Fade Siphon") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Remove a fade counter from target creature."
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.RemoveCounters(CounterType.FADE, 1, victim)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(fadingBear, plainBear, fadeSiphon))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, perm: EntityId, type: CounterType): Int =
        driver.state.getEntity(perm)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun castSpell(driver: GameTestDriver, player: EntityId, cardName: String, targets: List<EntityId> = emptyList()) {
        driver.giveMana(player, Color.GREEN, 3)
        driver.giveMana(player, Color.BLUE, 3)
        val cardId = driver.putCardInHand(player, cardName)
        val result = driver.submit(CastSpell(player, cardId, targets.map { ChosenTarget.Permanent(it) }))
        if (result.outcome !is Outcome.Done) throw AssertionError("cast of $cardName failed: ${result.error}")
    }

    fun castAndResolve(driver: GameTestDriver, player: EntityId, cardName: String, targets: List<EntityId> = emptyList()) {
        castSpell(driver, player, cardName, targets)
        driver.bothPass()
    }

    /** Advance to [owner]'s next upkeep, leaving the fading trigger on the stack. */
    fun reachNextOwnerUpkeep(driver: GameTestDriver, owner: EntityId) {
        do {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.passPriorityUntil(Step.UPKEEP)
        } while (driver.activePlayer != owner)
    }

    fun resolveNextOwnerUpkeep(driver: GameTestDriver, owner: EntityId) {
        reachNextOwnerUpkeep(driver, owner)
        driver.bothPass()
    }

    test("a declared Fading N enters the battlefield with N fade counters and no time counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        castAndResolve(driver, player, "Fading Bear")

        val bear = driver.findPermanent(player, "Fading Bear")!!
        counters(driver, bear, CounterType.FADE) shouldBe 1
        counters(driver, bear, CounterType.TIME) shouldBe 0
    }

    test("it survives the upkeep that removes its last counter and is sacrificed at the next") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        castAndResolve(driver, player, "Fading Bear")
        val bear = driver.findPermanent(player, "Fading Bear")!!

        // The opponent's upkeep comes first and must not fade anything.
        driver.passPriorityUntil(Step.UPKEEP)
        (driver.activePlayer == player) shouldBe false
        counters(driver, bear, CounterType.FADE) shouldBe 1

        resolveNextOwnerUpkeep(driver, player)
        counters(driver, bear, CounterType.FADE) shouldBe 0
        driver.findPermanent(player, "Fading Bear").shouldNotBeNull()

        // Nothing left to remove: the same single ability sacrifices it — no second trigger.
        resolveNextOwnerUpkeep(driver, player)
        driver.findPermanent(player, "Fading Bear").shouldBeNull()
        driver.getGraveyardCardNames(player).contains("Fading Bear") shouldBe true
    }

    test("draining its counters outside the upkeep does not sacrifice it until its controller's next upkeep") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        castAndResolve(driver, player, "Fading Bear")
        val bear = driver.findPermanent(player, "Fading Bear")!!

        castAndResolve(driver, player, "Fade Siphon", targets = listOf(bear))
        counters(driver, bear, CounterType.FADE) shouldBe 0
        driver.findPermanent(player, "Fading Bear").shouldNotBeNull()

        // Time counters are not fade counters — they do not keep it alive.
        driver.replaceState(driver.state.updateEntity(bear) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.TIME, 3))
        })

        resolveNextOwnerUpkeep(driver, player)
        driver.findPermanent(player, "Fading Bear").shouldBeNull()
    }

    test("'if you can't' is judged on resolution: draining the counter in response sacrifices it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        castAndResolve(driver, player, "Fading Bear")
        val bear = driver.findPermanent(player, "Fading Bear")!!

        reachNextOwnerUpkeep(driver, player)
        counters(driver, bear, CounterType.FADE) shouldBe 1 // trigger waiting on the stack

        castAndResolve(driver, player, "Fade Siphon", targets = listOf(bear))
        counters(driver, bear, CounterType.FADE) shouldBe 0
        driver.findPermanent(player, "Fading Bear").shouldNotBeNull()

        driver.bothPass() // the fading trigger now finds nothing to remove
        driver.findPermanent(player, "Fading Bear").shouldBeNull()
    }

    test("a creature that gains fading fades too") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        castAndResolve(driver, player, "Plain Bear")
        val bear = driver.findPermanent(player, "Plain Bear")!!

        val s = driver.state.addFloatingEffects(
            listOf(
                ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.ABILITY,
                        modification = SerializableModification.GrantKeyword(Keyword.FADING.name),
                        affectedEntities = setOf(bear),
                    ),
                    duration = Duration.Permanent,
                    sourceId = bear,
                    sourceName = "Plain Bear",
                    controllerId = player,
                    timestamp = driver.state.timestamp,
                )
            )
        )
        driver.replaceState(s)
        driver.state.projectedState.hasKeyword(bear, Keyword.FADING) shouldBe true

        // No fade counters at all: its first upkeep already finds nothing to remove.
        resolveNextOwnerUpkeep(driver, player)
        driver.findPermanent(player, "Plain Bear").shouldBeNull()
    }

    test("Fading.printedCount sums multiple printed instances") {
        val doubled = card("Doubly Fading Bear") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
            keywordAbilities(KeywordAbility.fading(2), KeywordAbility.fading(3))
        }
        Fading.printedCount(doubled) shouldBe 5
        Fading.printedCount(plainBear).shouldBeNull()
    }
})
