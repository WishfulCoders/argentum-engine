package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * "The game is a draw" (CR 104.4c — `Effects.GameIsDraw()`, Divine Intervention): the game ends at
 * once with no winner, and nobody has lost — so neither a "can't lose the game" nor a "your
 * opponents can't win the game" grant has anything to stop.
 */
class GameIsDrawTest : FunSpec({

    val callItEven = card("Call It Even") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GameIsDraw() }
    }

    // A draw first, then a life loss that would otherwise end the game with a winner.
    val evenThenBleed = card("Even Then Bleed") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GameIsDraw().then(Effects.LoseLife(30, EffectTarget.Controller)) }
    }

    val lockward = card("Draw Lockward Aura") {
        manaCost = "{0}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = GrantStaticAbility(GrantCantLoseGame, filter = Filters.EnchantedCreature) }
        staticAbility { ability = GrantStaticAbility(GrantOpponentsCantWinGame, filter = Filters.EnchantedCreature) }
    }

    val host = card("Draw Lockward Host") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(callItEven, evenThenBleed, lockward, host))
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()): List<com.wingedsheep.engine.core.GameEvent> {
        val holder = state.priorityPlayerId
        if (holder != null && holder != player) passPriority(holder)
        val events = castSpell(player, putCardInHand(player, name), targets).also { it.outcome shouldBe Outcome.Done }.events.toMutableList()
        var guard = 0
        while (!state.gameOver && state.stack.isNotEmpty() && guard++ < 10) events += bothPass().events
        return events
    }

    fun GameTestDriver.nobodyLost() =
        listOf(player1, player2).none { state.getEntity(it)?.get<PlayerLostComponent>() != null }

    test("ends the game at once with no winner and no loser") {
        val d = driver()
        val events = d.cast(d.player1, "Call It Even")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe null
        d.nobodyLost().shouldBeTrue()
        events shouldContain GameEndedEvent(winnerId = null, reason = GameEndReason.DRAW)
    }

    test("can't-lose and opponents-can't-win grants don't stop a draw") {
        val d = driver()
        val creature = d.putCreatureOnBattlefield(d.player1, "Draw Lockward Host")
        d.cast(d.player1, "Draw Lockward Aura", listOf(creature))
        d.state.gameOver.shouldBeFalse()

        d.cast(d.player2, "Call It Even")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe null
    }

    test("the draw stands: a loss later in the same resolution doesn't turn it into a win") {
        val d = driver()
        d.cast(d.player1, "Even Then Bleed")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe null
    }

    test("no action is accepted once the game is a draw") {
        val d = driver()
        d.cast(d.player1, "Call It Even")

        d.submitExpectFailure(
            com.wingedsheep.engine.core.PassPriority(d.player1)
        ).error.shouldNotBeNull()
    }
})
