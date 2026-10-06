package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evokeWith
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Evoke with a non-mana cost (CR 702.74a) — the Modern Horizons 2 Incarnations' "Evoke—Exile a
 * white card from your hand." The non-mana part rides `KeywordAbility.Evoke.additionalCosts`, so
 * it is the *evoke* alternative cost, not a separate self-alternative cost, and everything that
 * keys off "its evoke cost was paid" — the sacrifice trigger — works for it.
 *
 * The rules pinned here:
 *  - CR 702.74a: paying the evoke cost is an alternative cost; the permanent is sacrificed when it
 *    enters, after (or before — controller's order) its own enters trigger.
 *  - CR 118.3: a cost can't be paid without the resources — no white card to exile, no evoke.
 *  - CR 601.2a: the spell is on the stack while its costs are paid, so it can't exile itself.
 *  - CR 601.2h: the exile is part of the total cost; omitting it or exiling a wrong card fails.
 *  - CR 118.9c: the alternative cost doesn't change the spell's mana value.
 *  - CR 118.9d: cost increases apply on top of the alternative cost.
 */
class EvokeNonManaCostScenarioTest : ScenarioTestBase() {

    private val pitchEvoker = card("Pitch Evoker") {
        manaCost = "{3}{W}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Elemental Incarnation"
        power = 3
        toughness = 2
        oracleText = "When this creature enters, you gain 3 life.\n" +
            "Evoke—Exile a white card from your hand."
        evokeWith(
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.WHITE),
                fromZone = CostZone.HAND
            )
        )
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }

    // "Creature spells cost {1} more to cast." — a symmetric tax on the alternative cost.
    private val creatureTax = card("Creature Tax Probe") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Creature spells cost {1} more to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.AnyCaster(GameObjectFilter.Creature),
                modification = CostModification.IncreaseGeneric(1),
            )
        }
    }

    init {
        cardRegistry.register(pitchEvoker)
        cardRegistry.register(creatureTax)

        fun ScenarioTestBase.TestGame.cardInHand(name: String): EntityId =
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun ScenarioTestBase.TestGame.evokeActions(): List<LegalActionInfo> = getLegalActions(1).filter { info ->
            val cast = info.action as? CastSpell ?: return@filter false
            cast.alternativeCostType == AlternativeCostType.EVOKE &&
                state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Pitch Evoker"
        }

        fun ScenarioTestBase.TestGame.evoke(pitched: List<EntityId>?, type: AlternativeCostType? = AlternativeCostType.EVOKE) =
            execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = cardInHand("Pitch Evoker"),
                    useAlternativeCost = true,
                    alternativeCostType = type,
                    additionalCostPayment = pitched?.let { AdditionalCostPayment(exiledCards = it) }
                )
            )

        /** Resolve everything, ordering simultaneous triggers as presented. */
        fun ScenarioTestBase.TestGame.resolveAll() {
            var guard = 0
            while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 20) {
                val pending = state.pendingDecision
                if (pending is OrderObjectsDecision) {
                    submitDecision(OrderedResponse(pending.id, pending.objects)).error shouldBe null
                } else if (pending != null) {
                    error("unexpected decision: $pending")
                } else {
                    resolveStack()
                }
            }
        }

        fun board(vararg hand: String, plains: Int = 0, tax: Boolean = false) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Pitch Evoker")
            .let { b -> hand.fold(b) { acc, name -> acc.withCardInHand(1, name) } }
            .let { b -> if (plains > 0) b.withLandsOnBattlefield(1, "Plains", plains) else b }
            .let { b -> if (tax) b.withCardOnBattlefield(2, "Creature Tax Probe") else b }
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("pitching a white card evokes it for no mana: it enters, its trigger resolves, and it is sacrificed") {
            val game = board("Savannah Lions")
            val lions = game.cardInHand("Savannah Lions")

            val offered = game.evokeActions()
            withClue("the evoke cast is offered with no lands, carrying the exile picker") {
                offered.size shouldBe 1
                offered.single().additionalCostInfo.shouldNotBeNull()
                offered.single().description shouldContain "exile"
                offered.single().description shouldContain "white"
            }
            withClue("the hard cast {3}{W}{W} is not affordable, so evoke is the only cast") {
                game.getLegalActions(1).none { info ->
                    val cast = info.action as? CastSpell
                    cast != null && !cast.useAlternativeCost && info.isAffordable &&
                        game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Pitch Evoker"
                } shouldBe true
            }

            game.evoke(listOf(lions)).error shouldBe null
            withClue("the pitched card is exiled as the cost is paid (CR 601.2h)") {
                game.isInExile(1, "Savannah Lions") shouldBe true
                game.isInHand(1, "Savannah Lions") shouldBe false
            }
            withClue("an alternative cost doesn't change mana value (CR 118.9c)") {
                val onStack = game.state.stack.single()
                game.state.getEntity(onStack)!!.get<CardComponent>()!!.manaCost.cmc shouldBe 5
            }

            game.resolveAll()
            withClue("the enters trigger resolved") { game.getLifeTotal(1) shouldBe 23 }
            withClue("evoke's sacrifice trigger resolved too (CR 702.74a)") {
                game.isOnBattlefield("Pitch Evoker") shouldBe false
                game.isInGraveyard(1, "Pitch Evoker") shouldBe true
            }
        }

        test("the card can't pitch itself, and a card of another colour doesn't pay the cost") {
            val game = board("Grizzly Bears")
            val self = game.cardInHand("Pitch Evoker")
            val bears = game.cardInHand("Grizzly Bears")

            withClue("no other white card in hand: evoke isn't offered (CR 118.3, 601.2a)") {
                game.evokeActions() shouldBe emptyList()
            }
            withClue("exiling itself is rejected — it is on the stack while costs are paid") {
                game.evoke(listOf(self)).error shouldNotBe null
            }
            withClue("exiling a green card is rejected") {
                game.evoke(listOf(bears)).error shouldNotBe null
            }
            withClue("omitting the exile is rejected — partial payments aren't allowed (CR 601.2h)") {
                game.evoke(null).error shouldNotBe null
            }
            game.isInHand(1, "Pitch Evoker") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("a cast with no recorded alternative-cost type still owes the exile") {
            // Hand-built actions leave the discriminator null; the legacy chain resolves to evoke,
            // and its non-mana part must come with it rather than making the cast free.
            val game = board("Savannah Lions")
            withClue("without a payment the null-type alternative cast is rejected") {
                game.evoke(null, type = null).error shouldNotBe null
            }
            game.evoke(listOf(game.cardInHand("Savannah Lions")), type = null).error shouldBe null
            game.isInExile(1, "Savannah Lions") shouldBe true
            game.resolveAll()
            game.isInGraveyard(1, "Pitch Evoker") shouldBe true
        }

        test("hard-casting pays mana, exiles nothing and keeps the creature") {
            val game = board("Savannah Lions", plains = 5)
            game.castSpell(1, "Pitch Evoker").error shouldBe null
            game.resolveAll()
            game.getLifeTotal(1) shouldBe 23
            game.isOnBattlefield("Pitch Evoker") shouldBe true
            game.isInHand(1, "Savannah Lions") shouldBe true
        }

        test("a cost increase is added to the non-mana evoke cost (CR 118.9d)") {
            val untapped = board("Savannah Lions", tax = true)
            withClue("with the tax and no mana, evoke is unaffordable") {
                untapped.evokeActions() shouldBe emptyList()
            }

            val game = board("Savannah Lions", plains = 1, tax = true)
            val offered = game.evokeActions().single()
            offered.manaCostString shouldBe "{1}"
            game.execute(offered.action.let { (it as CastSpell).copy(
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(game.cardInHand("Savannah Lions")))
            ) }).error shouldBe null
            withClue("the taxed evoke paid {1} as well as the exile") {
                game.state.getBattlefield(game.player1Id).count { id ->
                    game.state.getEntity(id)?.has<TappedComponent>() == true
                } shouldBe 1
                game.isInExile(1, "Savannah Lions") shouldBe true
            }
        }

        test("the client card names the non-mana evoke cost; a mana-only evoke card has none") {
            val game = board("Mulldrifter")
            val client = stateTransformer.transform(game.state, game.player1Id)
            val evoker = client.cards[game.cardInHand("Pitch Evoker")]!!
            evoker.evokeAdditionalCost.shouldNotBeNull() shouldContain "white"
            evoker.evoke shouldBe "{0}"
            val mulldrifter = client.cards[game.cardInHand("Mulldrifter")]!!
            mulldrifter.evoke shouldBe "{2}{U}"
            mulldrifter.evokeAdditionalCost.shouldBeNull()
        }
    }
}
