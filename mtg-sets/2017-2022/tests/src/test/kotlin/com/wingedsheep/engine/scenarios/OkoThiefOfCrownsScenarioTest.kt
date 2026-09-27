package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.OkoThiefOfCrowns
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oko, Thief of Crowns (ELD #197): one case per loyalty ability, the +1 checked against its
 * rulings — the target is "just a green Elk" 3/3 with no abilities, and an artifact stops being an
 * artifact.
 */
class OkoThiefOfCrownsScenarioTest : FunSpec({

    val relic = card("Test Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(1)
        }
    }

    val flyer = CardDefinition.creature(
        name = "Test Flying Knight",
        manaCost = ManaCost.parse("{3}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(OkoThiefOfCrowns, relic, flyer))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.oko(loyalty: Int): EntityId {
        val oko = putPermanentOnBattlefield(player1, "Oko, Thief of Crowns")
        replaceState(state.updateEntity(oko) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.LOYALTY, loyalty))
        })
        return oko
    }

    fun GameTestDriver.activate(oko: EntityId, index: Int, targets: List<EntityId> = emptyList()) {
        val ability = OkoThiefOfCrowns.script.activatedAbilities[index]
        submit(ActivateAbility(player1, oko, ability.id, targets = targets.map { ChosenTarget.Permanent(it) })).error shouldBe null
        var guard = 0
        while (guard++ < 20) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("+2 makes a Food") {
        val driver = newDriver()
        val oko = driver.oko(4)
        driver.activate(oko, 0)

        driver.getPermanents(driver.player1).count { driver.getCardName(it) == "Food" } shouldBe 1
    }

    test("+1 makes a creature a vanilla green Elk 3/3") {
        val driver = newDriver()
        val opponent = driver.getOpponent(driver.player1)
        val oko = driver.oko(4)
        val knight = driver.putCreatureOnBattlefield(opponent, "Test Flying Knight")
        driver.activate(oko, 1, listOf(knight))

        val p = driver.state.projectedState
        p.getPower(knight) shouldBe 3
        p.getToughness(knight) shouldBe 3
        p.hasKeyword(knight, Keyword.FLYING) shouldBe false
        p.getSubtypes(knight) shouldBe setOf("Elk")
        p.getColors(knight) shouldBe setOf(Color.GREEN.name)
    }

    test("+1 makes a noncreature artifact a creature that is no longer an artifact") {
        val driver = newDriver()
        val opponent = driver.getOpponent(driver.player1)
        val oko = driver.oko(4)
        val artifact = driver.putPermanentOnBattlefield(opponent, "Test Relic")
        driver.activate(oko, 1, listOf(artifact))

        val p = driver.state.projectedState
        withClue("it's just a green Elk (ruling)") {
            p.isCreature(artifact) shouldBe true
            p.hasType(artifact, "ARTIFACT") shouldBe false
            p.getPower(artifact) shouldBe 3
            p.hasLostAllAbilities(artifact) shouldBe true
        }
    }

    test("−5 exchanges control of your artifact and their small creature") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val oko = driver.oko(5)
        val mine = driver.putPermanentOnBattlefield(you, "Test Relic")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Flying Knight")
        driver.activate(oko, 2, listOf(mine, theirs))

        driver.state.projectedState.getController(mine) shouldBe opponent
        driver.state.projectedState.getController(theirs) shouldBe you
    }
})
