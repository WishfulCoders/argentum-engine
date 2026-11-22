package com.wingedsheep.engine.view

import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * A disguised or cloaked permanent has ward {2} while face down (CR 702.168a, 701.58a), the one
 * ability a hidden card can put on the stack. The ward trigger used to carry the card underneath
 * onto the stack object — its name as `sourceName` and its card definition in `abilityIdentity` —
 * so the game state, and anything keyed by that identity (a persistent yield, a batched answer),
 * differed with a card the targeting player may not look at. Found by
 * `HiddenIdentityRandomGamesTest` (MKM).
 */
class FaceDownWardTriggerIdentityTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [state] with [entityId] made into [cardName] underneath — same id, owner and components otherwise. */
    fun GameTestDriver.withIdentity(state: GameState, entityId: EntityId, cardName: String): GameState {
        val def = cardRegistry.requireCard(cardName)
        return state.updateEntity(entityId) { container ->
            val card = container.get<CardComponent>()!!
            container.with(
                card.copy(
                    cardDefinitionId = def.name,
                    name = def.name,
                    manaCost = def.manaCost,
                    typeLine = def.typeLine,
                    oracleText = def.oracleText,
                    baseStats = def.creatureStats,
                    baseKeywords = def.keywords,
                    baseFlags = def.flags,
                    colors = def.colors,
                    imageUri = def.metadata.imageUri,
                )
            )
        }
    }

    test("targeting a disguised permanent puts a ward trigger on the stack that does not depend on the hidden card") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val disguiser = driver.getOpponent(caster)
        val hidden = driver.putCreatureOnBattlefield(disguiser, "Centaur Courser")
        driver.replaceState(
            driver.state.updateEntity(hidden) {
                it.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.DISGUISE))
            }
        )
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        val fork = driver.state

        fun boltIt(world: GameState): GameState {
            driver.replaceState(world)
            driver.castSpell(caster, bolt, listOf(hidden)).error.shouldBeNull()
            return driver.state
        }

        val real = boltIt(fork)
        val ward = real.stack.mapNotNull { real.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }.single()
        ward.sourceId shouldBe hidden
        ward.sourceName shouldBe FACE_DOWN_DISPLAY_NAME
        ward.abilityIdentity.shouldBeNull()

        // The whole game state, not only the fields above: the caster may not look under the
        // permanent, so the world where it is another card must play out identically.
        val swapped = boltIt(driver.withIdentity(fork, hidden, "Savannah Lions"))
        swapped shouldBe driver.withIdentity(real, hidden, "Savannah Lions")
    }
})
