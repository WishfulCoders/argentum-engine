package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.HeraldsHorn
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Herald's Horn {3} — Artifact (C17).
 *
 * "As this artifact enters, choose a creature type.
 *  Creature spells you cast of the chosen type cost {1} less to cast.
 *  At the beginning of your upkeep, look at the top card of your library. If it's a creature card
 *  of the chosen type, you may reveal it and put it into your hand."
 *
 * Pins the as-enters choice driving both the discount (yours only, generic only) and the upkeep
 * look, and that a non-matching top card stays where it is.
 */
class HeraldsHornScenarioTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(TestCards.all)
        register(HeraldsHorn)
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HeraldsHorn)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castHornChoosing(player: EntityId, creatureType: String) {
        giveColorlessMana(player, 3)
        val card = putCardInHand(player, "Herald's Horn")
        castSpell(player, card).error shouldBe null
        bothPass()

        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(creatureType)
        (index >= 0) shouldBe true
        submitDecision(player, OptionChosenResponse(decision.id, index))
    }

    fun GameTestDriver.costFor(caster: EntityId, cardName: String) =
        registry().let { reg ->
            CostCalculator(reg, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
                .calculateEffectiveCost(state, reg.requireCard(cardName), caster)
        }

    test("creature spells you cast of the chosen type cost {1} less; opponents and other types pay full") {
        val d = createDriver()
        val you = d.player1
        val opponent = d.getOpponent(you)
        d.castHornChoosing(you, "Zombie")

        withClue("Gurmag Angler {6}{B} (Zombie Fish) costs {5}{B} for you") {
            d.costFor(you, "Gurmag Angler").genericAmount shouldBe 5
        }
        withClue("the discount is only for spells you cast") {
            d.costFor(opponent, "Gurmag Angler").genericAmount shouldBe 6
        }
        withClue("Centaur Courser is not a Zombie") {
            d.costFor(you, "Centaur Courser").genericAmount shouldBe 2
        }
    }

    test("at your upkeep a creature card of the chosen type on top may be revealed into your hand") {
        val d = createDriver()
        val you = d.player1
        d.castHornChoosing(you, "Zombie")
        val angler = d.putCardOnTopOfLibrary(you, "Gurmag Angler")

        d.passPriorityUntil(Step.UPKEEP, you)
        d.bothPass()

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(angler)
        d.submitCardSelection(you, listOf(angler)).error shouldBe null

        d.getHand(you).contains(angler) shouldBe true
    }

    test("a top card that isn't a creature of the chosen type stays on top of your library") {
        val d = createDriver()
        val you = d.player1
        d.castHornChoosing(you, "Zombie")
        val courser = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.passPriorityUntil(Step.UPKEEP, you)
        d.bothPass()

        (d.pendingDecision is SelectCardsDecision) shouldBe false
        d.getHand(you).contains(courser) shouldBe false
        d.state.getLibrary(you).first() shouldBe courser
    }
})
