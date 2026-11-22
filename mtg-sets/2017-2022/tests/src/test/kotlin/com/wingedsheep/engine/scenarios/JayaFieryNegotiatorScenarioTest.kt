package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jaya, Fiery Negotiator (DMU #133, {2}{R}{R}, Loyalty 4).
 *
 *   −2: Choose target creature an opponent controls. Whenever you attack this turn, Jaya deals
 *       damage equal to the number of attacking creatures to that creature.
 *   −8: You get an emblem with "Whenever you cast a red instant or sorcery spell, copy it twice.
 *       You may choose new targets for the copies."
 *
 * The −2 is the composition worth pinning: the target is chosen on activation and handed to a
 * delayed attack trigger through a carried collection, and the damage is counted when the attack
 * happens. The −8 is pinned through its emblem outliving Jaya and tripling a Lightning Bolt.
 */
class JayaFieryNegotiatorScenarioTest : ScenarioTestBase() {

    init {
        context("the −2") {

            test("the chosen creature takes damage equal to the number of attackers when you attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jaya, Fiery Negotiator")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jaya = game.findPermanent("Jaya, Fiery Negotiator")!!
                val giant = game.findPermanent("Hill Giant")!!
                activate(game, jaya, index = 2, targets = listOf(ChosenTarget.Permanent(giant)))
                game.resolveStack()
                withClue("−2 took Jaya from 4 to 2, and nothing has happened to the Giant yet") {
                    loyalty(game, jaya) shouldBe 2
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2, "Centaur Courser" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("three attackers → 3 damage kills the 3/3 Hill Giant") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("the damage went to that creature, not the opponent") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("the damage counts the attackers, so fewer attackers deal less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jaya, Fiery Negotiator")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jaya = game.findPermanent("Jaya, Fiery Negotiator")!!
                val giant = game.findPermanent("Hill Giant")!!
                activate(game, jaya, index = 2, targets = listOf(ChosenTarget.Permanent(giant)))
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.resolveStack()

                withClue("two attackers → 2 damage; the 3/3 survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.state.getEntity(giant)
                        ?.get<DamageComponent>()
                        ?.amount shouldBe 2
                }
            }
        }

        context("the −8") {

            test("the emblem copies a red instant twice, even with Jaya gone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jaya, Fiery Negotiator")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jaya = game.findPermanent("Jaya, Fiery Negotiator")!!
                setLoyalty(game, jaya, 8)
                activate(game, jaya, index = 3)
                game.resolveStack()
                withClue("Jaya went to 0 loyalty; the emblem is global") {
                    game.findPermanent("Jaya, Fiery Negotiator") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                var guard = 0
                while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 40) {
                    if (game.getPendingDecision() is ChooseTargetsDecision) {
                        game.selectTargets(listOf(game.player2Id))
                    } else if (game.hasPendingDecision()) {
                        game.submitManaSourcesAutoPay()
                    } else {
                        game.passPriority()
                    }
                }

                withClue("the original Bolt plus two copies: 9 damage") {
                    game.getLifeTotal(2) shouldBe 11
                }
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Jaya, Fiery Negotiator")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
