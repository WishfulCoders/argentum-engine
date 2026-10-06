package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urza's Saga (MH2 #259) — a Saga that is a land.
 *
 * Played from hand it enters with a lore counter (CR 714.3a) and chapter I triggers; chapter I gives
 * it "{T}: Add {C}", chapter II "{2}, {T}: create a 0/0 Construct that gets +1/+1 per artifact you
 * control", and chapter III tutors an artifact with mana cost {0} or {1} — *mana cost*, not mana
 * value (ruling 2021-06-18) — onto the battlefield, after which the Saga is sacrificed (CR 714.4).
 */
class UrzasSagaScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.saga(): EntityId? = findPermanent("Urza's Saga")
        fun TestGame.lore(): Int =
            saga()?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.LORE) } ?: -1
        fun TestGame.grantedAbilityIds(id: EntityId) =
            state.grantedActivatedAbilities.filter { it.entityId == id }.map { it.ability }

        fun TestGame.toOwnNextMain() {
            val start = state.turnNumber
            var guard = 0
            while (guard++ < 8) {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (state.activePlayerId == player1Id && state.turnNumber > start) break
            }
        }

        context("Urza's Saga") {
            test("played as a land it is a Saga: chapters I-III, then it is sacrificed") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Urza's Saga")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Islands on top for the draws; the artifacts below them for chapter III.
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Island")
                }
                builder
                    .withCardInLibrary(1, "Ornithopter")          // {0}: findable
                    .withCardInLibrary(1, "Sol Ring")             // {1}: findable
                    .withCardInLibrary(1, "Chalice of the Void")  // {X}{X}: mana value 0, not {0}
                    .withCardInLibrary(1, "Lotus Bloom")          // no mana cost (CR 202.1b)
                    .withCardInLibrary(1, "Bottle Gnomes")        // {3}
                val game = builder.build()

                val sagaCard = game.findCardsInHand(1, "Urza's Saga").single()
                game.execute(PlayLand(game.player1Id, sagaCard)).error shouldBe null
                withClue("played as a land, it entered as a Saga with one lore counter") {
                    game.state.getEntity(sagaCard)?.get<SagaComponent>() shouldNotBe null
                    game.lore() shouldBe 1
                }
                game.resolveStack()

                // Chapter I: "{T}: Add {C}."
                val manaAbility = game.grantedAbilityIds(sagaCard).single()
                withClue("chapter I granted a mana ability") { manaAbility.isManaAbility shouldBe true }
                game.execute(ActivateAbility(game.player1Id, sagaCard, manaAbility.id)).error shouldBe null
                withClue("tapping the Saga added {C}") {
                    game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
                }

                // Chapter II on the next turn: "{2}, {T}: Create a 0/0 Construct ..."
                game.toOwnNextMain()
                game.lore() shouldBe 2
                game.resolveStack()
                val abilities = game.grantedAbilityIds(sagaCard)
                withClue("the Saga keeps chapter I's ability and gains chapter II's") { abilities.size shouldBe 2 }
                val construct = abilities.single { !it.isManaAbility }
                game.execute(ActivateAbility(game.player1Id, sagaCard, construct.id)).error shouldBe null
                game.resolveStack()
                val token = game.findPermanent("Construct Token")
                withClue("a Construct token was created and counts itself as an artifact") {
                    token shouldNotBe null
                    game.state.projectedState.getPower(token!!) shouldBe 1
                    game.state.projectedState.getToughness(token!!) shouldBe 1
                }

                // Chapter III: only the {0} and {1} artifacts are offered.
                game.toOwnNextMain()
                game.lore() shouldBe 3
                game.resolveStack()
                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val ornithopter = game.findCardsInLibrary(1, "Ornithopter").single()
                val solRing = game.findCardsInLibrary(1, "Sol Ring").single()
                withClue("mana cost {0} or {1} — not mana value") {
                    search.options shouldContainExactlyInAnyOrder listOf(ornithopter, solRing)
                }
                game.selectCards(listOf(solRing))
                game.resolveStack()

                withClue("Sol Ring was put onto the battlefield") { game.isOnBattlefield("Sol Ring") shouldBe true }
                withClue("the Construct now sees two artifacts") {
                    game.state.projectedState.getPower(token!!) shouldBe 2
                }
                withClue("after chapter III the Saga is sacrificed (CR 714.4)") {
                    game.saga() shouldBe null
                    game.isInGraveyard(1, "Urza's Saga") shouldBe true
                }
            }
        }
    }
}
