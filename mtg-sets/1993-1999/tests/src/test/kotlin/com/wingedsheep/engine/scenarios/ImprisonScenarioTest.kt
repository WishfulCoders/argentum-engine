package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.FrozenShade
import com.wingedsheep.mtg.sets.definitions.lea.cards.ProdigalSorcerer
import com.wingedsheep.mtg.sets.definitions.leg.cards.Imprison
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Imprison (LEG #107) — "Whenever a player activates an ability of enchanted creature with {T} in
 * its activation cost that isn't a mana ability, you may pay {1}. If you do, counter that ability.
 * If you don't, destroy this Aura. Whenever enchanted creature attacks or blocks, you may pay {1}.
 * If you do, tap the creature, remove it from combat, and creatures it was blocking that had become
 * blocked by only that creature this combat become unblocked. If you don't, destroy this Aura."
 *
 * The {T}-cost trigger has to fire on a {T} ability and on nothing else — not a mana ability with
 * {T}, not a non-{T} ability — and "that ability" has to be the activated ability on the stack.
 * The combat trigger fires from the Aura on the enchanted creature's attack *and* its block.
 */
class ImprisonScenarioTest : FunSpec({

    val pingAbility = ProdigalSorcerer.activatedAbilities.single().id
    val shadePump = FrozenShade.activatedAbilities.single().id
    val elvesMana = TestCards.LlanowarElves.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ProdigalSorcerer + FrozenShade + Imprison)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Puts Imprison under [controller] onto the battlefield attached to [host]. */
    fun GameTestDriver.imprison(controller: EntityId, host: EntityId): EntityId {
        val aura = putPermanentOnBattlefield(controller, "Imprison")
        addComponent(aura, AttachedToComponent(host))
        val existing = state.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        addComponent(host, AttachmentsComponent(existing + aura))
        return aura
    }

    fun GameTestDriver.readyCreature(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    test("paying {1} counters a {T} ability of the enchanted creature") {
        val d = driver()
        val tim = d.readyCreature(d.player1, "Prodigal Sorcerer")
        val aura = d.imprison(d.player2, tim)
        d.giveColorlessMana(d.player2, 1)

        d.submit(
            ActivateAbility(d.player1, tim, pingAbility, targets = listOf(ChosenTarget.Player(d.player2)))
        ).outcome shouldBe Outcome.Done
        d.state.stack.size shouldBe 2
        d.bothPass()

        withClue("Imprison's controller is asked for the {1}") {
            d.pendingDecision?.playerId shouldBe d.player2
        }
        d.submitYesNo(d.player2, true)

        withClue("the ping was countered — nothing left to resolve, no damage") {
            d.state.stack.isEmpty() shouldBe true
            d.getLifeTotal(d.player2) shouldBe 20
        }
        d.state.getBattlefield() shouldContain aura
    }

    test("declining the {1} destroys Imprison and the ability resolves") {
        val d = driver()
        val tim = d.readyCreature(d.player1, "Prodigal Sorcerer")
        val aura = d.imprison(d.player2, tim)
        d.giveColorlessMana(d.player2, 1)

        d.submit(
            ActivateAbility(d.player1, tim, pingAbility, targets = listOf(ChosenTarget.Player(d.player2)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submitYesNo(d.player2, false)
        d.bothPass()

        d.getGraveyard(d.player2) shouldContain aura
        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("a {T} mana ability and an ability without {T} don't trigger it") {
        val d = driver()
        val elves = d.readyCreature(d.player1, "Llanowar Elves")
        d.imprison(d.player2, elves)
        val shade = d.readyCreature(d.player1, "Frozen Shade")
        d.imprison(d.player2, shade)

        d.submit(ActivateAbility(d.player1, elves, elvesMana)).outcome shouldBe Outcome.Done
        withClue("the mana ability is excluded") {
            d.state.stack.isEmpty() shouldBe true
            d.pendingDecision shouldBe null
        }

        d.giveMana(d.player1, Color.BLACK, 1)
        d.submit(ActivateAbility(d.player1, shade, shadePump)).outcome shouldBe Outcome.Done
        withClue("only the pump is on the stack — no {T} in its cost") {
            d.state.stack.size shouldBe 1
        }
    }

    test("enchanted creature attacks: paying {1} taps it and removes it from combat") {
        val d = driver()
        val courser = d.readyCreature(d.player1, "Centaur Courser")
        val aura = d.imprison(d.player2, courser)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2)
        d.giveColorlessMana(d.player2, 1)
        d.bothPass()
        d.pendingDecision?.playerId shouldBe d.player2
        d.submitYesNo(d.player2, true)

        withClue("no longer attacking") {
            d.state.getEntity(courser)?.has<AttackingComponent>() shouldBe false
        }
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getBattlefield() shouldContain aura
    }

    test("enchanted creature blocks: paying {1} removes it and the attacker it alone blocked is unblocked") {
        val d = driver()
        val attacker = d.readyCreature(d.player1, "Centaur Courser")
        val blocker = d.readyCreature(d.player2, "Centaur Courser")
        d.imprison(d.player1, blocker)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2)
        d.bothPass()
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker)))
        d.giveColorlessMana(d.player1, 1)
        d.bothPass()
        d.pendingDecision?.playerId shouldBe d.player1
        d.submitYesNo(d.player1, true)

        withClue("the blocker is tapped and out of combat") {
            d.isTapped(blocker) shouldBe true
        }
        d.passPriorityUntil(Step.END_COMBAT)
        withClue("the now-unblocked attacker deals its 3 to the defending player; the blocker survives") {
            d.getLifeTotal(d.player2) shouldBe 17
            d.state.getBattlefield() shouldContain blocker
        }
    }

    test("declining on attack destroys Imprison and the creature keeps attacking") {
        val d = driver()
        val courser = d.readyCreature(d.player1, "Centaur Courser")
        val aura = d.imprison(d.player2, courser)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2)
        d.giveColorlessMana(d.player2, 1)
        d.bothPass()
        d.submitYesNo(d.player2, false)

        d.getGraveyard(d.player2) shouldContain aura
        d.state.getEntity(courser)?.get<AttackingComponent>() shouldNotBe null
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 17
    }
})
