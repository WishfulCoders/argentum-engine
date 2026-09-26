package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.core.Subtype

/**
 * Tests for AddCreatureTypeEffect.
 * Verifies that a creature subtype is added in addition to existing types.
 */
class AddCreatureTypeTest : FunSpec({

    val ZombifySpell = CardDefinition(
        name = "Zombify Spell",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "Target creature becomes a Zombie in addition to its other types until end of turn.",
        script = CardScript.spell(
            effect = AddCreatureTypeEffect(
                subtype = "Zombie",
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn
            ),
            TargetObject(filter = TargetFilter.Creature)
        )
    )

    val TestElf = CardDefinition(
        name = "Test Elf",
        manaCost = ManaCost.parse("{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Elf"))),
        oracleText = "",
        creatureStats = CreatureStats(1, 1)
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZombifySpell, TestElf))
        return driver
    }

    test("adds subtype in addition to existing types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val elf = driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        // Verify starting subtypes
        val projectedBefore = projector.project(driver.state)
        projectedBefore.hasSubtype(elf, "Elf") shouldBe true
        projectedBefore.hasSubtype(elf, "Zombie") shouldBe false

        // Give mana and put spell in hand
        driver.giveMana(activePlayer, Color.BLUE, 1)
        val spellId = driver.putCardInHand(activePlayer, "Zombify Spell")

        // Cast the spell targeting the elf
        driver.castSpell(activePlayer, spellId, targets = listOf(elf))
        driver.bothPass()

        // Verify subtypes after: should have BOTH Elf and Zombie
        val projectedAfter = projector.project(driver.state)
        projectedAfter.hasSubtype(elf, "Elf") shouldBe true
        projectedAfter.hasSubtype(elf, "Zombie") shouldBe true
        projectedAfter.isCreature(elf) shouldBe true
    }

    test("permanent duration keeps subtype after turn ends") {
        val permanentSpell = CardDefinition(
            name = "Permanent Zombify",
            manaCost = ManaCost.parse("{U}"),
            typeLine = TypeLine.parse("Instant"),
            oracleText = "Target creature becomes a Zombie in addition to its other types.",
            script = CardScript.spell(
                effect = AddCreatureTypeEffect(
                    subtype = "Zombie",
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.Permanent
                ),
                TargetObject(filter = TargetFilter.Creature)
            )
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(permanentSpell, TestElf))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val elf = driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        driver.giveMana(activePlayer, Color.BLUE, 1)
        val spellId = driver.putCardInHand(activePlayer, "Permanent Zombify")
        driver.castSpell(activePlayer, spellId, targets = listOf(elf))
        driver.bothPass()

        // Advance to next turn — permanent duration should persist
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val projected = projector.project(driver.state)
        projected.hasSubtype(elf, "Elf") shouldBe true
        projected.hasSubtype(elf, "Zombie") shouldBe true
    }
})
