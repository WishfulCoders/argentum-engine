package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for player-target restrictions — the `restriction: Condition?` slot on
 * [TargetPlayer] / [com.wingedsheep.sdk.scripting.targets.TargetOpponent] (TargetUnion PR 1).
 *
 * The restriction is evaluated against each candidate player with
 * [com.wingedsheep.sdk.scripting.references.Player.Candidate] bound to that player. These tests
 * pin the three behaviours the engine must guarantee:
 *
 *  1. Legal-target *enumeration* excludes players who fail the restriction (and "target player"
 *     spans every player — controller included — when they satisfy it).
 *  2. *Cast/activation validation* rejects an illegal player target.
 *  3. *Resolution re-check* (CR 608.2b): a target whose restriction stopped holding between
 *     targeting and resolution is removed, fizzling a single-target spell.
 *
 * Driven by two inline cards: a drainer whose ability mirrors Rix Maadi Guildmage's real
 * "target player who lost life this turn" ability, and a life-threshold sorcery ("target player
 * with 10 or less life") whose restriction can flip false mid-stack to exercise the re-check.
 */
class PlayerTargetRestrictionScenarioTest : ScenarioTestBase() {

    // {B}: Target player who lost life this turn loses 1 life.  (cf. Rix Maadi Guildmage)
    private val lostLifeRestriction = Conditions.candidateLostLifeThisTurn()
    private val drainer = card("Restriction Test Drainer") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        oracleText = "{B}: Target player who lost life this turn loses 1 life."
        activatedAbility {
            cost = com.wingedsheep.sdk.dsl.Costs.Mana("{B}")
            val t = target(TargetPlayer(
                    restriction = lostLifeRestriction,
                    descriptionOverride = "target player who lost life this turn"
                ))
            effect = LoseLifeEffect(DynamicAmount.Fixed(1), t)
        }
    }

    // Target player with 10 or less life loses 3 life.
    private val lifeAtMostRestriction = Conditions.candidateLifeAtMost(10)
    private val lance = card("Restriction Test Lance") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Target player with 10 or less life loses 3 life."
        spell {
            val t = target(TargetPlayer(
                    restriction = lifeAtMostRestriction,
                    descriptionOverride = "target player with 10 or less life"
                ))
            effect = LoseLifeEffect(DynamicAmount.Fixed(3), t)
        }
    }

    private val drainerAbilityId = drainer.activatedAbilities.first().id

    // Enumerate legal player targets the same way the legal-action layer does — through
    // TargetEnumerationUtils, the path that feeds the client's selectable-target list.
    private val targetEnumerator = TargetEnumerationUtils(services.predicateEvaluator)

    init {
        cardRegistry.register(drainer)
        cardRegistry.register(lance)

        context("\"target player who lost life this turn\" (LIFE_LOST tracker restriction)") {

            test("enumeration: only the opponent who lost life this turn is a legal target; resolving drains them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Restriction Test Drainer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent lost life this turn; controller did not.
                game.state = game.state.updateEntity(game.player2Id) { it.with(LifeLostThisTurnComponent) }
                val drainerId = game.findPermanent("Restriction Test Drainer")!!

                val targets = targetEnumerator.findValidTargets(
                    game.state, game.player1Id,
                    TargetPlayer(restriction = lostLifeRestriction), drainerId
                )
                withClue("Restriction filters enumeration to the opponent who lost life") {
                    targets shouldContain game.player2Id
                    targets shouldNotContain game.player1Id
                }

                val before = game.getLifeTotal(2)
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = drainerId,
                        abilityId = drainerAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Activating against a player who lost life is legal: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()
                withClue("Resolution drains the targeted player for 1") {
                    game.getLifeTotal(2) shouldBe before - 1
                }
            }

            test("validation: activating against a player who did NOT lose life is rejected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Restriction Test Drainer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Nobody lost life this turn.
                val drainerId = game.findPermanent("Restriction Test Drainer")!!
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = drainerId,
                        abilityId = drainerAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("The restriction is unmet, so the activation is rejected") {
                    activation.error shouldNotBe null
                }
            }

            test("enumeration: the controller is a legal target when THEY lost life (target player spans all players)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Restriction Test Drainer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Only the controller lost life this turn.
                game.state = game.state.updateEntity(game.player1Id) { it.with(LifeLostThisTurnComponent) }
                val drainerId = game.findPermanent("Restriction Test Drainer")!!

                val targets = targetEnumerator.findValidTargets(
                    game.state, game.player1Id,
                    TargetPlayer(restriction = lostLifeRestriction), drainerId
                )
                withClue("\"target player\" includes the controller when they satisfy the restriction") {
                    targets shouldContain game.player1Id
                    targets shouldNotContain game.player2Id
                }
            }

            test("enumeration: no player is a legal target when nobody lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Restriction Test Drainer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drainerId = game.findPermanent("Restriction Test Drainer")!!
                withClue("With no life lost this turn, the restriction excludes every player") {
                    targetEnumerator.findValidTargets(
                        game.state, game.player1Id,
                        TargetPlayer(restriction = lostLifeRestriction), drainerId
                    ) shouldBe emptyList()
                }
            }
        }

        context("\"target player with 10 or less life\" (life-threshold restriction + CR 608.2b)") {

            test("enumeration + cast: only a player at or below the threshold is a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Restriction Test Lance")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targets = targetEnumerator.findValidTargets(
                    game.state, game.player1Id,
                    TargetPlayer(restriction = lifeAtMostRestriction), sourceId = null
                )
                withClue("Opponent at 8 life (<=10) is legal; controller at 20 is not") {
                    targets shouldContain game.player2Id
                    targets shouldNotContain game.player1Id
                }

                game.castSpellTargetingPlayer(1, "Restriction Test Lance", targetPlayerNumber = 2)
                game.resolveStack()
                withClue("Resolving against the 8-life opponent drains 3") {
                    game.getLifeTotal(2) shouldBe 5
                }
            }

            test("validation: casting against a player above the threshold is rejected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Restriction Test Lance")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingPlayer(1, "Restriction Test Lance", targetPlayerNumber = 2)
                withClue("Both players are at 20 life, so no player satisfies the restriction") {
                    cast.error shouldNotBe null
                }
            }

            test("CR 608.2b: a target that rises above the threshold before resolution is removed and the spell fizzles") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Restriction Test Lance")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Legal at cast time (8 <= 10).
                val cast = game.castSpellTargetingPlayer(1, "Restriction Test Lance", targetPlayerNumber = 2)
                withClue("Targeting an 8-life player is legal at cast: ${cast.error}") {
                    cast.error shouldBe null
                }

                // Before it resolves, the opponent's life rises above the threshold.
                game.state = game.state.updateEntity(game.player2Id) { it.with(LifeTotalComponent(12)) }

                game.resolveStack()
                withClue(
                    "The only target became illegal (12 > 10), so the spell is removed from the " +
                        "stack without effect — the opponent loses no life."
                ) {
                    game.getLifeTotal(2) shouldBe 12
                    game.isInGraveyard(1, "Restriction Test Lance") shouldBe true
                }
            }
        }
    }
}
