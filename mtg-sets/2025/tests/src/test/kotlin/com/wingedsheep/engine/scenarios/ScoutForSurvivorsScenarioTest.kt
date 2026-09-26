package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoe.cards.ScoutForSurvivors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scout for Survivors — {2}{W} Sorcery
 * Return up to three target creature cards with total mana value 3 or less from your graveyard to
 * the battlefield. Put a +1/+1 counter on each of them.
 *
 * The creature cards are chosen as the spell is cast (CR 601.2c) and the summed-mana-value cap is a
 * targeting restriction, so an over-cap set is rejected at cast. On resolution only the targets
 * still in the graveyard come back (CR 608.2b).
 */
class ScoutForSurvivorsScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ScoutForSurvivors))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 1)
        return driver to me
    }

    fun gy(card: EntityId, owner: EntityId) = ChosenTarget.Card(card, owner, Zone.GRAVEYARD)

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("targets with total mana value 3 return with a +1/+1 counter each") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Scout for Survivors")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")    // MV 2
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")   // MV 1
        val giant = driver.putCardInGraveyard(me, "Hill Giant")       // MV 4

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(lions, me))).error shouldBe null
        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        driver.findPermanent(me, "Savannah Lions") shouldBe lions
        driver.plusOnes(bears) shouldBe 1
        driver.plusOnes(lions) shouldBe 1
        driver.getGraveyard(me).contains(giant) shouldBe true
    }

    test("a target set over total mana value 3 can't be chosen") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Scout for Survivors")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")
        val giant = driver.putCardInGraveyard(me, "Hill Giant")

        withClue("2 + 1 + 4 exceeds the cap") {
            driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(lions, me), gy(giant, me))).error shouldNotBe null
        }
        withClue("a single card over the cap is also illegal") {
            driver.castSpellWithTargets(me, spell, listOf(gy(giant, me))).error shouldNotBe null
        }
    }

    test("a creature card in an opponent's graveyard is not a legal target") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Scout for Survivors")
        val opponent = driver.getOpponent(me)
        val theirs = driver.putCardInGraveyard(opponent, "Savannah Lions")

        driver.castSpellWithTargets(me, spell, listOf(gy(theirs, opponent))).error shouldNotBe null
    }

    test("a target that leaves the graveyard in response is skipped; the rest still return") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Scout for Survivors")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(lions, me))).error shouldBe null
        driver.replaceState(driver.state.moveToZone(bears, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        driver.getExile(me).contains(bears) shouldBe true
        driver.findPermanent(me, "Savannah Lions") shouldBe lions
        driver.plusOnes(lions) shouldBe 1
    }
})
