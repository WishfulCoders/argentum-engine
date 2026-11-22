package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TokenComponent
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
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Chaotic Transformation: every target is exiled at once, then each exiled permanent's controller
 * (as it last existed on the battlefield) reveals until a card sharing a card type with it, puts
 * that card onto the battlefield, and shuffles.
 */
class ChaoticTransformationScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Targets by slot: artifact, creature, enchantment, planeswalker, land. */
    fun GameTestDriver.castChaotic(
        artifact: EntityId? = null,
        creature: EntityId? = null,
        land: EntityId? = null,
    ): List<ExecutionResult> {
        val me = activePlayer!!
        val spell = putCardInHand(me, "Chaotic Transformation")
        giveMana(me, Color.RED, 1); giveColorlessMana(me, 5)
        val slots = listOf(artifact, creature, null, null, land)
        val cast = submit(CastSpell(
            playerId = me,
            cardId = spell,
            targets = slots.filterNotNull().map { ChosenTarget.Permanent(it) },
            paymentStrategy = PaymentStrategy.FromPool,
            targetGroupCounts = slots.map { if (it == null) 0 else 1 },
        ))
        cast.outcome shouldBe Outcome.Done
        val resolved = bothPass()
        stackSize shouldBe 0
        return listOf(cast, resolved)
    }

    fun GameTestDriver.battlefieldOf(player: EntityId) = state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
    fun GameTestDriver.libraryOf(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))

    test("each exiled permanent's controller gets a card sharing its type, under their own control") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theirCreature = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val myLand = d.putLandOnBattlefield(me, "Forest")
        // My top card is a creature: the land I lose must skip it for a land.
        val myTopCreature = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val theirTopCreature = d.putCardOnTopOfLibrary(opp, "Ornithopter")
        val myLibrarySize = d.libraryOf(me).size

        d.castChaotic(creature = theirCreature, land = myLand)

        d.getExile(opp) shouldContain theirCreature
        d.getExile(me) shouldContain myLand
        d.battlefieldOf(opp) shouldContain theirTopCreature
        d.getController(theirTopCreature) shouldBe opp
        d.libraryOf(me) shouldContain myTopCreature
        d.battlefieldOf(me).count { d.getCardName(it) == "Forest" } shouldBe 1
        d.battlefieldOf(me) shouldNotContain myLand
        d.libraryOf(me).size shouldBe myLibrarySize - 1
    }

    test("an animated land exiled as the land target brings back a creature — it last existed as an artifact creature land") {
        val d = driver(); val me = d.activePlayer!!
        val factory = d.putLandOnBattlefield(me, "Mishra's Factory")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, factory, TestCards.all.first { it.name == "Mishra's Factory" }.activatedAbilities[1].id))
        d.bothPass()
        d.state.projectedState.isCreature(factory) shouldBe true
        val myTopCreature = d.putCardOnTopOfLibrary(me, "Grizzly Bears")

        d.castChaotic(land = factory)

        d.getExile(me) shouldContain factory
        // Its card in exile is only a Land, which would have skipped the Bears for a Forest.
        d.battlefieldOf(me) shouldContain myTopCreature
        d.battlefieldOf(me).none { d.getCardName(it) == "Forest" } shouldBe true
    }

    test("a stolen creature is answered from the stealer's library; its owner's library is untouched") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theirCreature = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val treason = d.putCardInHand(me, "Act of Treason")
        d.giveMana(me, Color.RED, 1); d.giveColorlessMana(me, 2)
        d.castSpell(me, treason, listOf(theirCreature)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getController(theirCreature) shouldBe me
        val myTopCreature = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.putCardOnTopOfLibrary(opp, "Ornithopter")
        val theirLibrary = d.libraryOf(opp).toList()
        val theirBattlefield = d.battlefieldOf(opp).toSet() - theirCreature

        d.castChaotic(creature = theirCreature)

        d.getExile(opp) shouldContain theirCreature
        d.battlefieldOf(me) shouldContain myTopCreature
        d.libraryOf(opp) shouldBe theirLibrary
        d.battlefieldOf(opp).toSet() shouldBe theirBattlefield
    }

    test("an artifact creature chosen for both the artifact and the creature target is exiled and answered once") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val ornithopter = d.putPermanentOnBattlefield(opp, "Ornithopter")
        val first = d.putCardOnTopOfLibrary(opp, "Grizzly Bears")
        val second = d.putCardOnTopOfLibrary(opp, "Grizzly Bears")
        val before = d.battlefieldOf(opp).toSet() - ornithopter

        d.castChaotic(artifact = ornithopter, creature = ornithopter)

        d.getExile(opp) shouldContain ornithopter
        val added = d.battlefieldOf(opp).toSet() - before
        added.size shouldBe 1
        (added.single() == first || added.single() == second) shouldBe true
    }

    test("an exiled token still finds a card that shares its type") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val token = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(token, TokenComponent)
        val theirTop = d.putCardOnTopOfLibrary(opp, "Ornithopter")

        d.castChaotic(creature = token)

        d.battlefieldOf(opp) shouldContain theirTop
        d.battlefieldOf(opp) shouldNotContain token
    }

    test("with no matching card the whole library is revealed and shuffled, and nothing is put onto the battlefield") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theirCreature = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val theirLibrary = d.libraryOf(opp).toSet()
        val theirBattlefield = d.battlefieldOf(opp).toSet() - theirCreature

        val (_, resolved) = d.castChaotic(creature = theirCreature)

        d.getExile(opp) shouldContain theirCreature
        d.battlefieldOf(opp).toSet() shouldBe theirBattlefield
        d.libraryOf(opp).toSet() shouldBe theirLibrary
        resolved.events.filterIsInstance<CardsRevealedEvent>()
            .any { it.cardIds.toSet() == theirLibrary } shouldBe true
    }
})
