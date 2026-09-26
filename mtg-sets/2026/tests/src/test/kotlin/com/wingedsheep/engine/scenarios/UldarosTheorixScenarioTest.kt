package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Uldaros Theorix — {3}{U}{B}{B} Legendary Creature — Elder Sphinx 5/5 (FRA #159).
 *
 *   Flying
 *   When Uldaros Theorix enters, if you cast him, exile up to one target nonland card of each
 *   card type from your graveyard. Copy those cards. You may cast any number of spells with total
 *   mana value 6 or less from among the copies without paying their mana costs. (Permanent spells
 *   cast this way become tokens.)
 */
class UldarosTheorixScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Uldaros from hand and resolve it, stopping at its ETB trigger's target decision. */
    fun GameTestDriver.castUldaros(player: EntityId): ChooseTargetsDecision {
        val uldaros = putCardInHand(player, "Uldaros Theorix")
        giveMana(player, Color.BLUE, 1)
        giveMana(player, Color.BLACK, 2)
        giveColorlessMana(player, 3)
        castSpell(player, uldaros).error shouldBe null
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    fun GameTestDriver.names(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "?" }

    test("exiles one card of each type, then casts copies within a total mana value of 6") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val giant = driver.putCardInGraveyard(player, "Hill Giant")        // creature, MV 4
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")     // instant, MV 1
        val divination = driver.putCardInGraveyard(player, "Divination")   // sorcery, MV 3
        val forest = driver.putCardInGraveyard(player, "Forest")           // land — not a legal target

        val targetDecision = driver.castUldaros(player)
        val legal = targetDecision.legalTargets[0].orEmpty()
        legal shouldContainExactlyInAnyOrder listOf(giant, bolt, divination)
        (forest in legal) shouldBe false

        driver.submitTargetSelection(player, listOf(giant, bolt, divination)).error shouldBe null
        driver.bothPass() // resolve the trigger

        // Every copy fits the initial budget of 6.
        val first = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.names(first.options) shouldContainExactlyInAnyOrder listOf("Hill Giant", "Lightning Bolt", "Divination")
        val giantCopy = first.options.first { driver.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }
        driver.submitCardSelection(player, listOf(giantCopy)).error shouldBe null

        // 2 left: Divination (3) no longer fits; Lightning Bolt (1) does.
        val second = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.names(second.options) shouldBe listOf("Lightning Bolt")
        driver.submitCardSelection(player, second.options).error shouldBe null
        // The Bolt copy needs a target.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player, listOf(opponent)).error shouldBe null

        // Budget 1 left and nothing fits, so the loop ends without another offer.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) {
            (driver.pendingDecision is SelectCardsDecision) shouldBe false
            driver.bothPass()
        }

        driver.getLifeTotal(opponent) shouldBe 17
        val giantToken = driver.findPermanent(player, "Hill Giant").shouldNotBeNull()
        driver.state.getEntity(giantToken)?.has<TokenComponent>() shouldBe true
        // The originals stay exiled; the uncast Divination copy ceased to exist (CR 707.10a).
        driver.getExileCardNames(player) shouldContainExactlyInAnyOrder
            listOf("Hill Giant", "Lightning Bolt", "Divination")
        driver.getGraveyardCardNames(player) shouldBe listOf("Forest")
    }

    test("targets must be one per card type — an artifact creature fills only one slot") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")     // creature, MV 2
        val giant = driver.putCardInGraveyard(player, "Hill Giant")        // creature, MV 4
        val juggernaut = driver.putCardInGraveyard(player, "Juggernaut")   // artifact creature, MV 4

        val decision = driver.castUldaros(player)
        // Only two card types (artifact, creature) are present among the legal targets.
        decision.targetRequirements.single().maxTargets shouldBe 2

        // Two plain creatures can't both be chosen.
        driver.submitDecision(player, TargetsResponse(decision.id, mapOf(0 to listOf(bears, giant))))
            .error shouldNotBe null
        // Juggernaut + a creature pairs up as artifact + creature.
        driver.submitDecision(player, TargetsResponse(decision.id, mapOf(0 to listOf(juggernaut, bears))))
            .error shouldBe null
        driver.bothPass()

        // 4 + 2 = 6 — both copies can be cast.
        val first = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.names(first.options) shouldContainExactlyInAnyOrder listOf("Juggernaut", "Grizzly Bears")
        val juggernautCopy = first.options.first { driver.state.getEntity(it)?.get<CardComponent>()?.name == "Juggernaut" }
        driver.submitCardSelection(player, listOf(juggernautCopy)).error shouldBe null
        val second = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.names(second.options) shouldBe listOf("Grizzly Bears")
        driver.submitCardSelection(player, second.options).error shouldBe null

        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 20) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
        driver.findPermanent(player, "Juggernaut") shouldNotBe null
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
        driver.getGraveyardCardNames(player) shouldBe listOf("Hill Giant")
    }

    test("declining to cast leaves the originals exiled and the copies gone") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")

        driver.castUldaros(player)
        driver.submitTargetSelection(player, listOf(bears)).error shouldBe null
        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, emptyList()).error shouldBe null

        driver.findPermanent(player, "Grizzly Bears") shouldBe null
        driver.getExileCardNames(player) shouldBe listOf("Grizzly Bears")
    }

    test("does nothing when Uldaros enters without being cast") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCardInGraveyard(player, "Grizzly Bears")

        driver.putCreatureOnBattlefield(player, "Uldaros Theorix")
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        driver.getGraveyardCardNames(player) shouldBe listOf("Grizzly Bears")
    }
})
