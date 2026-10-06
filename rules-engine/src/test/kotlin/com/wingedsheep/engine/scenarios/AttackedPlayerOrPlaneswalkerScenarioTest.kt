package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [EffectTarget.AttackedPlayerOrPlaneswalker] — "the player or planeswalker it's attacking".
 *
 * Pins the rules it encodes:
 * - it names the attacked *object* (CR 506.3 / 508.1b): a creature attacking a planeswalker hits
 *   the planeswalker, not its controller (contrast `Player.DefendingPlayer`, CR 802.2a);
 * - it is per attacking creature (Hellrider's ruling: "For each attacking creature, Hellrider will
 *   deal damage to the corresponding player or planeswalker");
 * - a planeswalker removed from combat (CR 506.4) leaves the attacker attacking nothing
 *   (CR 506.4c), so nothing is dealt — and that is a no-op, not an error;
 * - a battle is neither a player nor a planeswalker;
 * - an attacker that has left the battlefield answers from last-known information (CR 608.2h,
 *   Myr Battlesphere's ruling).
 */
class AttackedPlayerOrPlaneswalkerScenarioTest : ScenarioTestBase() {

    /** Hellrider's shape: the triggering attacker's defender. */
    private val pinger = card("Test Attack Pinger") {
        manaCost = "{2}{R}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Devil"
        power = 3
        toughness = 3
        oracleText = "Haste\nWhenever a creature you control attacks, this creature deals 1 damage to the player or planeswalker it's attacking."
        keywords(Keyword.HASTE)
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            effect = Effects.DealDamage(1, EffectTarget.AttackedPlayerOrPlaneswalker())
        }
    }

    /** Self shape, sacrificing itself first: the defender must come from its exit snapshot. */
    private val martyr = card("Test Attack Martyr") {
        manaCost = "{3}"
        colorIdentity = ""
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "Whenever this creature attacks, sacrifice it. It deals 2 damage to the player or planeswalker it was attacking."
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = Effects.SacrificeTarget(EffectTarget.Self) then
                Effects.DealDamage(2, EffectTarget.AttackedPlayerOrPlaneswalker(EffectTarget.Self))
        }
    }

    private val siege = card("Test Attack Rampart") {
        manaCost = "{2}{W}"
        colorIdentity = "W"
        typeLine = "Battle — Siege"
        startingDefense = 5
        oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack it.)"
    }

    private fun TestGame.counters(name: String, type: CounterType): Int =
        findPermanent(name)?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(type) } ?: 0

    /** Resolve every attack trigger, ordering simultaneous ones as offered. */
    private fun TestGame.resolveTriggers() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                null -> passPriority().error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board(vararg opponent: String, player: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        player.forEach { builder.withCardOnBattlefield(1, it, summoningSickness = false) }
        opponent.forEach { builder.withCardOnBattlefield(2, it, summoningSickness = false) }
        val game = builder.build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        return game
    }

    init {
        cardRegistry.register(pinger)
        cardRegistry.register(martyr)
        cardRegistry.register(siege)

        context("CR 506.3 — it names the attacked object, one per attacking creature") {

            test("a creature attacking a player: that player takes the damage") {
                val game = board(player = listOf("Test Attack Pinger", "Grizzly Bears"))
                game.declareAttackers(mapOf("Test Attack Pinger" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveTriggers()
                withClue("two attackers, two triggers, one damage each") { game.getLifeTotal(2) shouldBe 18 }
                game.getLifeTotal(1) shouldBe 20
            }

            test("a creature attacking a planeswalker: the planeswalker takes it, not its controller") {
                val game = board("Ajani Goldmane", player = listOf("Test Attack Pinger", "Grizzly Bears"))
                val loyalty = game.counters("Ajani Goldmane", CounterType.LOYALTY)
                game.declareAttackersWithPermanentTargets(
                    playerAttackers = mapOf("Test Attack Pinger" to 2),
                    permanentAttackers = mapOf("Grizzly Bears" to "Ajani Goldmane"),
                ).error shouldBe null
                game.resolveTriggers()
                withClue("the Pinger's own trigger hit the player it attacks") { game.getLifeTotal(2) shouldBe 19 }
                withClue("the Bears' trigger hit the planeswalker they attack (CR 306.8)") {
                    game.counters("Ajani Goldmane", CounterType.LOYALTY) shouldBe loyalty - 1
                }
            }
        }

        context("CR 506.4c — an attacked planeswalker removed from combat leaves nothing to hit") {

            test("the planeswalker changes control with the trigger on the stack: no damage, no error") {
                val game = board("Ajani Goldmane", player = listOf("Test Attack Pinger"))
                val ajani = game.findPermanent("Ajani Goldmane")!!
                val loyalty = game.counters("Ajani Goldmane", CounterType.LOYALTY)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Test Attack Pinger" to "Ajani Goldmane"),
                ).error shouldBe null
                game.state.stack.size shouldBe 1

                game.state = game.state.updateEntity(ajani) { it.with(ControllerComponent(game.player1Id)) }
                game.checkStateBasedActions()
                game.resolveTriggers()

                withClue("the stolen planeswalker isn't the thing it's attacking any more") {
                    game.counters("Ajani Goldmane", CounterType.LOYALTY) shouldBe loyalty
                }
                withClue("and the damage doesn't fall through to a player") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("A battle is neither a player nor a planeswalker") {

            test("attacking a Siege deals nothing to the battle or its protector") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Test Attack Rampart")
                    .withCardOnBattlefield(1, "Test Attack Pinger", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()
                Battles.protectorOf(game.state, game.findPermanent("Test Attack Rampart")!!) shouldBe game.player2Id
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Test Attack Pinger" to "Test Attack Rampart"),
                ).error shouldBe null
                game.resolveTriggers()

                game.counters("Test Attack Rampart", CounterType.DEFENSE) shouldBe 5
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("CR 608.2h — an attacker that has left answers from last-known information") {

            test("sacrificed before the damage, it still hits the planeswalker it was attacking") {
                val game = board("Ajani Goldmane", player = listOf("Test Attack Martyr"))
                val loyalty = game.counters("Ajani Goldmane", CounterType.LOYALTY)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Test Attack Martyr" to "Ajani Goldmane"),
                ).error shouldBe null
                game.resolveTriggers()

                game.isInGraveyard(1, "Test Attack Martyr") shouldBe true
                game.counters("Ajani Goldmane", CounterType.LOYALTY) shouldBe loyalty - 2
                game.getLifeTotal(2) shouldBe 20
            }

            test("sacrificed before the damage, it still hits the player it was attacking") {
                val game = board(player = listOf("Test Attack Martyr"))
                game.declareAttackers(mapOf("Test Attack Martyr" to 2)).error shouldBe null
                game.resolveTriggers()

                game.isInGraveyard(1, "Test Attack Martyr") shouldBe true
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
