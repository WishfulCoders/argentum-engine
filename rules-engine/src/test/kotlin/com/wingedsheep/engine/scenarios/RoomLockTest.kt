package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DoorLockedEvent
import com.wingedsheep.engine.core.DoorUnlockedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.RoomFullyUnlockedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Feature tests for the resolution-time "lock a door" effect (CR 709.5g) and the "lock or unlock a
 * door" modal choice (`Effects.LockOrUnlockDoor`), driven through small test artifacts. The unlock
 * side and the unlock special action are covered by [RoomUnlockTest]; this file proves the new lock
 * primitive, the player door-choice when more than one door is eligible (CR 709.5f/g), and that
 * locking is correctly *not* a trigger source.
 */
class RoomLockTest : FunSpec({

    // A Room whose left face has an end-step draw trigger (observable when locked vs. unlocked) and
    // whose right face fires a draw on unlock (to prove the modal-unlock path fires CR 709.5h).
    val triggerRoom = card("Trigger Hall // Trigger Vault") {
        layout = CardLayout.SPLIT
        face("Trigger Hall") {
            manaCost = "{2}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "At the beginning of your end step, draw a card."
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                effect = Effects.DrawCards(1)
            }
        }
        face("Trigger Vault") {
            manaCost = "{3}{B}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "When you unlock this door, draw a card."
            triggeredAbility {
                trigger = Triggers.self.doorUnlocked()
                effect = Effects.DrawCards(1)
            }
        }
    }

    // A Room with two plain faces — used where door triggers would only add noise (the door-choice
    // and no-op cases).
    val plainRoom = card("Plain Parlor // Plain Cellar") {
        layout = CardLayout.SPLIT
        face("Plain Parlor") {
            manaCost = "{1}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "Plain."
        }
        face("Plain Cellar") {
            manaCost = "{2}{B}"
            typeLine = "Enchantment — Room"
            oracleText = "Plain."
        }
    }

    fun roomYouControl() = TargetObject(
        filter = TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.ROOM).youControl())
    )

    // "{T}: Lock a door of target Room you control."
    val lockKey = card("Test Lock Key") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            target(roomYouControl())
            effect = Effects.LockDoor(EffectTarget.ContextTarget(0))
        }
    }

    // "{T}: Lock or unlock a door of target Room you control."
    val skeletonKey = card("Test Skeleton Key") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            target(roomYouControl())
            effect = Effects.LockOrUnlockDoor(EffectTarget.ContextTarget(0))
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(triggerRoom)
        d.registerCard(plainRoom)
        d.registerCard(lockKey)
        d.registerCard(skeletonKey)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 25, "Grizzly Bears" to 15), skipMulligans = true)
        return d
    }

    fun GameTestDriver.room(id: com.wingedsheep.sdk.model.EntityId): RoomComponent =
        state.getEntity(id)?.get<RoomComponent>() ?: error("not a Room")

    test("locking the only unlocked door needs no choice, emits a DoorLockedEvent, and suppresses that half") {
        val d = driver()
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Trigger Hall (left face) → only Trigger Hall unlocked, its end-step draw active.
        val roomId = d.putCardInHand(p, triggerRoom.name)
        d.giveMana(p, Color.BLACK, 3)
        d.submitSuccess(CastSpell(p, roomId, faceIndex = 0))
        d.bothPass()
        d.room(roomId).unlocked shouldBe setOf(RoomFaceId("Trigger Hall"))

        val key = d.putPermanentOnBattlefield(p, lockKey.name)
        val abilityId = lockKey.activatedAbilities.first().id
        d.submitSuccess(
            ActivateAbility(
                playerId = p,
                sourceId = key,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(d.state, roomId)),
            )
        )
        // Only one unlocked door → no door-choice prompt; resolving locks it directly.
        val res = d.bothPass()
        d.isPaused shouldBe false

        d.room(roomId).unlocked.shouldBeEmpty()
        val lockEvents = res.events.filterIsInstance<DoorLockedEvent>()
        lockEvents shouldHaveSize 1
        lockEvents.single().faceId shouldBe RoomFaceId("Trigger Hall")
        // Locking is not an unlock and never fully-unlocks — no unlock-family events.
        res.events.filterIsInstance<DoorUnlockedEvent>().shouldBeEmpty()
        res.events.filterIsInstance<RoomFullyUnlockedEvent>().shouldBeEmpty()

        // Trigger Hall is now locked → its end-step draw is suppressed (CR 709.5).
        val handBefore = d.getHand(p).size
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.getHand(p).size shouldBe handBefore
    }

    test("locking with two unlocked doors prompts the controller for which door (CR 709.5g)") {
        val d = driver()
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Plain Parlor, then unlock Plain Cellar via the special action → both unlocked.
        val roomId = d.putCardInHand(p, plainRoom.name)
        d.giveMana(p, Color.BLACK, 2)
        d.submitSuccess(CastSpell(p, roomId, faceIndex = 0))
        d.bothPass()
        d.giveMana(p, Color.BLACK, 3)
        d.submitSuccess(UnlockRoomDoor(p, roomId, RoomFaceId("Plain Cellar")))
        d.bothPass()
        d.room(roomId).isFullyUnlocked shouldBe true

        val key = d.putPermanentOnBattlefield(p, lockKey.name)
        d.submitSuccess(
            ActivateAbility(
                playerId = p,
                sourceId = key,
                abilityId = lockKey.activatedAbilities.first().id,
                targets = listOf(entityIdToChosenTarget(d.state, roomId)),
            )
        )
        d.bothPass()

        // Two eligible doors → the controller is asked which to lock.
        val choice = d.pendingDecision as ChooseOptionDecision
        choice.options shouldContainExactly listOf("Plain Parlor", "Plain Cellar")
        d.submitDecision(p, OptionChosenResponse(choice.id, 1)) // lock Plain Cellar

        d.room(roomId).unlocked shouldBe setOf(RoomFaceId("Plain Parlor"))
    }

    test("locking a Room with no unlocked door is a harmless no-op") {
        val d = driver()
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Plain Parlor (Parlor unlocked, Cellar locked), then lock Parlor → both locked.
        val roomId = d.putCardInHand(p, plainRoom.name)
        d.giveMana(p, Color.BLACK, 2)
        d.submitSuccess(CastSpell(p, roomId, faceIndex = 0))
        d.bothPass()

        val key = d.putPermanentOnBattlefield(p, lockKey.name)
        val abilityId = lockKey.activatedAbilities.first().id
        d.submitSuccess(
            ActivateAbility(p, key, abilityId, targets = listOf(entityIdToChosenTarget(d.state, roomId)))
        )
        d.bothPass()
        d.room(roomId).unlocked.shouldBeEmpty()

        // A second key: now there is no unlocked door to lock.
        val key2 = d.putPermanentOnBattlefield(p, lockKey.name)
        d.submitSuccess(
            ActivateAbility(p, key2, abilityId, targets = listOf(entityIdToChosenTarget(d.state, roomId)))
        )
        val res = d.bothPass()
        d.isPaused shouldBe false
        res.events.filterIsInstance<DoorLockedEvent>().shouldBeEmpty()
        d.room(roomId).unlocked.shouldBeEmpty()
    }

    test("'lock or unlock' modal: choosing unlock unlocks a locked door and fires its trigger (CR 709.5h)") {
        val d = driver()
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Trigger Hall → Trigger Vault is locked; its "when you unlock, draw" is dormant.
        val roomId = d.putCardInHand(p, triggerRoom.name)
        d.giveMana(p, Color.BLACK, 3)
        d.submitSuccess(CastSpell(p, roomId, faceIndex = 0))
        d.bothPass()

        val key = d.putPermanentOnBattlefield(p, skeletonKey.name)
        d.submitSuccess(
            ActivateAbility(
                p, key, skeletonKey.activatedAbilities.first().id,
                targets = listOf(entityIdToChosenTarget(d.state, roomId)),
            )
        )
        d.bothPass()

        // Resolution presents the lock/unlock mode choice.
        val mode = d.pendingDecision as ChooseOptionDecision
        mode.options shouldContainExactly listOf("Lock a door", "Unlock a door")
        val handBefore = d.getHand(p).size
        d.submitDecision(p, OptionChosenResponse(mode.id, 1)) // unlock

        // Trigger Vault is the only locked door → unlocked directly; its OnDoorUnlocked trigger fires.
        d.room(roomId).unlocked shouldBe setOf(RoomFaceId("Trigger Hall"), RoomFaceId("Trigger Vault"))
        d.bothPass() // resolve the draw trigger
        d.getHand(p).size shouldBe handBefore + 1
    }

    test("'lock or unlock' modal: choosing lock locks an unlocked door and fires no trigger") {
        val d = driver()
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val roomId = d.putCardInHand(p, triggerRoom.name)
        d.giveMana(p, Color.BLACK, 3)
        d.submitSuccess(CastSpell(p, roomId, faceIndex = 0))
        d.bothPass()

        val key = d.putPermanentOnBattlefield(p, skeletonKey.name)
        d.submitSuccess(
            ActivateAbility(
                p, key, skeletonKey.activatedAbilities.first().id,
                targets = listOf(entityIdToChosenTarget(d.state, roomId)),
            )
        )
        d.bothPass()

        val mode = d.pendingDecision as ChooseOptionDecision
        d.submitDecision(p, OptionChosenResponse(mode.id, 0)) // lock

        // Trigger Hall (the only unlocked door) is locked; nothing is fully unlocked, no draw.
        d.room(roomId).unlocked.shouldBeEmpty()
        d.isPaused shouldBe false
    }
})
