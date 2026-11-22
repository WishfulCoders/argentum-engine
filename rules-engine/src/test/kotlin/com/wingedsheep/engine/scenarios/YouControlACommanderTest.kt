package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "If you control a commander, you may cast this spell without paying its mana cost" — the
 * `StatePredicate.IsCommander` predicate behind `Conditions.YouControlACommander`, exercised through
 * the two cards that gate a free cast on it (Fierce Guardianship, Deadly Rollick).
 *
 * The rules pinned here:
 *  - CR 903.3d: "controlling a commander" means a *permanent on the battlefield* that is a
 *    commander. A commander in the command zone or a graveyard doesn't count.
 *  - Ruling (2020-04-17): whose commander doesn't matter. An opponent's commander you control
 *    counts; your own commander that an opponent controls doesn't count for you.
 *  - CR 903.3: the designation is an attribute of the card, not a characteristic, so a face-down
 *    commander is still one, and a creature that merely shares its name isn't.
 *  - The gate is enforced on the cast-authorization path as well as in enumeration.
 */
class YouControlACommanderTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 40)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tagCommander(entity: EntityId, owner: EntityId) =
        replaceState(state.updateEntity(entity) { it.with(CommanderComponent(ownerId = owner)) })

    fun freeCastOffered(d: GameTestDriver, player: EntityId, cardId: EntityId): Boolean =
        d.legalActions(player).any { legal: LegalAction ->
            val action = legal.action
            action is CastSpell && action.cardId == cardId && action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    fun freeCast(cardId: EntityId, player: EntityId, targets: List<ChosenTarget>) = CastSpell(
        playerId = player,
        cardId = cardId,
        targets = targets,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE
    )

    test("no commander on the battlefield: the free cast is neither offered nor authorized") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        val rollick = d.putCardInHand(me, "Deadly Rollick")

        freeCastOffered(d, me, rollick) shouldBe false
        d.submit(freeCast(rollick, me, listOf(ChosenTarget.Permanent(victim)))).error shouldBe
            "Alternative cost is not available: ${d.cardRegistry.getCard("Deadly Rollick")!!.script.selfAlternativeCost!!.condition!!.description}"
    }

    test("your commander on the battlefield: Deadly Rollick is cast for free and exiles its target") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.tagCommander(d.putCreatureOnBattlefield(me, "Savannah Lions"), me)
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        val rollick = d.putCardInHand(me, "Deadly Rollick")

        freeCastOffered(d, me, rollick) shouldBe true
        d.submit(freeCast(rollick, me, listOf(ChosenTarget.Permanent(victim)))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.getZone(opp, Zone.EXILE) shouldContain victim
    }

    test("Fierce Guardianship is cast for free and counters a noncreature spell") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.tagCommander(d.putCreatureOnBattlefield(me, "Savannah Lions"), me)
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        val guardianship = d.putCardInHand(me, "Fierce Guardianship")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, targets = listOf(opp)).error shouldBe null

        d.submit(freeCast(guardianship, me, listOf(ChosenTarget.Spell(bolt)))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getGraveyard(me) shouldContain bolt
        d.getLifeTotal(opp) shouldBe 40
    }

    test("an opponent's commander that you control counts — whose commander doesn't matter") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val theirs = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        d.tagCommander(theirs, opp)
        d.replaceState(d.state.updateEntity(theirs) { it.with(ControllerComponent(me)) })
        val rollick = d.putCardInHand(me, "Deadly Rollick")

        freeCastOffered(d, me, rollick) shouldBe true
    }

    test("your commander controlled by an opponent doesn't count for you") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val mine = d.putCreatureOnBattlefield(me, "Savannah Lions")
        d.tagCommander(mine, me)
        d.replaceState(d.state.updateEntity(mine) { it.with(ControllerComponent(opp)) })
        val rollick = d.putCardInHand(me, "Deadly Rollick")

        freeCastOffered(d, me, rollick) shouldBe false
    }

    test("a commander off the battlefield doesn't count (CR 903.3d)") {
        val d = driver()
        val me = d.activePlayer!!
        d.tagCommander(d.putCardInCommandZone(me, "Savannah Lions"), me)
        d.tagCommander(d.putCardInGraveyard(me, "Centaur Courser"), me)
        val rollick = d.putCardInHand(me, "Deadly Rollick")

        freeCastOffered(d, me, rollick) shouldBe false
    }

    test("the designation is the card's, not its characteristics: face-down counts, a namesake doesn't") {
        val d = driver()
        val me = d.activePlayer!!
        // A creature that shares the commander's name but isn't the commander.
        d.putCreatureOnBattlefield(me, "Savannah Lions")
        val rollick = d.putCardInHand(me, "Deadly Rollick")
        freeCastOffered(d, me, rollick) shouldBe false

        // The commander itself, face down, is still a commander.
        val faceDown = d.putCreatureOnBattlefield(me, "Centaur Courser")
        d.tagCommander(faceDown, me)
        d.replaceState(d.state.updateEntity(faceDown) { it.with(FaceDownComponent) })
        freeCastOffered(d, me, rollick) shouldBe true
    }
})
