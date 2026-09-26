package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Regression for https://github.com/wingedsheep/argentum-engine/issues/1250 — "it only understands
 * the single-color lands": a {1}{R}{G} spell couldn't be cast despite plenty of lands in play.
 *
 * A land whose single tap ability adds two DIFFERENT colored mana is scripted as a
 * [com.wingedsheep.sdk.scripting.effects.CompositeEffect] via `.then()` — e.g. Gruul Turf's
 * `{T}: Add {R}{G}` is `AddMana(RED).then(AddMana(GREEN))`. The [ManaSolver] source enumerator
 * unwrapped a composite to its FIRST mana leaf only, so it saw Gruul Turf as a mono-red source and
 * never knew it could make green. A {1}{R}{G} spell then looked unaffordable and the client never
 * highlighted it.
 *
 * Basics and pain lands (Karplusan Forest — two *separate* R and G abilities) are unaffected,
 * matching the report that only the multi-mana nonbasic land failed.
 */
class MultiColorLandManaTest : FunSpec({

    fun driverWithAllSets(): GameTestDriver {
        val driver = GameTestDriver()
        MtgSetCatalog.all.forEach { set ->
            driver.registerCards(set.cards)
            driver.registerCards(set.basicLands)
        }
        // A trivial legal deck for both seats; battlefield is set up manually below.
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingLife = 40)
        return driver
    }

    // {1}{R}{G} — Grumgully, the Generous. Needs one red, one green, one generic.
    val grumgullyCost = ManaCost.parse("{1}{R}{G}")

    test("control: {1}{R}{G} is affordable off basic lands (harness sanity)") {
        val driver = driverWithAllSets()
        val player = driver.player1
        // 3x Mountain + 1x Forest = R,R,R,G — clearly pays {1}{R}{G}.
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
        driver.putLandOnBattlefield(player, "Forest")

        val solution = ManaSolver(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null)).solve(driver.state, player, grumgullyCost)
        withClue("Basic lands must pay {1}{R}{G} — if this fails the harness itself is broken") {
            solution.shouldNotBeNull()
        }
    }

    test("Gruul Turf's green half is seen: {1}{R}{G} affordable when green comes only from Gruul Turf") {
        val driver = driverWithAllSets()
        val player = driver.player1
        // Green is available ONLY from Gruul Turf ({T}: Add {R}{G}). The three Mountains supply
        // red + generic. If the solver drops Gruul Turf's green half, it sees R,R,R,R — no green —
        // and reports {1}{R}{G} unaffordable. That is the reported bug.
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
        driver.putLandOnBattlefield(player, "Gruul Turf")

        val solution = ManaSolver(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null)).solve(driver.state, player, grumgullyCost)
        withClue("Gruul Turf must contribute {G}; the solver dropped the second mana of its {R}{G} tap") {
            solution.shouldNotBeNull()
        }
    }

    test("Gruul Turf's red half is seen: {1}{R}{G} affordable when red comes only from Gruul Turf") {
        val driver = driverWithAllSets()
        val player = driver.player1
        // Symmetric: red available ONLY from Gruul Turf; Forests supply green + generic.
        repeat(3) { driver.putLandOnBattlefield(player, "Forest") }
        driver.putLandOnBattlefield(player, "Gruul Turf")

        val solution = ManaSolver(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null)).solve(driver.state, player, grumgullyCost)
        withClue("Gruul Turf must contribute {R} as well as {G}") {
            solution.shouldNotBeNull()
        }
    }
})
