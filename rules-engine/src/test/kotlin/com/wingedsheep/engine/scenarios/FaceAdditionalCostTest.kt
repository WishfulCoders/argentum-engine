package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Additional costs declared on a spell face (#3196). Casting an Omen (CR 720.3a) or an Adventure
 * (CR 715.3a) evaluates only that face, so the cast owes the face's "as an additional cost to cast
 * this spell" and never the primary's — in legal actions, validation and payment alike.
 *
 * The fixture's two halves carry different costs so each direction is visible: the creature asks
 * for an artifact sacrifice the player can never make, the Omen for a creature sacrifice.
 */
class FaceAdditionalCostTest : FunSpec({

    val bloodPriceDrake = card("Blood Price Drake") {
        manaCost = "{4}{B}"
        typeLine = "Creature — Drake"
        power = 3
        toughness = 3
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact))
        omen("Blood Price") {
            manaCost = "{B}"
            typeLine = "Instant — Omen"
            oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nDraw a card."
            additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
            spell {
                effect = Effects.DrawCards(1)
            }
        }
    }

    fun setUp(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(bloodPriceDrake)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Forest" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun omenOffers(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, card: com.wingedsheep.sdk.model.EntityId) =
        driver.legalActions(player).filter {
            (it.action as? CastSpell)?.let { cs -> cs.cardId == card && cs.faceIndex == 0 } == true
        }

    test("an Omen face whose sacrifice can't be paid is neither offered nor castable") {
        val (driver, player) = setUp()
        val drake = driver.putCardInHand(player, "Blood Price Drake")
        driver.giveMana(player, Color.BLACK, 1)

        omenOffers(driver, player, drake).filter { it.affordable } shouldBe emptyList()
        driver.submit(
            CastSpell(player, drake, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldNotBe Outcome.Done
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain drake
    }

    test("the Omen face offers its own sacrifice and consumes the creature, not the primary's artifact") {
        val (driver, player) = setUp()
        val drake = driver.putCardInHand(player, "Blood Price Drake")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 1)

        val info = omenOffers(driver, player, drake).single().additionalCostInfo!!
        info.costType shouldBe "SacrificePermanent"
        info.validSacrificeTargets shouldContainExactly listOf(bears)

        driver.submit(
            CastSpell(player, drake, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldNotBe Outcome.Done

        driver.submit(
            CastSpell(
                player, drake, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
            )
        ).outcome shouldBe Outcome.Done
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain bears

        driver.bothPass()
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)) shouldContain drake
    }
})
