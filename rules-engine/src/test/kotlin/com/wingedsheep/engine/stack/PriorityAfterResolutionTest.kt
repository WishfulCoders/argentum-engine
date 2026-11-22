package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.Mode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Who receives priority once a spell or ability has resolved.
 *
 * CR 117.3b: after a spell or ability resolves, the active player receives priority — not the
 * resolved object's controller, and not whichever player answered the resolution's last question.
 * CR 117.3c is the contrast: after *casting* a spell or *activating* an ability, that player
 * receives priority, so an announcement's own decisions leave priority with the caster.
 */
class PriorityAfterResolutionTest : FunSpec({

    val opponentsDiscard = card("Priority Probe Opponents Discard") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Each opponent discards two cards."
        spell { effect = Effects.EachOpponentDiscards(2) }
    }
    val everyoneDiscards = card("Priority Probe Everyone Discards") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Each player discards a card."
        spell { effect = Effects.EachPlayerDiscards(1) }
    }
    val sorcery = card("Priority Probe Sorcery") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val instant = card("Priority Probe Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val modalInstant = card("Priority Probe Modal Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Choose one —\n• You gain 1 life.\n• You gain 2 life."
        spell {
            effect = Effects.Modal(listOf(
                Mode(effect = Effects.GainLife(1), description = "You gain 1 life."),
                Mode(effect = Effects.GainLife(2), description = "You gain 2 life."),
            ))
        }
    }
    val tapper = card("Priority Probe Tapper") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{T}: You gain 1 life."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.GainLife(1)
        }
    }
    val watcher = card("Priority Probe Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player casts a spell, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.casts()
            effect = Effects.GainLife(1)
        }
    }
    val probes = listOf(opponentsDiscard, everyoneDiscards, sorcery, instant, modalInstant, tapper, watcher)

    fun twoPlayerMain(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + probes)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(player: EntityId, cardName: String) {
        castSpell(player, putCardInHand(player, cardName)).error shouldBe null
    }

    /** Answer every discard prompt with the first cards offered, returning who was asked, in order. */
    fun GameTestDriver.answerDiscards(): List<EntityId> {
        val asked = mutableListOf<EntityId>()
        while (true) {
            val decision = pendingDecision as? SelectCardsDecision ?: return asked
            asked += decision.playerId
            submitDecision(decision.playerId, CardsSelectedResponse(decision.id, decision.options.take(decision.minSelections)))
                .error shouldBe null
        }
    }

    test("a resolution finished by the nonactive player's decision gives the active player priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.cast(active, opponentsDiscard.name)
        driver.bothPass()
        driver.answerDiscards() shouldBe listOf(opponent)

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 0
        driver.priorityPlayer shouldBe active
        // The active player can act again at once, without a pass to move the baton back.
        driver.cast(active, sorcery.name)
    }

    test("the nonactive player's instant resolving on the active player's turn gives the active player priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.cast(active, sorcery.name)
        driver.passPriority(active)
        driver.cast(opponent, instant.name)
        driver.bothPass()

        driver.stackSize shouldBe 1
        driver.priorityPlayer shouldBe active
    }

    test("the nonactive player's activated ability resolving gives the active player priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val artifact = driver.putPermanentOnBattlefield(opponent, tapper.name)

        driver.cast(active, sorcery.name)
        driver.passPriority(active)
        driver.submit(ActivateAbility(opponent, artifact, tapper.activatedAbilities.single().id))
            .outcome shouldBe Outcome.Done
        driver.priorityPlayer shouldBe opponent // CR 117.3c: the activator receives priority
        driver.bothPass()

        driver.stackSize shouldBe 1
        driver.priorityPlayer shouldBe active
    }

    test("the nonactive player's triggered ability resolving gives the active player priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(opponent, watcher.name)
        val lifeBefore = driver.getLifeTotal(opponent)

        driver.cast(active, sorcery.name)
        driver.stackSize shouldBe 2
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe lifeBefore + 1
        driver.stackSize shouldBe 1
        driver.priorityPlayer shouldBe active
    }

    test("a multiplayer resolution with several decision makers gives the active player priority") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + probes)
        val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        active shouldBe players[0]

        driver.cast(active, everyoneDiscards.name)
        driver.bothPass()
        while (driver.stackSize > 0 && driver.pendingDecision == null) driver.passPriority(driver.priorityPlayer!!)
        driver.answerDiscards() shouldBe players

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 0
        driver.priorityPlayer shouldBe active
    }

    test("a decision made while casting leaves the caster with priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.cast(active, sorcery.name)
        driver.passPriority(active)
        driver.cast(opponent, modalInstant.name)
        val mode = driver.pendingDecision as ChooseOptionDecision
        mode.playerId shouldBe opponent
        driver.submitDecision(opponent, OptionChosenResponse(mode.id, 1)).error shouldBe null

        driver.stackSize shouldBe 2
        driver.priorityPlayer shouldBe opponent
    }
})
