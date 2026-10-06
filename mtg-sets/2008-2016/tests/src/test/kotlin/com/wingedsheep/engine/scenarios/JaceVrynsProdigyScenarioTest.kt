package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace, Vryn's Prodigy // Jace, Telepath Unbound (ORI #60).
 *
 * Front: the loot checks the graveyard *after* the discard, and the flip is an exile-and-return
 * that finds nothing once Jace has left the battlefield (CR 400.7). Back: the −3's cast permission
 * pays the card's costs at its normal timing and exiles the spell instead of letting it return to
 * the graveyard — but loses track of a card that changes zones (ruling).
 */
class JaceVrynsProdigyScenarioTest : ScenarioTestBase() {
    private val front = "Jace, Vryn's Prodigy"
    private val back = "Jace, Telepath Unbound"

    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, front, summoningSickness = false)
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")

        test("the loot reaching five cards in the graveyard flips Jace into a new planeswalker with five loyalty") {
            val game = base()
                .withCardInGraveyard(1, "Grizzly Bears").withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest").withCardInGraveyard(1, "Mountain").build()
            loot(game)
            game.graveyardSize(1) shouldBe 5
            val walker = game.findPermanent(back)
            walker shouldNotBe null
            game.findPermanent(front) shouldBe null
            loyalty(game, walker!!) shouldBe 5
        }

        test("four cards after the discard: no flip") {
            val game = base()
                .withCardInGraveyard(1, "Grizzly Bears").withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest").build()
            loot(game)
            game.graveyardSize(1) shouldBe 4
            game.findPermanent(front) shouldNotBe null
            game.findPermanent(back) shouldBe null
        }

        test("bounced in response, Jace still loots but stays in hand (CR 400.7)") {
            val game = base()
                .withCardInGraveyard(1, "Grizzly Bears").withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest").withCardInGraveyard(1, "Mountain")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 1).build()
            val jace = game.findPermanent(front)!!
            activate(game, jace, frontAbility(0))
            game.castSpell(1, "Unsummon", jace).error shouldBe null
            game.resolveStack()
            discardIfAsked(game)
            game.resolveStack()
            game.isInHand(1, front) shouldBe true
            game.findPermanent(back) shouldBe null
            withClue("drew and discarded: Unsummon and the discarded card joined the four") {
                game.graveyardSize(1) shouldBe 6
            }
        }

        context("Jace, Telepath Unbound") {
            fun flipped(extra: (ScenarioBuilder) -> ScenarioBuilder = { it }): Pair<TestGame, EntityId> {
                val game = extra(
                    base().withCardInGraveyard(1, "Lightning Bolt").withCardInGraveyard(1, "Hill Giant")
                        .withCardInGraveyard(1, "Forest").withCardInGraveyard(1, "Mountain")
                ).build()
                loot(game)
                return game to game.findPermanent(back)!!
            }

            test("+1: target creature gets -2/-0") {
                val (game, jace) = flipped { it.withCardOnBattlefield(2, "Hill Giant") }
                val giant = game.findPermanents("Hill Giant").single()
                activate(game, jace, backAbility(0), listOf(ChosenTarget.Permanent(giant)))
                game.resolveStack()
                game.state.projectedState.getPower(giant) shouldBe 1
                loyalty(game, jace) shouldBe 6
            }

            test("+1 may be activated with no target") {
                val (game, jace) = flipped()
                activate(game, jace, backAbility(0))
                game.resolveStack()
                loyalty(game, jace) shouldBe 6
            }

            test("−3: cast the targeted card from the graveyard, paying for it; it is exiled after resolving") {
                val (game, jace) = flipped { it.withLandsOnBattlefield(1, "Mountain", 1) }
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                activate(game, jace, backAbility(1), listOf(ChosenTarget.Card(bolt, game.player1Id, Zone.GRAVEYARD)))
                game.resolveStack()
                loyalty(game, jace) shouldBe 2
                game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe false
            }

            test("−3: only the targeted card gets the permission, and it costs mana") {
                val (game, jace) = flipped { it.withCardInGraveyard(1, "Shock") }
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val shock = game.findCardsInGraveyard(1, "Shock").single()
                activate(game, jace, backAbility(1), listOf(ChosenTarget.Card(bolt, game.player1Id, Zone.GRAVEYARD)))
                game.resolveStack()
                withClue("no mana available") {
                    game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldNotBe null
                }
                game.execute(CastSpell(game.player1Id, shock, listOf(ChosenTarget.Player(game.player2Id)))).error shouldNotBe null
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }

            test("−9: the emblem makes target opponent mill five whenever you cast a spell") {
                val (game, jace) = flipped {
                    it.withCardInHand(1, "Grizzly Bears").withLandsOnBattlefield(1, "Forest", 2)
                        .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                        .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                }
                game.state = game.state.updateEntity(jace) { c ->
                    c.with(CountersComponent(mapOf(CounterType.LOYALTY to 9)))
                }
                activate(game, jace, backAbility(2))
                game.resolveStack()
                game.findPermanent(back) shouldBe null
                game.state.globalGrantedTriggeredAbilities.size shouldBe 1

                val before = game.librarySize(2)
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
                game.librarySize(2) shouldBe before - 5
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }

    private fun frontAbility(index: Int) = cardRegistry.getCard(front)!!.script.activatedAbilities[index]
    private fun backAbility(index: Int) = cardRegistry.getCard(front)!!.backFace!!.script.activatedAbilities[index]

    private fun activate(
        game: TestGame,
        source: EntityId,
        ability: com.wingedsheep.sdk.scripting.ActivatedAbility,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        game.execute(ActivateAbility(game.player1Id, source, ability.id, targets = targets)).error shouldBe null
    }

    /** Activate the loot and resolve it, discarding the drawn card if asked. */
    private fun loot(game: TestGame) {
        activate(game, game.findPermanent(front)!!, frontAbility(0))
        game.resolveStack()
        discardIfAsked(game)
        game.resolveStack()
    }

    private fun discardIfAsked(game: TestGame) {
        val decision = game.getPendingDecision() as? SelectCardsDecision ?: return
        // Discard the drawn Island — never Jace (the bounce case) or a card the test casts later.
        val pick = decision.options.firstOrNull { game.state.getEntity(it)?.get<CardComponent>()?.name == "Island" }
            ?: decision.options.first()
        game.selectCards(listOf(pick)).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
