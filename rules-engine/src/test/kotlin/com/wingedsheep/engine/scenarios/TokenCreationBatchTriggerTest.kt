package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.shouldBe

/**
 * "Whenever you create one or more creature tokens" — `TokenCreationEvent(batch = true)`
 * (Staff of the Storyteller). The batch shape fires **once** per simultaneous creation however
 * many matching tokens it made (CR 603.2c), never for a creation with no matching token, never for
 * an opponent's tokens, and never for token copies of permanent spells, which are not "created"
 * (CR 111.13, 608.3f). The singular per-token shape (Mirkwood Bats) is unchanged alongside it.
 *
 * The observers gain 1 life per trigger, so a life total counts how many times each fired.
 */
class TokenCreationBatchTriggerTest : ScenarioTestBase() {

    private val batchObserver = card("Batch Token Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.createsToken(GameObjectFilter.Creature, batch = true)
            effect = Effects.GainLife(1)
        }
    }

    private val perTokenObserver = card("Per Token Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.createsToken(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }

    private val threeSpirits = card("Make Three Spirits") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Spirit"), count = 3) }
    }

    private val oneSpirit = card("Make One Spirit") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Spirit"), count = 1) }
    }

    private val twoTreasures = card("Make Two Treasures") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(2) }
    }

    private val treasureAndSpirit = card("Make Treasure And Spirit") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = Effects.CreateTreasure(1) then
                Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Spirit"), count = 1)
        }
    }

    init {
        cardRegistry.register(listOf(batchObserver, perTokenObserver, threeSpirits, oneSpirit, twoTreasures, treasureAndSpirit))

        fun game(observer: String, caster: Int = 1, vararg hand: String) = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, observer)
            .apply { hand.forEach { withCardInHand(caster, it) } }
            .withCardInLibrary(1, "Make One Spirit")
            .withCardInLibrary(2, "Make One Spirit")
            .withActivePlayer(caster)
            .withPriorityPlayer(caster)
            .build()

        test("three creature tokens created at once trigger the batch shape once") {
            val g = game("Batch Token Observer", 1, "Make Three Spirits")
            g.castSpell(1, "Make Three Spirits").error shouldBe null
            g.resolveStack()

            g.findPermanents("Spirit Token").size shouldBe 3
            g.getLifeTotal(1) shouldBe 21
        }

        test("the per-token shape still triggers once per token for the same creation") {
            val g = game("Per Token Observer", 1, "Make Three Spirits")
            g.castSpell(1, "Make Three Spirits").error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 23
        }

        test("a single creature token triggers the batch shape once") {
            val g = game("Batch Token Observer", 1, "Make One Spirit")
            g.castSpell(1, "Make One Spirit").error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 21
        }

        test("two separate creations trigger it twice") {
            val g = game("Batch Token Observer", 1, "Make One Spirit", "Make Three Spirits")
            g.castSpell(1, "Make One Spirit").error shouldBe null
            g.resolveStack()
            g.castSpell(1, "Make Three Spirits").error shouldBe null
            g.resolveStack()

            g.findPermanents("Spirit Token").size shouldBe 4
            g.getLifeTotal(1) shouldBe 22
        }

        test("noncreature tokens don't satisfy a creature-token batch") {
            val g = game("Batch Token Observer", 1, "Make Two Treasures")
            g.castSpell(1, "Make Two Treasures").error shouldBe null
            g.resolveStack()

            g.findPermanents("Treasure").size shouldBe 2
            g.getLifeTotal(1) shouldBe 20
        }

        test("a batch with one matching creature token among noncreature tokens triggers once") {
            val g = game("Batch Token Observer", 1, "Make Treasure And Spirit")
            g.castSpell(1, "Make Treasure And Spirit").error shouldBe null
            g.resolveStack()

            g.findPermanents("Treasure").size shouldBe 1
            g.findPermanents("Spirit Token").size shouldBe 1
            g.getLifeTotal(1) shouldBe 21
        }

        test("tokens an opponent creates don't trigger your batch observer") {
            val g = game("Batch Token Observer", 2, "Make Three Spirits")
            g.castSpell(2, "Make Three Spirits").error shouldBe null
            g.resolveStack()

            g.findPermanents("Spirit Token").size shouldBe 3
            g.getLifeTotal(1) shouldBe 20
            g.getLifeTotal(2) shouldBe 20
        }
    }
}

/**
 * Token copies of a permanent spell are not "created" (CR 111.13, 608.3f): two storm copies of a
 * creature spell resolve into creature tokens without triggering the batch observer.
 */
class TokenCreationBatchTriggerStormCopyTest : io.kotest.core.spec.style.FunSpec({

    val stormBear = CardDefinition.creature(
        name = "Storm Batch Bear",
        manaCost = ManaCost.parse("{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.STORM)
    )
    val observer = card("Batch Token Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.createsToken(GameObjectFilter.Creature, batch = true)
            effect = Effects.GainLife(1)
        }
    }

    test("storm copies of a creature spell become tokens without being created") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(stormBear, observer))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        driver.putPermanentOnBattlefield(caster, "Batch Token Observer")
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 2))
        driver.putLandOnBattlefield(caster, "Forest")
        val bear = driver.putCardInHand(caster, "Storm Batch Bear")

        driver.castSpell(caster, bear).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 10) {
            driver.bothPass()
            guard++
        }

        val bears = driver.state.getBattlefield(caster).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Storm Batch Bear"
        }
        bears.count { driver.state.getEntity(it)!!.has<TokenComponent>() } shouldBe 2
        driver.getLifeTotal(caster) shouldBe 20
    }
})
