package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MayCastFromGraveyard(playLands = true, gainsAbility = …)` — the "play a land or cast a spell
 * from your graveyard" grant and its "if you do, it gains '…'" rider, proven on an inline granter
 * shaped like Serra Paragon:
 *
 * - CR 305.1: a land matching the filter is *played* from the graveyard (special action, land drop
 *   used); CR 305.9: it is never offered as a cast.
 * - One `oncePerTurn` allowance is shared by the land play and the spell cast: after either, the
 *   other is no longer offered, and the allowance returns next turn. `duringYourTurnOnly` holds.
 * - The filter gates the cast half (mana value 3 or less).
 * - CR 400.7h / 400.7i / 400.7b: the spell cast (or land played) this way gains the triggered
 *   ability, the permanent keeps it — even after the granter leaves — and it fires when that
 *   permanent is put into a graveyard from the battlefield.
 */
class GraveyardPlayGrantScenarioTest : FunSpec({

    val granter = card("Test Graveyard Paragon") {
        manaCost = "{2}{W}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Angel"
        oracleText = "Once during each of your turns, you may play a land from your graveyard or cast a " +
            "permanent spell with mana value 3 or less from your graveyard. If you do, it gains \"When " +
            "this permanent is put into a graveyard from the battlefield, exile it and you gain 2 life.\""
        power = 3
        toughness = 4
        staticAbility {
            ability = MayCastFromGraveyard(
                filter = GameObjectFilter.Permanent.manaValueAtMost(3),
                duringYourTurnOnly = true,
                oncePerTurn = true,
                playLands = true,
                gainsAbility = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Exile(EffectTarget.Self) then Effects.GainLife(2),
                ),
            )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(granter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun graveyardActions(driver: GameTestDriver, player: EntityId, card: EntityId): List<LegalAction> =
        driver.legalActions(player).filter { la ->
            when (val a = la.action) {
                is PlayLand -> a.cardId == card
                is CastSpell -> a.cardId == card
                else -> false
            }
        }

    fun toOwnNextMain(driver: GameTestDriver) {
        repeat(2) {
            driver.passPriorityUntil(Step.END, maxPasses = 300)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        }
    }

    test("CR 305.1/305.9: a graveyard land is offered as a land play, never as a cast, and spends the shared allowance") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Graveyard Paragon")
        val plains = driver.putCardInGraveyard(me, "Plains")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")

        val offers = graveyardActions(driver, me, plains)
        offers.map { it.action::class } shouldBe listOf(PlayLand::class)
        graveyardActions(driver, me, bears).map { it.action::class } shouldBe listOf(CastSpell::class)

        driver.playLand(me, plains).error shouldBe null
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(plains) shouldBe true

        // The single allowance is spent: the creature card is no longer castable from the graveyard.
        graveyardActions(driver, me, bears) shouldBe emptyList()
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).error shouldNotBe null
    }

    test("casting a permanent spell spends the allowance for the land too; the filter gates the cast; it renews next turn") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Graveyard Paragon")
        val plains = driver.putCardInGraveyard(me, "Plains")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val giant = driver.putCardInGraveyard(me, "Hill Giant")

        withClue("mana value 4 is outside the filter") {
            graveyardActions(driver, me, giant) shouldBe emptyList()
        }

        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).error shouldBe null
        resolveStack(driver)
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(bears) shouldBe true

        graveyardActions(driver, me, plains) shouldBe emptyList()
        driver.playLand(me, plains).error shouldNotBe null

        // Not on the opponent's turn (during your turn only) …
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        graveyardActions(driver, me, plains) shouldBe emptyList()
        // … and back on the next one of yours.
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        driver.activePlayer shouldBe me
        graveyardActions(driver, me, plains).map { it.action::class } shouldBe listOf(PlayLand::class)
    }

    test("CR 400.7h/400.7b: the permanent cast this way keeps the gained ability after the granter leaves, and it fires on death") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val paragon = driver.putPermanentOnBattlefield(me, "Test Graveyard Paragon")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).error shouldBe null
        resolveStack(driver)

        // The granter leaves; the gained ability stays with the Bears.
        driver.moveToGraveyard(paragon)
        val life = driver.getLifeTotal(me)
        val doom = driver.putCardInHand(me, "Doom Blade")
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, doom, targets = listOf(bears)).error shouldBe null
        resolveStack(driver)

        driver.getExileCardNames(me).contains("Grizzly Bears") shouldBe true
        driver.getGraveyardCardNames(me).contains("Grizzly Bears") shouldBe false
        driver.getLifeTotal(me) shouldBe life + 2
    }

    test("CR 400.7i: a land played this way gains the ability too; a land played from hand does not") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Graveyard Paragon")
        val plains = driver.putCardInGraveyard(me, "Plains")
        driver.playLand(me, plains).error shouldBe null
        val handIsland = driver.putLandOnBattlefield(me, "Island")

        val life = driver.getLifeTotal(me)
        for (land in listOf(handIsland, plains)) {
            val rain = driver.putCardInHand(me, "Stone Rain")
            driver.giveMana(me, Color.RED, 3)
            driver.castSpell(me, rain, targets = listOf(land)).error shouldBe null
            resolveStack(driver)
        }

        driver.getExileCardNames(me) shouldBe listOf("Plains")
        driver.getGraveyardCardNames(me).contains("Island") shouldBe true
        driver.getLifeTotal(me) shouldBe life + 2
    }

    test("a spell cast this way that is countered drops the gained ability with the stack object") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Graveyard Paragon")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).error shouldBe null
        driver.state.grantedTriggeredAbilities.count { it.entityId == bears } shouldBe 1

        val opponent = driver.getOpponent(me)
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.passPriority(me)
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.castSpellWithTargets(opponent, counter, listOf(ChosenTarget.Spell(bears))).error shouldBe null
        resolveStack(driver)

        driver.getGraveyardCardNames(me).contains("Grizzly Bears") shouldBe true
        driver.state.grantedTriggeredAbilities.none { it.entityId == bears } shouldBe true
    }
})
