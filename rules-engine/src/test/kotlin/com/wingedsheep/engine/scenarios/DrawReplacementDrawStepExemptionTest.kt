package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.DrawEvent(exceptFirstInDrawStep = true)` on a per-card **draw replacement** —
 * "If an opponent would draw a card except the first one they draw in each of their draw steps,
 * instead you create a Treasure token." (Hullbreacher).
 *
 * Rules pinned:
 *  - CR 504.1 — the first card the player draws in their own draw step (the turn-based draw) is
 *    exempt and is drawn normally.
 *  - Every other draw is replaced, whichever instruction it belongs to: CR 121.2 makes each card an
 *    individual draw, so the *first* card of an off-step "draw two" is replaced too.
 *  - A later draw in the same draw step is not exempt.
 *  - "The first one they draw" is the first card actually drawn in the step: when the turn-based
 *    draw doesn't happen (the starting player's skipped first draw, CR 103.8a), the first card drawn
 *    by a spell in that step is the exempt one.
 *  - Only opponents are affected; and the Treasure belongs to the replacement's controller (CR 111.2),
 *    although the replacement runs in the drawing player's context.
 */
class DrawReplacementDrawStepExemptionTest : FunSpec({

    val DrawTax = card("Draw Tax Test") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "If an opponent would draw a card except the first one they draw in each of " +
            "their draw steps, instead you create a Treasure token."
        replacementEffect(
            ReplaceDrawWith(
                replacementEffect = Effects.CreateTreasure(controller = EffectTarget.PlayerRef(Player.ControllerOfSource)),
                appliesTo = EventPattern.DrawEvent(player = Player.EachOpponent, exceptFirstInDrawStep = true),
            )
        )
        metadata { rarity = Rarity.RARE; collectorNumber = "T01" }
    }
    val DrawTwo = card("Draw Two Instant Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T02" }
    }
    val DrawOne = card("Draw One Instant Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T03" }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(DrawTax, DrawTwo, DrawOne))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun GameTestDriver.treasures(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Treasure" }

    fun GameTestDriver.resolveAll() {
        while (getTopOfStack() != null) bothPass()
    }

    test("every off-step draw is replaced, including the first card of a multi-card instruction") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "Draw Tax Test")

        // p2's turn, main phase: their for-turn draw is behind them.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHand(p2).size

        val spell = driver.putCardInHand(p2, "Draw Two Instant Test")
        driver.castSpell(p2, spell)
        driver.resolveAll()

        withClue("both draws were replaced") { driver.getHand(p2).size shouldBe handBefore }
        withClue("two Treasures, created under the replacement's controller") {
            driver.treasures(p1) shouldBe 2
            driver.treasures(p2) shouldBe 0
        }
    }

    test("the turn-based draw is exempt; a second draw in the same draw step is not") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "Draw Tax Test")
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        val handBefore = driver.getHand(p2).size
        driver.passPriorityUntil(Step.DRAW)

        withClue("the for-turn draw happened normally") {
            driver.getHand(p2).size shouldBe handBefore + 1
            driver.treasures(p1) shouldBe 0
        }

        val spell = driver.putCardInHand(p2, "Draw One Instant Test")
        driver.castSpell(p2, spell)
        driver.resolveAll()

        withClue("the second draw of the draw step is replaced") {
            driver.getHand(p2).size shouldBe handBefore + 1
            driver.treasures(p1) shouldBe 1
        }
    }

    test("when the turn-based draw doesn't happen, the first card drawn in the step is the exempt one") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        // p2 controls the tax; p1 is the starting player, whose turn-1 draw is skipped (CR 103.8a).
        driver.putPermanentOnBattlefield(p2, "Draw Tax Test")
        driver.passPriorityUntil(Step.DRAW)
        val handBefore = driver.getHand(p1).size

        val spell = driver.putCardInHand(p1, "Draw Two Instant Test")
        driver.castSpell(p1, spell)
        driver.resolveAll()

        withClue("first card drawn in the draw step is exempt, the second is replaced") {
            driver.getHand(p1).size shouldBe handBefore + 1
            driver.treasures(p2) shouldBe 1
        }
    }

    test("an opponent drawing during the controller's turn is replaced; the controller's draws are not") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "Draw Tax Test")

        val ownHand = driver.getHand(p1).size
        val own = driver.putCardInHand(p1, "Draw One Instant Test")
        driver.castSpell(p1, own)
        driver.resolveAll()
        withClue("the controller draws normally") {
            driver.getHand(p1).size shouldBe ownHand + 1
            driver.treasures(p1) shouldBe 0
        }

        val theirHand = driver.getHand(p2).size
        val theirs = driver.putCardInHand(p2, "Draw One Instant Test")
        driver.passPriority(p1)
        driver.castSpell(p2, theirs)
        driver.resolveAll()
        withClue("the opponent's draw on p1's turn is replaced") {
            driver.getHand(p2).size shouldBe theirHand
            driver.treasures(p1) shouldBe 1
        }
    }
})
