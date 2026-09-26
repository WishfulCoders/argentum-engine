package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DoorUnlockedEvent
import com.wingedsheep.engine.core.RoomFullyUnlockedEvent
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phase 3 of the Rooms mechanic (CR 709.5e/h): the door-unlock special action, the
 * face-scoped "When you unlock this door" trigger, and ability suppression of locked
 * halves. Phase 2's [RoomCastTest] already covers the cast-a-half flow; this file
 * exercises everything that lands once a Room is on the battlefield.
 */
class RoomUnlockTest : FunSpec({

    // Test fixture: a Room whose right face has a "When you unlock this door, draw a card"
    // trigger and whose left face has "At the beginning of your end step, draw a card."
    // We use Effects.DrawCards as a stand-in for any face-scoped ETB/triggered effect so
    // we can observe it via hand size deltas without a real card definition.
    val testRoom = card("Test Hall // Test Vault") {
        layout = CardLayout.SPLIT
        face("Test Hall") {
            manaCost = "{2}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "At the beginning of your end step, draw a card."
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                effect = Effects.DrawCards(1)
            }
        }
        face("Test Vault") {
            manaCost = "{3}{B}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "When you unlock this door, draw a card."
            triggeredAbility {
                trigger = Triggers.self.doorUnlocked()
                effect = Effects.DrawCards(1)
            }
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(testRoom)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Grizzly Bears" to 20,
            ),
            skipMulligans = true,
        )
        return driver
    }

    test("unlock-door action is enumerated for the locked face only") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val roomId = driver.putCardInHand(player, testRoom.name)
        driver.giveMana(player, Color.BLACK, 5)

        // Cast Test Hall (left face).
        driver.submitSuccess(CastSpell(player, roomId, faceIndex = 0))
        driver.bothPass()

        // The Room is now on the battlefield with Test Hall unlocked.
        val room = driver.state.getEntity(roomId)?.get<RoomComponent>()
        room shouldNotBe null
        room!!.unlocked shouldBe setOf(RoomFaceId("Test Hall"))

        // Give the player enough mana to unlock the right face ({3}{B}{B}).
        driver.giveMana(player, Color.BLACK, 5)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val unlockActions = enumerator.enumerate(driver.state, player)
            .mapNotNull { it.action as? UnlockRoomDoor }
            .filter { it.roomId == roomId }
        unlockActions shouldHaveSize 1
        unlockActions.single().faceId shouldBe RoomFaceId("Test Vault")
    }

    test("unlocking the second door fires its OnDoorUnlocked trigger and a RoomFullyUnlockedEvent") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val roomId = driver.putCardInHand(player, testRoom.name)
        // Five black covers Test Hall ({2}{B}).
        driver.giveMana(player, Color.BLACK, 3)
        driver.submitSuccess(CastSpell(player, roomId, faceIndex = 0))
        driver.bothPass()

        // Mana for the unlock cost ({3}{B}{B}).
        driver.giveMana(player, Color.BLACK, 5)
        val handSizeBefore = driver.getHand(player).size

        val unlockResult = driver.submitSuccess(UnlockRoomDoor(player, roomId, RoomFaceId("Test Vault")))

        // Both faces are now unlocked.
        val room = driver.state.getEntity(roomId)?.get<RoomComponent>()
        room!!.isFullyUnlocked shouldBe true

        // OnDoorUnlocked trigger from Test Vault drew the player a card. The trigger goes
        // on the stack; resolve it.
        driver.bothPass()
        val handSizeAfter = driver.getHand(player).size
        handSizeAfter shouldBe handSizeBefore + 1

        // The unlock result included a DoorUnlockedEvent with becameFullyUnlocked = true,
        // and a RoomFullyUnlockedEvent for Eerie matching.
        val doorEvents = unlockResult.events.filterIsInstance<DoorUnlockedEvent>()
        doorEvents shouldHaveSize 1
        doorEvents.single().faceId shouldBe RoomFaceId("Test Vault")
        doorEvents.single().becameFullyUnlocked shouldBe true
        unlockResult.events.filterIsInstance<RoomFullyUnlockedEvent>() shouldHaveSize 1
    }

    test("locked face's triggered ability is suppressed; unlocking it activates the trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val roomId = driver.putCardInHand(player, testRoom.name)
        driver.giveMana(player, Color.BLACK, 5)
        // Cast Test Vault (right face); the left face's "draw on end step" trigger should
        // be suppressed while Test Hall is locked.
        driver.submitSuccess(CastSpell(player, roomId, faceIndex = 1))
        driver.bothPass()

        // Resolve any OnDoorUnlocked trigger from Test Vault entering. (Test Vault's
        // trigger draws a card.)
        driver.bothPass()

        val room = driver.state.getEntity(roomId)?.get<RoomComponent>()
        room!!.unlocked shouldContainExactly setOf(RoomFaceId("Test Vault"))

        val handBeforeEndStep = driver.getHand(player).size

        // Pass through to end step. Test Hall is still locked; its end-step trigger
        // should NOT fire. So no extra cards are drawn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val handAfterEndStep = driver.getHand(player).size
        // Only the normal beginning-of-turn draw counts; no Test Hall trigger fired
        // (locked half's abilities are suppressed per CR 709.5).
        handAfterEndStep shouldBe handBeforeEndStep
    }
})
