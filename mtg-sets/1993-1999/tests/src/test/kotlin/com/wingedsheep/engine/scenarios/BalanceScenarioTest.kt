package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Balance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Balance (LEA #3) — each player keeps as many lands as the player with the fewest lands controls
 * and sacrifices the rest; then the same for cards in hand (discard) and creatures (sacrifice).
 *
 * Pinned: the per-pass counting of the third 2016-06-08 ruling (a land creature sacrificed in the
 * land pass no longer counts for the creature pass), that a player's keep choice is honoured,
 * that the player with the fewest is untouched, that zero means everything goes, and that each
 * player's discards land in their own graveyard as their own discard.
 */
class BalanceScenarioTest : FunSpec({

    // A land creature, so one permanent is counted by both the land pass and the creature pass.
    val landCreature = card("Test Arbor") {
        typeLine = "Land Creature — Forest Dryad"
        power = 1
        toughness = 1
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Balance + landCreature)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.lands(player: EntityId) = getLands(player)
    fun GameTestDriver.creatures(player: EntityId) = getCreatures(player)

    /**
     * Cast Balance and answer each keep prompt with [keep] for that decision's player when given,
     * else the first options offered.
     */
    fun GameTestDriver.castBalance(caster: EntityId, keep: (SelectCardsDecision) -> List<EntityId>? = { null }) {
        val spell = putCardInHand(caster, "Balance")
        giveMana(caster, Color.WHITE, 1)
        giveColorlessMana(caster, 1)
        castSpell(caster, spell).outcome shouldBe Outcome.Done
        bothPass()
        var guard = 0
        while (isPaused && guard++ < 20) {
            val decision = pendingDecision
            if (decision is SelectCardsDecision) {
                submitCardSelection(decision.playerId, keep(decision) ?: decision.options.take(decision.minSelections))
            } else {
                autoResolveDecision()
            }
        }
    }

    test("every player is cut down to the fewest lands, cards in hand and creatures") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val myLands = List(5) { driver.putLandOnBattlefield(me, "Mountain") }
        repeat(2) { driver.putLandOnBattlefield(opponent, "Mountain") }
        repeat(3) { driver.putCreatureOnBattlefield(me, "Grizzly Bears") }
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        // Opening hands are 7 each; trim the opponent's to 2 so the hand pass bites on me.
        driver.getHand(opponent).drop(2).forEach { driver.moveToGraveyard(it) }
        val keptLands = listOf(myLands[1], myLands[3])

        driver.castBalance(me) { decision ->
            if (decision.playerId == me && decision.options.containsAll(keptLands)) keptLands else null
        }

        withClue("the keep choice is honoured: exactly the two chosen lands survive") {
            driver.lands(me) shouldContainExactlyInAnyOrder keptLands
        }
        driver.lands(opponent).size shouldBe 2
        driver.getHandSize(me) shouldBe 2
        driver.getHandSize(opponent) shouldBe 2
        driver.creatures(me).size shouldBe 1
        withClue("the player with the fewest keeps everything") {
            driver.creatures(opponent) shouldBe listOf(theirBear)
        }
    }

    test("an opponent with no lands makes every player sacrifice every land") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        repeat(4) { driver.putLandOnBattlefield(me, "Mountain") }

        driver.castBalance(me)

        driver.lands(me).size shouldBe 0
        driver.getGraveyardCardNames(me).count { it == "Mountain" } shouldBe 4
        driver.lands(opponent).size shouldBe 0
    }

    test("creatures are counted after the land pass — a sacrificed land creature no longer counts") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val arbor = driver.putLandOnBattlefield(me, "Test Arbor")
        val myMountain = driver.putLandOnBattlefield(me, "Mountain")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putLandOnBattlefield(opponent, "Mountain")
        repeat(2) { driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") }

        // Keep the Mountain, so the land creature goes in the land pass.
        driver.castBalance(me) { decision ->
            if (decision.playerId == me && arbor in decision.options) listOf(myMountain) else null
        }

        driver.getGraveyardCardNames(me) shouldContain "Test Arbor"
        withClue("I control one creature when the creature pass counts, so the opponent keeps one") {
            driver.creatures(opponent).size shouldBe 1
        }
        driver.creatures(me).size shouldBe 1
    }

    test("each player's unkept cards are discarded from their own hand as their own discard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.getHand(me).drop(4).forEach { driver.moveToGraveyard(it) }
        driver.getHand(opponent).drop(1).forEach { driver.moveToGraveyard(it) }
        val myGraveyardBefore = driver.getGraveyard(me).size
        val theirHand = driver.getHand(opponent)

        driver.castBalance(me)

        driver.getHandSize(me) shouldBe 1
        driver.getHand(opponent) shouldBe theirHand
        val discards = driver.events.filterIsInstance<CardsDiscardedEvent>()
        withClue("only the player with more cards discards, and the event names them") {
            discards.map { it.playerId }.toSet() shouldBe setOf(me)
            discards.sumOf { it.cardIds.size } shouldBe 3
        }
        // Three discarded Mountains plus Balance itself.
        driver.getGraveyard(me).size shouldBe myGraveyardBefore + 4
    }
})
