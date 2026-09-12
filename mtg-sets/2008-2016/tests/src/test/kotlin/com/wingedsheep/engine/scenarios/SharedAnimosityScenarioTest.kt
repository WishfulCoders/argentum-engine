package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.SharedAnimosity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shared Animosity (MOR #104) and `StatePredicate.IsTriggeringEntity`, which its "each **other**
 * attacking creature that shares a creature type with it" introduced.
 *
 * The first case is the card's own ruling, verbatim: an Elf Shaman, an Elf Warrior, a Goblin
 * Shaman, an Elemental and a creature with every creature type attack together and get +3/+0,
 * +2/+0, +2/+0, +1/+0 and +4/+0. The others pin the "other" (a lone attacker gets nothing) and
 * that only attacking creatures count.
 */
class SharedAnimosityScenarioTest : FunSpec({

    fun bear(name: String, vararg types: String, keywords: Set<Keyword> = emptySet()) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}"),
        subtypes = types.map { Subtype(it) }.toSet(),
        power = 1,
        toughness = 1,
        keywords = keywords,
        oracleText = ""
    )

    val elfShaman = bear("Test Elf Shaman", "Elf", "Shaman")
    val elfWarrior = bear("Test Elf Warrior", "Elf", "Warrior")
    val goblinShaman = bear("Test Goblin Shaman", "Goblin", "Shaman")
    val elemental = bear("Test Elemental", "Elemental")
    val shapeshifter = bear("Test Shapeshifter", "Shapeshifter", keywords = setOf(Keyword.CHANGELING))

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SharedAnimosity, elfShaman, elfWarrior, goblinShaman, elemental, shapeshifter)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(driver.player1, "Shared Animosity")
        return driver
    }

    fun GameTestDriver.creature(name: String) =
        putCreatureOnBattlefield(player1, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 30) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.power(id: com.wingedsheep.sdk.model.EntityId) = state.projectedState.getPower(id)

    test("the ruling's five attackers get +3, +2, +2, +1 and +4") {
        val driver = newDriver()
        val you = driver.player1
        val names = listOf("Test Elf Shaman", "Test Elf Warrior", "Test Goblin Shaman", "Test Elemental", "Test Shapeshifter")
        val ids = names.map { driver.creature(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, ids, driver.getOpponent(you)).error shouldBe null
        driver.resolveStack()

        driver.assertStep(Step.DECLARE_ATTACKERS)
        withClue("each is a 1/1, so power is 1 + its bonus") {
            ids.map { driver.power(it) } shouldBe listOf(4, 3, 3, 2, 5)
        }
    }

    test("a lone attacker shares a type only with itself, which is not another creature") {
        val driver = newDriver()
        val you = driver.player1
        val shaman = driver.creature("Test Elf Shaman")
        driver.creature("Test Elf Warrior") // stays home

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(shaman), driver.getOpponent(you)).error shouldBe null
        driver.resolveStack()

        withClue("the Elf Warrior that did not attack does not count") {
            driver.power(shaman) shouldBe 1
        }
    }
})
