package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The auto-payer sees mana abilities a *resolved effect* granted to a permanent
 * (`GameState.grantedActivatedAbilities` — Glorious Sunrise's "{T}: Add {G}{G}{G}", Hydro-Man's
 * "{T}: Add {U}", Emrakul, the Exigent Doom's "{T}: Add {C}{C}"), not only printed and statically
 * granted ones — and it keeps each ability's amount with the kind it produces.
 *
 * A Forest that gained "{T}: Add {C}{C}" taps for {G} **or** {C}{C}: one tap activates one ability.
 * Folding the two into one "2 mana, green or colorless" source would promise {G}{G}.
 */
class RuntimeGrantedManaAbilityAutoTapTest : FunSpec({

    val bear = card("Test Granted-Mana Bear") {
        typeLine = "Creature — Bear"
        manaCost = "{2}{G}"
        colorIdentity = "G"
        power = 3
        toughness = 3
    }

    val allTestCards = TestCards.all + listOf(bear)

    val twoColorless = ActivatedAbility(
        id = AbilityId("RuntimeGrantedManaAbilityAutoTapTest_1"),
        cost = AbilityCost.Tap,
        effect = Effects.AddColorlessMana(2),
        isManaAbility = true,
        timing = TimingRule.ManaAbility,
    )

    data class Board(val driver: GameTestDriver, val playerId: EntityId, val granted: EntityId, val plain: EntityId?)

    /** A Forest carrying a granted "{T}: Add {C}{C}", optionally beside a plain Forest. */
    fun board(withPlainForest: Boolean, duration: Duration = Duration.Permanent, sourceId: EntityId? = null): Board {
        val driver = GameTestDriver()
        driver.registerCards(allTestCards)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val playerId = driver.activePlayer!!
        val granted = driver.putLandOnBattlefield(playerId, "Forest")
        val plain = if (withPlainForest) driver.putLandOnBattlefield(playerId, "Forest") else null
        driver.replaceState(
            driver.state.copy(
                grantedActivatedAbilities = driver.state.grantedActivatedAbilities +
                    GrantedActivatedAbility(granted, twoColorless, duration, sourceId)
            )
        )
        return Board(driver, playerId, granted, plain)
    }

    fun solver(): ManaSolver {
        val registry = CardRegistry().apply { register(allTestCards) }
        return ManaSolver(registry, PredicateEvaluator(registry))
    }

    test("the granted ability joins the land's mana source with its own per-kind amount") {
        val (driver, playerId, granted) = board(withPlainForest = false)

        val source = solver().findAvailableManaSources(driver.state, playerId).single { it.entityId == granted }

        source.producesColors shouldContain Color.GREEN
        source.producesColorless shouldBe true
        source.amountFor(Color.GREEN) shouldBe 1
        source.amountFor(null) shouldBe 2
        solver().getAvailableManaCount(driver.state, playerId) shouldBe 2
    }

    test("generic is paid with the {C}{C}, from one tap") {
        val (driver, playerId, granted) = board(withPlainForest = false)

        val solution = solver().solve(driver.state, playerId, ManaCost.parse("{2}"))

        solution.shouldNotBeNull()
        solution.sources.map { it.entityId } shouldBe listOf(granted)
        solution.manaProduced[granted] shouldBe ManaProduction(colorless = 2)
    }

    test("one tap never makes {G}{G}") {
        val (driver, playerId) = board(withPlainForest = false)

        solver().solve(driver.state, playerId, ManaCost.parse("{G}{G}")).shouldBeNull()
        solver().solve(driver.state, playerId, ManaCost.parse("{1}{G}")).shouldBeNull()
        solver().solve(driver.state, playerId, ManaCost.parse("{G}")).shouldNotBeNull()
    }

    test("beside a plain Forest: {2}{G} is payable ({G} + {C}{C}) but {1}{G}{G} is not") {
        val (driver, playerId, granted, plain) = board(withPlainForest = true)

        val solution = solver().solve(driver.state, playerId, ManaCost.parse("{2}{G}"))
        solution.shouldNotBeNull()
        solution.sources.map { it.entityId } shouldContainExactlyInAnyOrder listOf(granted, plain!!)
        solution.manaProduced[plain] shouldBe ManaProduction(color = Color.GREEN, amount = 1)
        solution.manaProduced[granted] shouldBe ManaProduction(colorless = 2)

        solver().solve(driver.state, playerId, ManaCost.parse("{1}{G}{G}")).shouldBeNull()
    }

    test("auto-pay casts a spell off the granted ability and floats nothing") {
        val (driver, playerId, granted, plain) = board(withPlainForest = true)
        val bearId = driver.putCardInHand(playerId, "Test Granted-Mana Bear")

        driver.castSpell(playerId, bearId)
        driver.bothPass()

        driver.state.getBattlefield(playerId) shouldContain bearId
        driver.state.getEntity(granted)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(plain!!)?.has<TappedComponent>() shouldBe true
        val pool = driver.state.getEntity(playerId)?.get<ManaPoolComponent>()
        (pool?.colorless ?: 0) shouldBe 0
        (pool?.green ?: 0) shouldBe 0
    }

    test("a grant whose \"for as long as\" duration has already failed is not a source") {
        // Source-keyed to a permanent that isn't on the battlefield: the gate is closed even before
        // the state-based check removes the grant.
        val (driver, playerId, granted) = board(
            withPlainForest = false,
            duration = Duration.WhileSourceOnBattlefield(),
            sourceId = EntityId.generate(),
        )

        val source = solver().findAvailableManaSources(driver.state, playerId).single { it.entityId == granted }
        source.producesColorless shouldBe false
        solver().solve(driver.state, playerId, ManaCost.parse("{2}")).shouldBeNull()
    }
})
