package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Issue #2780: a card's name copied into a decision or a badge must not name a face-down object to
 * a player who may not look under it. [DecisionMasker] renders every decision per viewer through
 * [Visibility.cardNameFor]; player badges name face-down objects by their public label.
 */
class DecisionMaskerTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, Visibility> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        // A face-down creature (really "Centaur Courser") that player 2 controls.
        val faceDown = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.replaceState(driver.state.updateEntity(faceDown) { it.with(FaceDownComponent) })
        val visibility = Visibility(driver.cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry).conditions)
        return Triple(driver, faceDown, visibility)
    }

    fun selection(chooser: EntityId, faceDown: EntityId) = SelectCardsDecision(
        id = "d",
        playerId = chooser,
        prompt = "Choose a pile",
        context = DecisionContext(sourceId = faceDown, sourceName = "Centaur Courser"),
        options = listOf(faceDown),
        minSelections = 0,
        maxSelections = 1,
        cardInfo = mapOf(faceDown to SearchCardInfo(name = "Centaur Courser", manaCost = "{2}{G}", typeLine = "Creature — Centaur Warrior", power = 3)),
    )

    test("a selection offering an opponent's face-down creature hides its identity from the chooser") {
        val (driver, faceDown, visibility) = setup()
        val masked = DecisionMasker(visibility).maskFor(selection(driver.player1, faceDown), driver.state, driver.player1)
            as SelectCardsDecision

        val info = masked.cardInfo!!.getValue(faceDown)
        info.name shouldBe FACE_DOWN_DISPLAY_NAME
        info.manaCost shouldBe ""
        info.typeLine shouldBe "Creature"
        info.power shouldBe 2
        masked.context.sourceName shouldBe FACE_DOWN_DISPLAY_NAME
    }

    test("the face-down creature's controller still sees its name") {
        val (driver, faceDown, visibility) = setup()
        val decision = selection(driver.player2, faceDown)
        DecisionMasker(visibility).maskFor(decision, driver.state, driver.player2) shouldBe decision
    }

    test("a player the creature was revealed to sees its name") {
        val (driver, faceDown, visibility) = setup()
        driver.replaceState(driver.state.updateEntity(faceDown) { it.with(RevealedToComponent(setOf(driver.player1))) })
        val masked = DecisionMasker(visibility).maskFor(selection(driver.player1, faceDown), driver.state, driver.player1)
            as SelectCardsDecision
        masked.cardInfo!!.getValue(faceDown).name shouldBe "Centaur Courser"
    }

    test("a spectator sees the face-down label even when rendered from the controller's seat") {
        val (driver, faceDown, visibility) = setup()
        val masked = DecisionMasker(visibility)
            .maskFor(selection(driver.player2, faceDown), driver.state, driver.player2, isSpectator = true)
            as SelectCardsDecision
        masked.context.sourceName shouldBe FACE_DOWN_DISPLAY_NAME
        masked.cardInfo!!.getValue(faceDown).name shouldBe FACE_DOWN_DISPLAY_NAME
    }

    test("a decision with no card info still masks a face-down source name") {
        val (driver, faceDown, visibility) = setup()
        val decision = YesNoDecision(
            id = "d",
            playerId = driver.player1,
            prompt = "Pay?",
            context = DecisionContext(sourceId = faceDown, sourceName = "Centaur Courser"),
        )
        DecisionMasker(visibility).maskFor(decision, driver.state, driver.player1)
            .context.sourceName shouldBe FACE_DOWN_DISPLAY_NAME
    }

    test("the Ring badge names a face-down Ring-bearer only to its controller") {
        val (driver, faceDown, _) = setup()
        driver.replaceState(
            driver.state
                .updateEntity(faceDown) { it.with(RingBearerComponent(driver.player2)) }
                .updateEntity(driver.player2) { it.with(TheRingComponent(temptCount = 1)) }
        )
        val transformer = ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry))
        fun ringBadge(viewer: EntityId) = transformer.transform(driver.state, viewer)
            .players.single { it.playerId == driver.player2 }.activeEffects.single { it.effectId == "the_ring" }

        ringBadge(driver.player1).description shouldNotContain "Centaur Courser"
        ringBadge(driver.player1).description shouldContain FACE_DOWN_DISPLAY_NAME
        ringBadge(driver.player2).description shouldContain "Centaur Courser"
    }
})
