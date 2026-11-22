package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.view.DecisionEnricher
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The "<player> is choosing targets" banner shows the art of the spell behind the decision. A spell
 * still being cast from its controller's hand isn't in the opponent's masked state, so the status
 * carries the art itself — but never for a source whose identity the viewer may not see.
 */
class OpponentDecisionSourceArtTest : FunSpec({

    val art = "https://cards.example/collective-defiance.jpg"

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val caster = driver.activePlayer!!
        val viewer = if (caster == driver.player1) driver.player2 else driver.player1
        return Triple(driver, caster, viewer)
    }

    fun GameTestDriver.stampArt(entityId: EntityId) = replaceState(state.updateEntity(entityId) { container ->
        container.with(container.get<CardComponent>()!!.copy(imageUri = art))
    })

    fun targetsDecision(caster: EntityId, sourceId: EntityId) = ChooseTargetsDecision(
        id = "d1",
        playerId = caster,
        prompt = "Choose targets",
        context = DecisionContext(sourceId = sourceId, sourceName = "Savannah Lions", phase = DecisionPhase.CASTING),
        targetRequirements = emptyList(),
        legalTargets = emptyMap(),
    )

    test("a spell being cast from hand sends its art to the opponent") {
        val (driver, caster, viewer) = setUp()
        val spell = driver.putCardInHand(caster, "Savannah Lions")
        driver.stampArt(spell)

        val status = DecisionEnricher(driver.cardRegistry)
            .createOpponentDecisionStatus(targetsDecision(caster, spell), driver.state, viewer)

        status.sourceName shouldBe "Savannah Lions"
        status.sourceImageUri shouldBe art
    }

    test("a face-down source sends no art") {
        val (driver, caster, viewer) = setUp()
        val morph = driver.putCreatureOnBattlefield(caster, "Savannah Lions")
        driver.stampArt(morph)
        driver.replaceState(driver.state.updateEntity(morph) { it.with(FaceDownComponent) })

        val status = DecisionEnricher(driver.cardRegistry)
            .createOpponentDecisionStatus(targetsDecision(caster, morph), driver.state, viewer)

        status.sourceImageUri shouldBe null
    }
})
