package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mir.cards.LionsEyeDiamond
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lion's Eye Diamond (MIR #307) — "Discard your hand, Sacrifice this artifact: Add three mana of any
 * one color. Activate only as an instant."
 *
 * A mana ability that can only be activated while its controller holds priority (CR 602.5e, CR 605.1;
 * 2004-10-04 ruling), never while a cost is being paid.
 */
class LionsEyeDiamondScenarioTest : FunSpec({

    val wardedBear = card("LED Test Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LionsEyeDiamond, wardedBear))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun ledAbility() = LionsEyeDiamond.script.activatedAbilities.single().id

    fun GameTestDriver.activateLed(player: EntityId, led: EntityId, color: Color = Color.BLACK) =
        submit(ActivateAbility(playerId = player, sourceId = led, abilityId = ledAbility(), manaColorChoice = color))

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("with priority: discards the whole hand, sacrifices itself and adds three mana of one color, off the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val led = d.putPermanentOnBattlefield(you, "Lion's Eye Diamond")
        d.putCardInHand(you, "Lightning Bolt")
        d.putCardInHand(you, "Grizzly Bears")
        val handBefore = d.getHandSize(you)

        d.activateLed(you, led, Color.BLUE).error shouldBe null

        d.pool(you).blue shouldBe 3
        d.pool(you).total shouldBe 3
        d.getHandSize(you) shouldBe 0
        d.getGraveyardCardNames(you) shouldContainAll listOf("Lion's Eye Diamond", "Lightning Bolt", "Grizzly Bears")
        d.getGraveyard(you).size shouldBe handBefore + 1
        d.state.stack.size shouldBe 0
    }

    test("can be activated in response, with a spell on the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val led = d.putPermanentOnBattlefield(you, "Lion's Eye Diamond")
        d.giveMana(you, Color.RED, 1)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null

        d.activateLed(you, led, Color.RED).error shouldBe null
        d.pool(you).red shouldBe 3
        d.state.stack.size shouldBe 1
    }

    test("can't be activated while ward asks for a mana payment, and isn't one of the window's sources") {
        val d = driver()
        val you = d.activePlayer!!
        val led = d.putPermanentOnBattlefield(you, "Lion's Eye Diamond")
        val bear = d.putCreatureOnBattlefield(d.getOpponent(you), "LED Test Warded Bear")
        // Two Mountains make ward's {2} payable, so the game opens the payment window.
        repeat(2) { d.putLandOnBattlefield(you, "Mountain") }
        d.giveMana(you, Color.RED, 1)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(bear))).error shouldBe null
        d.bothPass()

        val window = d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        window.availableSources.map { it.entityId } shouldNotContain led
        d.services.legalActionEnumerator.enumerateManaAbilities(d.state, you)
            .mapNotNull { it.action as? ActivateAbility }.map { it.sourceId } shouldNotContain led

        d.activateLed(you, led).error.shouldNotBeNull()
        withClue("nothing was paid: the Diamond and the pool are untouched") {
            d.findPermanent(you, "Lion's Eye Diamond") shouldBe led
            d.pool(you).total shouldBe 0
        }
    }

    test("its mana is not counted toward casting a spell from hand — it would have to be activated first") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Lion's Eye Diamond")
        val bolt = d.putCardInHand(you, "Lightning Bolt")

        withClue("Lightning Bolt is not offered as affordable on the Diamond's mana") {
            d.legalActions(you).filter { (it.action as? CastSpell)?.cardId == bolt }
                .none { it.affordable }.shouldBeTrue()
        }
        withClue("but the Diamond itself is offered while you hold priority") {
            d.legalActions(you).mapNotNull { it.action as? ActivateAbility }.map { it.sourceId } shouldContain
                d.findPermanent(you, "Lion's Eye Diamond")!!
        }
    }
})
