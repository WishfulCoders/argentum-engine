package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.PrismaticEnding
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Prismatic Ending (MH2) — {X}{W} Sorcery.
 *
 * "Converge — Exile target nonland permanent if its mana value is less than or equal to the number
 *  of colors of mana spent to cast this spell."
 *
 * Pins: the comparison is colors spent (not total mana), and mana value is checked on resolution,
 * not as a targeting restriction — a too-expensive permanent is a legal target that stays put.
 */
class PrismaticEndingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PrismaticEnding)
        return driver
    }

    test("X=1 paid with W + G exiles a mana value 2 permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature") // {1}{B}, MV 2

        val spell = driver.putCardInHand(player, "Prismatic Ending")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.GREEN, 1)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                xValue = 1,
            ),
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Black Creature") shouldBe null
        driver.getExile(opponent).size shouldBe 1
    }

    test("X=1 paid with two white mana is one color — a mana value 2 target stays") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature") // MV 2

        val spell = driver.putCardInHand(player, "Prismatic Ending")
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                xValue = 1,
            ),
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Black Creature") shouldNotBe null
        driver.getExile(opponent).size shouldBe 0
    }

    test("X=0 paid with W exiles a mana value 1 permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // {R}, MV 1

        val spell = driver.putCardInHand(player, "Prismatic Ending")
        driver.giveMana(player, Color.WHITE, 1)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                xValue = 0,
            ),
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Goblin Guide") shouldBe null
    }
})
