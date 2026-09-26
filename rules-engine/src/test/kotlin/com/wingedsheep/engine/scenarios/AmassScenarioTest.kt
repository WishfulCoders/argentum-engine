package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Substrate tests for the Amass mechanic (CR 701.47): "amass [subtype] N" creates a 0/0 black Army
 * token when you control none, then puts N +1/+1 counters on an Army you control and makes it the
 * subtype. Covers token creation, counter stacking onto the same Army, the multi-Army choice, and
 * the printed "When this creature enters, amass Orcs 2" shape (Dunland Crebain).
 */
class AmassScenarioTest : FunSpec({

    val projector = StateProjector()

    // {0} sorceries that just amass, so the substrate can be exercised deterministically.
    val AmassTwo = card("Amass Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Amass Orcs 2."
        spell { effect = Effects.Amass(2, "Orc") }
    }
    val AmassOne = card("Amass One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Amass Orcs 1."
        spell { effect = Effects.Amass(1, "Orc") }
    }

    // Pre-existing Armies for the multi-Army choice. Base 2/2 so they survive state-based actions.
    val ZombieArmyA = card("Zombie Army Alpha") {
        manaCost = "{0}"
        typeLine = "Creature — Zombie Army"
        power = 2; toughness = 2
    }
    val ZombieArmyB = card("Zombie Army Beta") {
        manaCost = "{0}"
        typeLine = "Creature — Zombie Army"
        power = 2; toughness = 2
    }

    // The printed card: {2}{B} 1/1 Bird Horror with flying and "When this creature enters, amass Orcs 2."
    val DunlandCrebain = card("Dunland Crebain") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Bird Horror"
        power = 1; toughness = 1
        keywords(Keyword.FLYING)
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.Amass(2, "Orc")
        }
    }

    // Warbeast of Gorgoroth's dies trigger: "Whenever this creature or another creature you control
    // with power 4 or greater dies, amass Orcs 2." The filter has to read last-known power, since the
    // creature is already in the graveyard when the leaves-the-battlefield trigger is evaluated.
    val Warbeast = card("Warbeast of Gorgoroth") {
        manaCost = "{4}{R}"
        typeLine = "Creature — Beast"
        power = 5; toughness = 4
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl().powerAtLeast(4)).dies()
            effect = Effects.Amass(2, "Orc")
        }
    }

    // Vanilla bodies to die on cue: one at the power-4 boundary, one just below it.
    val BigBeast = card("Cave Troll") {
        manaCost = "{0}"
        typeLine = "Creature — Troll"
        power = 4; toughness = 4
    }
    val SmallBeast = card("Goblin Footman") {
        manaCost = "{0}"
        typeLine = "Creature — Goblin"
        power = 3; toughness = 3
    }

    // {0} sorcery that destroys a target creature, to send a chosen body to the graveyard on demand.
    val Slay = card("Slay") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                AmassTwo, AmassOne, ZombieArmyA, ZombieArmyB, DunlandCrebain,
                Warbeast, BigBeast, SmallBeast, Slay
            )
        )
        return driver
    }

    fun GameTestDriver.armiesControlledBy(player: EntityId): List<EntityId> {
        val projected = projector.project(state)
        return projected.getBattlefieldControlledBy(player)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Cast a {0} amass sorcery and resolve it (no pending decision when you control 0 or 1 Army).
    fun GameTestDriver.amass(player: EntityId, cardName: String) {
        val cardId = putCardInHand(player, cardName)
        castSpell(player, cardId)
        bothPass()
    }

    test("amass with no Army creates a 0/0 black Orc Army token and puts the counters on it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.armiesControlledBy(active).size shouldBe 0

        driver.amass(active, "Amass Two")

        val armies = driver.armiesControlledBy(active)
        armies.size shouldBe 1
        val army = armies.single()

        val projected = projector.project(driver.state)
        projected.hasSubtype(army, "Orc") shouldBe true
        projected.hasSubtype(army, "Army") shouldBe true
        projected.getPower(army) shouldBe 2
        projected.getToughness(army) shouldBe 2
        driver.plusOneCounters(army) shouldBe 2
    }

    test("amassing again grows the same Army and creates no second Army") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.amass(active, "Amass Two")
        val army = driver.armiesControlledBy(active).single()

        driver.amass(active, "Amass One")

        driver.armiesControlledBy(active) shouldBe listOf(army)
        driver.plusOneCounters(army) shouldBe 3
        projector.project(driver.state).getPower(army) shouldBe 3
    }

    test("with multiple Armies, the controller chooses which one is amassed (CR 701.47a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val alpha = driver.putCreatureOnBattlefield(active, "Zombie Army Alpha")
        val beta = driver.putCreatureOnBattlefield(active, "Zombie Army Beta")

        val cardId = driver.putCardInHand(active, "Amass Two")
        driver.castSpell(active, cardId)
        driver.bothPass()

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        decision.options.toSet() shouldBe setOf(alpha, beta)
        driver.submitDecision(active, CardsSelectedResponse(decision.id, listOf(alpha)))

        // Alpha received the counters and became an Orc; Beta is untouched.
        driver.plusOneCounters(alpha) shouldBe 2
        projector.project(driver.state).hasSubtype(alpha, "Orc") shouldBe true
        driver.plusOneCounters(beta) shouldBe 0
        projector.project(driver.state).hasSubtype(beta, "Orc") shouldBe false
    }

    test("Dunland Crebain's enter trigger amasses Orcs 2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(active, Color.BLACK, 3)
        val crebain = driver.putCardInHand(active, "Dunland Crebain")
        driver.castSpell(active, crebain)
        driver.bothPass() // resolve the creature spell — it enters and its ETB trigger goes on the stack
        driver.bothPass() // resolve the "amass Orcs 2" triggered ability

        driver.findPermanent(active, "Dunland Crebain") shouldNotBe null
        val army = driver.armiesControlledBy(active).single()
        projector.project(driver.state).getPower(army) shouldBe 2
        driver.plusOneCounters(army) shouldBe 2
    }

    // Cast {0} "Slay" on a target creature and resolve it (the creature dies).
    fun GameTestDriver.slay(player: EntityId, victim: EntityId) {
        val slayId = putCardInHand(player, "Slay")
        castSpell(player, slayId, listOf(victim))
        bothPass()
    }

    test("Warbeast dies and triggers off its own last-known power 5 (amass Orcs 2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warbeast = driver.putCreatureOnBattlefield(active, "Warbeast of Gorgoroth")
        driver.armiesControlledBy(active).size shouldBe 0

        driver.slay(active, warbeast)   // Slay resolves → Warbeast dies → its leaves trigger goes on the stack
        driver.bothPass()               // resolve "amass Orcs 2"

        // The dies trigger fired even though Warbeast is in the graveyard: a 2/2 Orc Army now exists.
        val army = driver.armiesControlledBy(active).single()
        projector.project(driver.state).getPower(army) shouldBe 2
        driver.plusOneCounters(army) shouldBe 2
    }

    test("another power-4 creature dying triggers Warbeast via last-known power (amass Orcs 2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Warbeast of Gorgoroth")
        val troll = driver.putCreatureOnBattlefield(active, "Cave Troll")
        driver.armiesControlledBy(active).size shouldBe 0

        driver.slay(active, troll)      // the 4/4 dies; Warbeast's trigger must read its last-known power 4
        driver.bothPass()               // resolve "amass Orcs 2"

        val army = driver.armiesControlledBy(active).single()
        projector.project(driver.state).getPower(army) shouldBe 2
        driver.plusOneCounters(army) shouldBe 2
    }

    test("a power-3 creature dying does not trigger Warbeast (no amass)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Warbeast of Gorgoroth")
        val footman = driver.putCreatureOnBattlefield(active, "Goblin Footman")

        driver.slay(active, footman)    // 3/3 is below the power-4 threshold
        driver.bothPass()

        // No trigger, so no Army token was created.
        driver.armiesControlledBy(active).size shouldBe 0
    }
})
