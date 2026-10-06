package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `Costs.pay.PayDynamicMana(amount)` — "[suffer] unless that player pays {X}, where X is …"
 * ([com.wingedsheep.sdk.scripting.costs.PayCost.DynamicMana]).
 *
 * - X is evaluated as the ability resolves (CR 608.2h), not when it triggered.
 * - If the source has left the battlefield, its last-known information gives X (CR 608.2h).
 * - A negative X is {0} (CR 107.1b) — the payer is still asked and may decline.
 * - A payer who can't afford {X} just suffers.
 */
class PayDynamicManaTest : ScenarioTestBase() {

    private fun TestGame.handSize1() = state.getHand(player1Id).size
    private fun TestGame.untappedLands(playerId: EntityId) =
        state.getBattlefield().count { id ->
            projectedLand(id) && state.projectedState.getController(id) == playerId &&
                state.getEntity(id)?.has<TappedComponent>() != true
        }
    private fun TestGame.projectedLand(id: EntityId) = state.projectedState.hasType(id, "LAND")

    init {
        cardRegistry.register(card("Dynamic Tax Watcher") {
            manaCost = "{W}"
            typeLine = "Creature — Human"
            power = 1
            toughness = 1
            triggeredAbility {
                trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.PayDynamicMana(DynamicAmounts.sourcePower()),
                    suffer = Effects.DrawCards(1),
                    player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                )
            }
        })
        cardRegistry.register(card("Dynamic Tax Spell") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.GainLife(1) }
        })
        cardRegistry.register(card("Dynamic Tax Pump") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.ModifyStats(2, 0, target(TargetFilter.Creature)) }
        })
        cardRegistry.register(card("Dynamic Tax Shrink") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.ModifyStats(-3, 0, target(TargetFilter.Creature)) }
        })
        cardRegistry.register(card("Dynamic Tax Removal") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.Destroy(target(TargetFilter.Creature)) }
        })

        fun board(opponentLands: Int) = scenario().withPlayers()
            .withCardOnBattlefield(1, "Dynamic Tax Watcher")
            .withCardInHand(1, "Dynamic Tax Pump")
            .withCardInHand(1, "Dynamic Tax Shrink")
            .withCardInHand(1, "Dynamic Tax Removal")
            .withCardInHand(2, "Dynamic Tax Spell")
            .withLandsOnBattlefield(2, "Plains", opponentLands)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.opponentCasts() {
            castSpell(2, "Dynamic Tax Spell").error shouldBe null
        }

        test("the payer is asked for {X} = the source's power and paying it stops the draw") {
            val game = board(3).build()
            val before = game.handSize1()
            game.opponentCasts()
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.playerId shouldBe game.player2Id
            decision.prompt shouldContain "{1}"
            game.answerYesNo(true).error shouldBe null
            // Paying opens the mana-source window (CR 605.3a); auto-tap a Plains.
            game.submitManaSourcesAutoPay().error shouldBe null
            game.state.pendingDecision shouldBe null
            game.resolveStack()
            game.handSize1() shouldBe before
            game.untappedLands(game.player2Id) shouldBe 2
        }

        test("declining lets the controller draw") {
            val game = board(3).build()
            val before = game.handSize1()
            game.opponentCasts()
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.handSize1() shouldBe before + 1
        }

        test("X is read on resolution: a pump in response raises it") {
            val game = board(5).build()
            game.opponentCasts()
            // Trigger is on the stack; its controller responds by pumping the watcher.
            game.passPriority() // P2 passes priority with the trigger on top
            game.castSpell(1, "Dynamic Tax Pump", game.findPermanent("Dynamic Tax Watcher")!!).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.prompt shouldContain "{3}"
        }

        test("a source that left uses its last-known power") {
            val game = board(5).build()
            game.opponentCasts()
            game.passPriority()
            val watcher = game.findPermanent("Dynamic Tax Watcher")!!
            game.state = game.state.updateEntity(watcher) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
            }
            game.castSpell(1, "Dynamic Tax Removal", watcher).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.prompt shouldContain "{4}"
        }

        test("a negative X is {0}, which may still be declined") {
            val game = board(0).build()
            val before = game.handSize1()
            game.opponentCasts()
            game.passPriority()
            game.castSpell(1, "Dynamic Tax Shrink", game.findPermanent("Dynamic Tax Watcher")!!).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.prompt shouldContain "{0}"
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.handSize1() shouldBe before + 1 - 1 // drew one, cast the shrink
        }

        test("a payer who can't afford {X} just suffers") {
            val game = board(0).build()
            val before = game.handSize1()
            game.opponentCasts()
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.handSize1() shouldBe before + 1
        }
    }
}
