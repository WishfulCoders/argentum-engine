package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.state.components.player.SpellsCastThisGameComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json

/**
 * `DynamicAmount.SpellsCastThisGame` — the game-long cast count behind "the first spell you've cast
 * this game" (Once Upon a Time):
 *
 * - CR 601.2i: a spell counts once its cast completes; the count is per player and never resets at
 *   a turn boundary.
 * - Read while a spell is proposed, it excludes that spell, so a `{0}` self-alternative cost gated
 *   on `count == 0` is available for the first spell of the game and never again — not on a later
 *   turn, not after an opponent's spells (those are theirs).
 */
class SpellsCastThisGameTest : FunSpec({

    val firstFree = card("Test First Free Spell") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Instant"
        oracleText = "If this spell is the first spell you've cast this game, you may cast it without paying its mana cost.\nYou gain 1 life."
        selfAlternativeCost = SelfAlternativeCost(
            manaCost = ManaCost.parse("{0}"),
            condition = Conditions.CompareAmounts(DynamicAmounts.spellsCastThisGame(), ComparisonOperator.EQ, 0),
        )
        spell { effect = Effects.GainLife(1) }
    }

    val cheap = card("Test Cheap Spell") {
        manaCost = "{G}"
        colorIdentity = "G"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(firstFree, cheap))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun count(driver: GameTestDriver, player: EntityId) =
        driver.state.getEntity(player)?.get<SpellsCastThisGameComponent>()?.count ?: 0

    fun castFree(driver: GameTestDriver, player: EntityId, cardId: EntityId) = driver.submit(
        CastSpell(player, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)
    )

    test("CR 601.2i: the count rises per cast, per player, and survives the turn boundary") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        count(driver, me) shouldBe 0

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Test Cheap Spell")).error shouldBe null
        count(driver, me) shouldBe 1 // counted on cast, before it resolves
        resolveStack(driver)
        count(driver, opponent) shouldBe 0

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        count(driver, me) shouldBe 1
    }

    test("the first spell of the game may be cast for its {0} alternative cost; it doesn't count itself") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Test First Free Spell")
        val life = driver.getLifeTotal(me)
        castFree(driver, me, spell).error shouldBe null
        count(driver, me) shouldBe 1
        resolveStack(driver)
        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("after any earlier spell — even on an earlier turn — the free cast is gone, and the opponent's first is theirs") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Test Cheap Spell")).error shouldBe null
        resolveStack(driver)

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300) // opponent's turn
        driver.activePlayer shouldBe opponent
        val theirs = driver.putCardInHand(opponent, "Test First Free Spell")
        castFree(driver, opponent, theirs).error shouldBe null
        resolveStack(driver)

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300) // my next turn
        val mine = driver.putCardInHand(me, "Test First Free Spell")
        castFree(driver, me, mine).error shouldNotBe null
    }

    test("SpellsCastThisGame round-trips through serialization") {
        val amount: DynamicAmount = DynamicAmount.SpellsCastThisGame()
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
    }
})
