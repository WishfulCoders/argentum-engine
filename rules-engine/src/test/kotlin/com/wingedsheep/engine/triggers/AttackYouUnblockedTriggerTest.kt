package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.CreaturesAttackYouUnblockedEvent` — "whenever one or more creatures an opponent
 * controls attack you and aren't blocked" (Coveted Jewel), spelled `Triggers.you.isAttackedUnblocked()`.
 *
 * Pinned against CR 509.1h / 509.3g / 603.2c and the Coveted Jewel rulings (2018-07-13):
 * - it triggers after blockers are declared when any creature attacking you is unblocked, however
 *   many others were blocked, and only once for the batch;
 * - it doesn't trigger when every attacker is blocked, or for creatures attacking a planeswalker
 *   you control;
 * - the attacking player is "that player" (`Player.TriggeringPlayer`);
 * - when several opponents attack you at once (Two-Headed Giant) it triggers for each of them.
 */
class AttackYouUnblockedTriggerTest : FunSpec({

    // "…that player draws a card" — a per-player, observable payoff.
    val observer = card("Unblocked Attack Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.isAttackedUnblocked()
            effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + observer)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attack(attackers: Map<EntityId, EntityId>) {
        val active = activePlayer!!
        attackers.keys.forEach(::removeSummoningSickness)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, attackers)
        bothPass()
    }

    test("one unblocked attacker among blocked ones triggers once, and the attacker draws") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val me = driver.getOpponent(attacker)
        driver.putPermanentOnBattlefield(me, "Unblocked Attack Observer")
        val a1 = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val a2 = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val a3 = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val blocker = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.attack(listOf(a1, a2, a3).associateWith { me })
        driver.declareBlockers(me, mapOf(blocker to listOf(a1)))

        withClue("two unblocked attackers are one batch from one player") {
            driver.stackSize shouldBe 1
        }
        val attackerHand = driver.getHandSize(attacker)
        val myHand = driver.getHandSize(me)
        driver.bothPass()
        driver.getHandSize(attacker) shouldBe attackerHand + 1
        driver.getHandSize(me) shouldBe myHand
    }

    test("no trigger when every attacker is blocked") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val me = driver.getOpponent(attacker)
        driver.putPermanentOnBattlefield(me, "Unblocked Attack Observer")
        val a1 = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val blocker = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.attack(mapOf(a1 to me))
        driver.declareBlockers(me, mapOf(blocker to listOf(a1)))

        driver.stackSize shouldBe 0
    }

    test("an unblocked creature attacking a planeswalker I control doesn't trigger it") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val me = driver.getOpponent(attacker)
        driver.putPermanentOnBattlefield(me, "Unblocked Attack Observer")
        val walker = driver.putPermanentOnBattlefield(me, "Ajani Goldmane")
        val a1 = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")

        driver.attack(mapOf(a1 to walker))
        driver.declareNoBlockers(me)

        driver.stackSize shouldBe 0
    }

    test("the controller's own attack never triggers it — only creatures attacking it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Unblocked Attack Observer")
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.attack(mapOf(mine to opponent))
        driver.declareNoBlockers(opponent)

        driver.stackSize shouldBe 0
    }

    test("Two-Headed Giant: both opponents' unblocked creatures attacking me trigger it once each") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + observer)
        val p = driver.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p[2], "Unblocked Attack Observer")
        val a0 = driver.putCreatureOnBattlefield(p[0], "Grizzly Bears")
        val a1 = driver.putCreatureOnBattlefield(p[1], "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(p[1], "Grizzly Bears")

        // The attacking team's one combined attack (CR 805.10b): p0's and p1's creatures attack me
        // (p2); p1's second creature attacks my teammate, which is not "attack you".
        var state = driver.state
        for ((attacker, defender) in listOf(a0 to p[2], a1 to p[2], other to p[3])) {
            state = state.updateEntity(attacker) { it.with(AttackingComponent(defenderId = defender)) }
        }
        state = state.updateEntity(p[0]) { it.with(AttackersDeclaredThisCombatComponent) }
            .updateEntity(p[1]) { it.with(AttackersDeclaredThisCombatComponent) }
            .copy(step = Step.DECLARE_BLOCKERS, phase = Phase.COMBAT)
            .withPriority(p[2])
        driver.replaceState(state)

        driver.submitSuccess(DeclareBlockers(p[2], emptyMap()))
        var guard = 0
        while (driver.isPaused && guard++ < 5) driver.autoResolveDecision()

        withClue("one trigger per attacking opponent, none for the teammate's attacker") {
            driver.stackSize shouldBe 2
        }
        withClue("each trigger names its own attacking player as \"that player\"") {
            driver.state.stack.mapNotNull { id ->
                driver.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()?.triggerContext?.triggeringPlayerId
            }.toSet() shouldBe setOf(p[0], p[1])
        }
    }
})
