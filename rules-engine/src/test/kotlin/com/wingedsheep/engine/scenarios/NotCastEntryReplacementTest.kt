package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.ZoneChangeEvent(notCast = true)` on a battlefield-entry `RedirectZoneChange` —
 * the Containment Priest shape, "if a nontoken creature would enter and it wasn't cast, exile it
 * instead":
 *
 * - CR 601.2i / 608.3: a creature spell that was cast and resolves is untouched; a creature put
 *   onto the battlefield by an effect is exiled; a token is not a nontoken creature.
 * - CR 305.1 / 614.6: a creature land *played* (Dryad Arbor) wasn't cast — it is exiled, and the
 *   land play is still used.
 * - CR 614.12: creature-ness is judged as the permanent would exist on the battlefield — a face-down
 *   manifested card is a 2/2 creature (CR 708.2), an artifact under March of the Machines is a
 *   creature, a creature card with "isn't a creature" is not.
 * - The replacement needs its source on the battlefield: the warden doesn't exile itself.
 */
class NotCastEntryReplacementTest : FunSpec({

    val warden = card("Test Entry Warden") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human Cleric"
        oracleText = "If a nontoken creature would enter and it wasn't cast, exile it instead."
        power = 2
        toughness = 2
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.nontoken(),
                    to = Zone.BATTLEFIELD,
                    notCast = true,
                ),
            )
        )
    }

    val reanimate = card("Test Return to Play") {
        manaCost = "{B}"
        colorIdentity = "B"
        typeLine = "Sorcery"
        oracleText = "Put target card from your graveyard onto the battlefield."
        spell {
            val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD), optional = false)
            effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        }
    }

    val manifester = card("Test Manifester") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Sorcery"
        oracleText = "Manifest the top card of your library."
        spell { effect = Patterns.Library.manifest() }
    }

    val shy = card("Test Shy Creature") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Creature — Golem"
        oracleText = "This creature isn't a creature."
        power = 2
        toughness = 2
        staticAbility { ability = RemoveCardType("CREATURE") }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(warden, reanimate, manifester, shy))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun reanimate(driver: GameTestDriver, me: EntityId, cardName: String): EntityId {
        val target = driver.putCardInGraveyard(me, cardName)
        val spell = driver.putCardInHand(me, "Test Return to Play")
        driver.giveMana(me, Color.BLACK, 1)
        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, me, Zone.GRAVEYARD))).error shouldBe null
        resolveStack(driver)
        return target
    }

    fun onBattlefield(driver: GameTestDriver, me: EntityId, id: EntityId) =
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(id)

    fun inExile(driver: GameTestDriver, me: EntityId, id: EntityId) =
        driver.state.getZone(me, Zone.EXILE).contains(id)

    test("a creature put onto the battlefield by an effect is exiled instead") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val bears = reanimate(driver, me, "Grizzly Bears")
        inExile(driver, me, bears) shouldBe true
        onBattlefield(driver, me, bears) shouldBe false
    }

    test("a creature spell that was cast enters normally, and so do tokens") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).error shouldBe null
        resolveStack(driver)
        onBattlefield(driver, me, bears) shouldBe true

        val alarm = driver.putCardInHand(me, "Raise the Alarm")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, alarm).error shouldBe null
        resolveStack(driver)
        driver.state.getZone(me, Zone.BATTLEFIELD).count { driver.getCardName(it) == "Soldier Token" } shouldBe 2
    }

    test("CR 305.1: a played creature land wasn't cast — exiled, and the land play is spent") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val arbor = driver.putCardInHand(me, "Dryad Arbor")
        driver.playLand(me, arbor).error shouldBe null
        inExile(driver, me, arbor) shouldBe true
        onBattlefield(driver, me, arbor) shouldBe false
        driver.state.getEntity(me)!!.get<LandDropsComponent>()!!.remaining shouldBe 0
        // An ordinary land is still played normally.
        val other = newDriver()
        val p = other.activePlayer!!
        other.putPermanentOnBattlefield(p, "Test Entry Warden")
        val plains = other.putCardInHand(p, "Plains")
        other.playLand(p, plains).error shouldBe null
        onBattlefield(other, p, plains) shouldBe true
    }

    test("CR 614.12 / 708.2: a manifested noncreature card enters as a 2/2 creature — exiled") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val ring = driver.putCardOnTopOfLibrary(me, "Sol Ring")
        val spell = driver.putCardInHand(me, "Test Manifester")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, spell).error shouldBe null
        resolveStack(driver)
        inExile(driver, me, ring) shouldBe true
        onBattlefield(driver, me, ring) shouldBe false
    }

    test("CR 614.12: an artifact entering as a creature (March of the Machines) is exiled; without March it isn't") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val plain = reanimate(driver, me, "Sol Ring")
        onBattlefield(driver, me, plain) shouldBe true

        driver.putPermanentOnBattlefield(me, "March of the Machines")
        val animated = reanimate(driver, me, "Sol Ring")
        inExile(driver, me, animated) shouldBe true
    }

    test("CR 614.12: a creature card that isn't a creature on the battlefield is not exiled") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Entry Warden")
        val golem = reanimate(driver, me, "Test Shy Creature")
        onBattlefield(driver, me, golem) shouldBe true
    }

    test("the replacement needs its source on the battlefield: the warden itself enters") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val warden = reanimate(driver, me, "Test Entry Warden")
        onBattlefield(driver, me, warden) shouldBe true
    }
})
