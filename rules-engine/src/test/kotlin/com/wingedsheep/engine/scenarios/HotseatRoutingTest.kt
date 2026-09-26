package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * Input-routing tests for the single-client hotseat ("play against yourself") mechanic.
 *
 * Hotseat is the non-turn-scoped generalization of the Mindslaver-style hijack
 * ([HijackInputRoutingTest]): a [HotseatControlComponent] on a seat permanently routes
 * that seat's *input authority* to one human connection for the whole game, while resource
 * ownership stays with the seat. The whole feature rides the single
 * [com.wingedsheep.engine.state.GameState.actorFor] seam, so these tests pin:
 *  - actorFor resolves both seats to the controller,
 *  - the controller's client view reveals the controlled seat's hand,
 *  - the spectator path still masks it (no info leak),
 *  - a normal game (no component) is unchanged (regression guard),
 *  - the component survives a serialization round-trip.
 */
class HotseatRoutingTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Grizzly Bears" to 20), skipMulligans = true)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    /** Mark both seats as hotseat-controlled by [controller]. */
    fun GameTestDriver.enableHotseat(controller: EntityId) {
        var s = state
        for (playerId in s.turnOrder) {
            s = s.updateEntity(playerId) { it.with(HotseatControlComponent(controllerId = controller)) }
        }
        replaceState(s)
    }

    test("actorFor routes both seats to the single hotseat controller") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.enableHotseat(p1)

        d.state.actorFor(p1) shouldBe p1
        d.state.actorFor(p2) shouldBe p1
    }

    test("controller's client view reveals the controlled seat's hand; spectator path still masks it") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        // Give p2 a card so the controller has something to see once hotseat is on.
        val shock = d.putCardInHand(p2, "Shock")

        // Before hotseat: p1 cannot see p2's hand (zone hidden) — baseline.
        run {
            val pre = transformer(d).transform(d.state, viewingPlayerId = p1)
            pre.cards.keys shouldNotContain shock
            val p2Hand = pre.zones.first { it.zoneId.ownerId == p2 && it.zoneId.zoneType == Zone.HAND }
            p2Hand.isVisible shouldBe false
        }

        d.enableHotseat(p1)

        // Controller (p1) now sees p2's hand contents because actorFor(p2) == p1.
        val controllerView = transformer(d).transform(d.state, viewingPlayerId = p1)
        controllerView.cards.keys shouldContain shock
        val p2Hand = controllerView.zones.first { it.zoneId.ownerId == p2 && it.zoneId.zoneType == Zone.HAND }
        p2Hand.isVisible shouldBe true
        p2Hand.cardIds shouldContain shock

        // The dedicated hotseat flag drives the UI; hijack indicators are NOT conflated with it.
        controllerView.hotseat shouldBe true
        controllerView.youAreHijacking shouldBe null
        controllerView.youAreHijackedBy shouldBe null

        // Spectator path must still mask the hand even with hotseat active (no leak), and
        // must not report hotseat control.
        val spectatorView = transformer(d).transform(d.state, viewingPlayerId = p1, isSpectator = true)
        spectatorView.cards.keys shouldNotContain shock
        val spectatorZone = spectatorView.zones.first { it.zoneId.ownerId == p2 && it.zoneId.zoneType == Zone.HAND }
        spectatorZone.isVisible shouldBe false
        spectatorView.hotseat shouldBe false
    }

    test("regression: without the component a normal game is unchanged — opponent's seat and hand stay private") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        val shock = d.putCardInHand(p2, "Shock")

        d.state.actorFor(p1) shouldBe p1
        d.state.actorFor(p2) shouldBe p2

        val p1View = transformer(d).transform(d.state, viewingPlayerId = p1)
        p1View.cards.keys shouldNotContain shock
        val p2Hand = p1View.zones.first { it.zoneId.ownerId == p2 && it.zoneId.zoneType == Zone.HAND }
        p2Hand.isVisible shouldBe false
        p1View.hotseat shouldBe false
    }

    test("HotseatControlComponent survives a serialization round-trip") {
        val json = Json {
            serializersModule = engineSerializersModule
            encodeDefaults = true
        }
        val original = ComponentContainer().with(HotseatControlComponent(controllerId = EntityId("player-1")))
        val decoded = json.decodeFromString(
            ComponentContainer.serializer(),
            json.encodeToString(ComponentContainer.serializer(), original)
        )
        decoded shouldBe original
        decoded.get<HotseatControlComponent>() shouldBe HotseatControlComponent(controllerId = EntityId("player-1"))
    }
})
