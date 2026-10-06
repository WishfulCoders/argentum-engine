package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.DiceRolls
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DIE_ROLL_RESULT
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Die rolling (CR 706) end to end through real spells and a planeswalker:
 *
 * - **Results tables (CR 706.3a)** — `Patterns.Mechanic.rollDie` runs exactly the row whose range
 *   holds the result, at every row boundary (1, 9, 10, 19, 20), and nothing when no row matches.
 * - **The result as a number (CR 706.4)** — a later step reads the stored result.
 * - **Modifiers (CR 706.2)** — the table reads the modified result; the event keeps the natural one.
 * - **A row that pauses** — a scry row resumes and the remaining rows stay off.
 * - **Loyalty that depends on the roll** — a Comet, Stellar Pup-shaped planeswalker whose `0:`
 *   ability rolls a d6 and puts or removes loyalty as part of the *effect* (not a cost — Comet's
 *   2022-10-07 ruling), deals damage equal to its loyalty, and on a 6 may activate twice more.
 *
 * Each roll is forced by seeding the game's RNG right before resolution with a seed whose first
 * draw is the wanted face, which also pins that the roll is drawn from the seeded generator.
 */
class RollDieScenarioTest : ScenarioTestBase() {

    private val gainTable = card("Test Dice Gain") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Roll a d20.\n1—9 | You gain 1 life.\n10—19 | You gain 2 life.\n20 | You gain 5 life."
        spell {
            effect = Patterns.Mechanic.rollDie(
                20,
                1..9 to Effects.GainLife(1),
                10..19 to Effects.GainLife(2),
                20..20 to Effects.GainLife(5),
            )
        }
    }

    private val gapTable = card("Test Dice Gap") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Roll a d6.\n1—2 | You gain 1 life.\n5 | You gain 5 life."
        spell {
            effect = Patterns.Mechanic.rollDie(6, 1..2 to Effects.GainLife(1), 5..5 to Effects.GainLife(5))
        }
    }

    private val resultAsNumber = card("Test Dice Number") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Roll a d20. You gain life equal to the result."
        spell {
            effect = Effects.RollDie(20) then Effects.GainLife(DynamicAmounts.storedNumber(DIE_ROLL_RESULT))
        }
    }

    private val modified = card("Test Dice Modified") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Roll a d20 and subtract 5.\n0 or less | You lose 3 life.\n1—9 | You gain 1 life.\n10+ | You gain 2 life."
        spell {
            effect = Patterns.Mechanic.rollDie(
                20,
                Int.MIN_VALUE..0 to Effects.LoseLife(3, EffectTarget.Controller),
                1..9 to Effects.GainLife(1),
                10..Int.MAX_VALUE to Effects.GainLife(2),
                modifier = DynamicAmount.Fixed(-5),
            )
        }
    }

    private val scryTable = card("Test Dice Scry") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Roll a d20.\n1—9 | Scry 1, then you gain 1 life.\n10—20 | Scry 2, then you gain 2 life."
        spell {
            effect = Patterns.Mechanic.rollDie(
                20,
                1..9 to (Effects.Scry(1) then Effects.GainLife(1)),
                10..20 to (Effects.Scry(2) then Effects.GainLife(2)),
            )
        }
    }

    /**
     * Comet, Stellar Pup's shape, with "a creature or player" narrowed to each opponent. The 6 row's
     * `AllowAdditionalLoyaltyActivationsThisTurn()` is additive, so a second 6 the same turn adds two
     * more again.
     */
    private fun pup(name: String, loyalty: Int) = card(name) {
        manaCost = "{2}{R}{W}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = loyalty
        oracleText = "0: Roll a six-sided die.\n1 or 2 — [+2], then you gain 2 life.\n3 — [−1].\n" +
            "4 or 5 — $name deals damage equal to the number of loyalty counters on it to each opponent, then [−2].\n" +
            "6 — [+1], and you may activate $name's loyalty ability two more times this turn."
        loyaltyAbility(0) {
            effect = Patterns.Mechanic.rollDie(
                6,
                1..2 to (Effects.AddCounters(CounterType.LOYALTY, 2, EffectTarget.Self) then Effects.GainLife(2)),
                3..3 to Effects.RemoveCounters(CounterType.LOYALTY, 1, EffectTarget.Self),
                4..5 to (
                    Effects.DealDamage(
                        DynamicAmounts.countersOnSelf(CounterType.LOYALTY),
                        EffectTarget.PlayerRef(Player.EachOpponent)
                    ) then Effects.RemoveCounters(CounterType.LOYALTY, 2, EffectTarget.Self)
                    ),
                6..6 to (
                    Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self) then
                        Effects.AllowAdditionalLoyaltyActivationsThisTurn()
                    ),
            )
        }
    }

    private fun TestGame.cast(name: String, natural: Int, sides: Int = 20): List<DieRolledEvent> {
        castSpell(1, name).error shouldBe null
        state = state.copy(rng = DiceRolls.rngRolling(sides, natural))
        return resolveStack().flatMap { it.events }.filterIsInstance<DieRolledEvent>()
    }

    private fun board(vararg hand: String, library: Int = 0): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        hand.forEach { b.withCardInHand(1, it) }
        repeat(library) { b.withCardInLibrary(1, "Forest") }
        b.withCardInLibrary(2, "Forest")
        return b.build()
    }

    private fun pupBoard(name: String): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, name)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.loyalty(name: String): Int =
        state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun TestGame.activatePup(name: String, natural: Int): List<DieRolledEvent> {
        val source = findPermanent(name)!!
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities.first { it.cost is AbilityCost.Loyalty }
        withClue("activation of $name should be legal") {
            execute(ActivateAbility(player1Id, source, ability.id)).error shouldBe null
        }
        state = state.copy(rng = DiceRolls.rngRolling(6, natural))
        return resolveStack().flatMap { it.events }.filterIsInstance<DieRolledEvent>()
    }

    private fun TestGame.canActivate(name: String): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    init {
        listOf(gainTable, gapTable, resultAsNumber, modified, scryTable, pup("Test Pup", 5), pup("Test Pup One", 1))
            .forEach { cardRegistry.register(it) }

        context("CR 706.3a: the row whose range holds the result happens, and only that row") {
            for ((natural, gain) in listOf(1 to 1, 9 to 1, 10 to 2, 19 to 2, 20 to 5)) {
                test("a natural $natural gains $gain") {
                    val game = board("Test Dice Gain")
                    val rolls = game.cast("Test Dice Gain", natural)
                    rolls.size shouldBe 1
                    rolls.single().naturalResult shouldBe natural
                    rolls.single().result shouldBe natural
                    rolls.single().sides shouldBe 20
                    rolls.single().playerId shouldBe game.player1Id
                    rolls.single().sourceName shouldBe "Test Dice Gain"
                    game.getLifeTotal(1) shouldBe 20 + gain
                }
            }

            test("a result in no row does nothing") {
                val game = board("Test Dice Gap")
                game.cast("Test Dice Gap", natural = 3, sides = 6).single().result shouldBe 3
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("CR 706.4: a later step reads the result as a number") {
            val game = board("Test Dice Number")
            game.cast("Test Dice Number", 14)
            game.getLifeTotal(1) shouldBe 34
        }

        context("CR 706.2: the table reads the modified result; the event keeps the natural one") {
            test("natural 5 - 5 = 0 hits the '0 or less' row") {
                val game = board("Test Dice Modified")
                val roll = game.cast("Test Dice Modified", 5).single()
                roll.naturalResult shouldBe 5
                roll.result shouldBe 0
                game.getLifeTotal(1) shouldBe 17
            }
            test("natural 14 - 5 = 9 hits the 1-9 row, not 10+") {
                val game = board("Test Dice Modified")
                game.cast("Test Dice Modified", 14).single().result shouldBe 9
                game.getLifeTotal(1) shouldBe 21
            }
            test("natural 20 - 5 = 15 hits the open-ended 10+ row") {
                val game = board("Test Dice Modified")
                game.cast("Test Dice Modified", 20).single().result shouldBe 15
                game.getLifeTotal(1) shouldBe 22
            }
        }

        test("a row that pauses for a scry resumes, finishes its own effects, and fires no other row") {
            val game = board("Test Dice Scry", library = 3)
            game.cast("Test Dice Scry", 12)
            var looked = 0
            repeat(4) {
                when (val decision = game.getPendingDecision()) {
                    is SelectCardsDecision -> {
                        looked = decision.options.size
                        game.submitDecision(CardsSelectedResponse(decision.id, emptyList()))
                    }
                    is ReorderLibraryDecision -> game.submitDecision(OrderedResponse(decision.id, decision.cards))
                    else -> {}
                }
            }
            game.resolveStack()
            looked shouldBe 2
            game.getLifeTotal(1) shouldBe 22
        }

        context("loyalty that depends on the roll (Comet, Stellar Pup's shape)") {
            test("1 or 2: [+2] is put on as part of the effect, not paid as a cost") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 2).single().result shouldBe 2
                g.loyalty("Test Pup") shouldBe 7
                g.getLifeTotal(1) shouldBe 22
            }

            test("3: [−1]") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 3)
                g.loyalty("Test Pup") shouldBe 4
            }

            test("4 or 5: damage equal to loyalty, read before the [−2]") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 4)
                g.getLifeTotal(2) shouldBe 15
                g.loyalty("Test Pup") shouldBe 3
            }

            test("4 or 5 at one loyalty: deals 1, then the last counter goes and it dies (Comet ruling)") {
                val g = pupBoard("Test Pup One")
                g.activatePup("Test Pup One", 5)
                g.getLifeTotal(2) shouldBe 19
                g.isOnBattlefield("Test Pup One") shouldBe false
                g.isInGraveyard(1, "Test Pup One") shouldBe true
            }

            test("6: [+1], and the ability may be activated two more times this turn — not three") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 6)
                g.loyalty("Test Pup") shouldBe 6
                g.canActivate("Test Pup") shouldBe true
                g.activatePup("Test Pup", 3)
                g.canActivate("Test Pup") shouldBe true
                g.activatePup("Test Pup", 3)
                g.loyalty("Test Pup") shouldBe 4
                g.canActivate("Test Pup") shouldBe false
            }

            test("a second 6 the same turn adds two more again") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 6)
                g.activatePup("Test Pup", 6)
                g.loyalty("Test Pup") shouldBe 7
                repeat(3) {
                    g.canActivate("Test Pup") shouldBe true
                    g.activatePup("Test Pup", 3)
                }
                g.loyalty("Test Pup") shouldBe 4
                g.canActivate("Test Pup") shouldBe false
            }

            test("without a 6 the once-per-turn loyalty rule (CR 606.3) still holds") {
                val g = pupBoard("Test Pup")
                g.activatePup("Test Pup", 1)
                g.canActivate("Test Pup") shouldBe false
            }
        }
    }
}
