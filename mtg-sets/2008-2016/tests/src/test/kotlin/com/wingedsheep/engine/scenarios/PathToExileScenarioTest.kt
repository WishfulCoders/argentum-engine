package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.conflux.cards.PathToExile
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Path to Exile (CON #15) — {W} Instant.
 *
 *   "Exile target creature. Its controller may search their library for a basic land card, put
 *    that card onto the battlefield tapped, then shuffle."
 *
 * The search belongs to the *exiled creature's controller*, read after the creature has already
 * left the battlefield, and the fetched land enters tapped. Declining fetches nothing.
 */
class PathToExileScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + PathToExile)
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun plainsControlledBy(d: GameTestDriver, playerId: EntityId): List<EntityId> =
        d.state.getBattlefield().filter { id ->
            d.state.getEntity(id)?.get<CardComponent>()?.name == "Plains" &&
                d.state.projectedState.getController(id) == playerId
        }

    /** Resolves the stack, answering every decision with [accept]; returns who was asked. */
    fun resolve(d: GameTestDriver, accept: Boolean): Set<EntityId> {
        val asked = mutableSetOf<EntityId>()
        var safety = 0
        while (safety++ < 40) {
            when (val pending = d.state.pendingDecision) {
                is YesNoDecision -> {
                    asked += pending.playerId
                    d.submitYesNo(pending.playerId, accept)
                }
                is SelectCardsDecision -> {
                    asked += pending.playerId
                    d.submitCardSelection(pending.playerId, if (accept) pending.options.take(1) else emptyList())
                }
                else -> if (d.state.stack.isNotEmpty()) d.bothPass() else break
            }
        }
        return asked
    }

    test("exiles the creature and its controller fetches a tapped basic land") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        val path = d.putCardInHand(me, "Path to Exile")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpell(me, path, targets = listOf(victim)).error shouldBe null

        val asked = resolve(d, accept = true)

        asked shouldBe setOf(opp)
        d.state.getZone(opp, Zone.EXILE) shouldContain victim
        val fetched = plainsControlledBy(d, opp)
        fetched.size shouldBe 1
        d.isTapped(fetched.single()) shouldBe true
        plainsControlledBy(d, me).size shouldBe 0
    }

    test("the controller may decline the search") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        val path = d.putCardInHand(me, "Path to Exile")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpell(me, path, targets = listOf(victim)).error shouldBe null

        resolve(d, accept = false)

        d.state.getZone(opp, Zone.EXILE) shouldContain victim
        plainsControlledBy(d, opp).size shouldBe 0
    }
})
