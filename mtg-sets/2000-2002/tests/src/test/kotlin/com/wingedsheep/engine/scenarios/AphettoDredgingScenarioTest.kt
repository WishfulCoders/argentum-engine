package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.AphettoDredging
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aphetto Dredging — {3}{B} Sorcery
 * Return up to three target creature cards of the creature type of your choice from your graveyard
 * to your hand.
 *
 * The cards are targets chosen as the spell is cast (CR 601.2c); "of the creature type of your
 * choice" means they must all share a creature type (a changeling card shares every type,
 * CR 702.73a). Targets gone by resolution are skipped (CR 608.2b).
 */
class AphettoDredgingScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AphettoDredging))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.giveColorlessMana(me, 3)
        driver.giveMana(me, Color.BLACK, 1)
        return driver to me
    }

    fun GameTestDriver.dredge(me: EntityId, targets: List<Pair<EntityId, EntityId>>): ExecutionResult {
        val spell = putCardInHand(me, "Aphetto Dredging")
        return castSpellWithTargets(me, spell, targets.map { (card, owner) -> ChosenTarget.Card(card, owner, Zone.GRAVEYARD) })
    }

    test("returns up to three targeted creature cards that share a creature type") {
        val (driver, me) = setup()
        val bears1 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val bears2 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val shapesharer = driver.putCardInGraveyard(me, "Shapesharer") // changeling — also a Bear
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.dredge(me, listOf(bears1 to me, bears2 to me, shapesharer to me)).error shouldBe null
        driver.bothPass()

        driver.getHand(me).containsAll(listOf(bears1, bears2, shapesharer)) shouldBe true
        driver.getGraveyard(me).contains(lions) shouldBe true
    }

    test("targets that share no creature type can't be chosen together") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        withClue("a Bear and a Cat have no type in common") {
            driver.dredge(me, listOf(bears to me, lions to me)).error shouldNotBe null
        }
    }

    test("an opponent's graveyard is off limits") {
        val (driver, me) = setup()
        val theirs = driver.putCardInGraveyard(driver.getOpponent(me), "Grizzly Bears")

        driver.dredge(me, listOf(theirs to driver.getOpponent(me))).error shouldNotBe null
    }

    test("a target that leaves the graveyard in response is skipped; the rest still return") {
        val (driver, me) = setup()
        val bears1 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val bears2 = driver.putCardInGraveyard(me, "Grizzly Bears")

        driver.dredge(me, listOf(bears1 to me, bears2 to me)).error shouldBe null
        driver.replaceState(driver.state.moveToZone(bears1, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        driver.getExile(me).contains(bears1) shouldBe true
        driver.getHand(me).contains(bears2) shouldBe true
    }
})
