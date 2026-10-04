package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.FableOfTheMirrorBreaker
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fable of the Mirror-Breaker // Reflection of Kiki-Jiki (NEO #141).
 *
 *   I — Create a 2/2 red Goblin Shaman creature token with "Whenever this token attacks, create a
 *     Treasure token."
 *   II — You may discard up to two cards. If you do, draw that many cards.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *   Reflection of Kiki-Jiki: {1}, {T}: Create a token that's a copy of another target nonlegendary
 *   creature you control, except it has haste. Sacrifice it at the beginning of the next end step.
 *
 * Walks the whole card across real turns: chapter I's token makes a Treasure when it attacks,
 * chapter III flips the Saga into the Reflection, and the Reflection (once it can tap) copies the
 * Goblin with haste — but never itself.
 */
class FableOfTheMirrorBreakerScenarioTest : ScenarioTestBase() {

    private fun names(driver: GameTestDriver, player: EntityId): List<String> =
        driver.getPermanents(player).mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }

    private fun drain(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 60) {
            when {
                driver.pendingDecision != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }
    }

    private fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        drain(driver)
    }

    init {
        test("chapter I token makes Treasure, chapter III transforms, and the Reflection copies with haste") {
            val projector = StateProjector()
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + listOf(FableOfTheMirrorBreaker, PredefinedTokens.Treasure))
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val you = driver.activePlayer!!
            val opponent = driver.getOpponent(you)

            val spell = driver.putCardInHand(you, "Fable of the Mirror-Breaker")
            driver.giveMana(you, Color.RED, 1)
            driver.giveColorlessMana(you, 2)
            driver.castSpell(you, spell).error shouldBe null
            drain(driver)

            val goblin = driver.findPermanent(you, "Goblin Shaman Token")
            withClue("chapter I creates the Goblin Shaman token") { goblin shouldNotBe null }

            advanceToNextTurnMain(driver) // opponent's turn
            advanceToNextTurnMain(driver) // my turn: lore 2, chapter II

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(you, listOf(goblin!!), opponent).error shouldBe null
            drain(driver)
            withClue("the Goblin's attack trigger creates a Treasure") {
                names(driver, you).count { it == "Treasure" } shouldBe 1
            }

            advanceToNextTurnMain(driver) // opponent's turn
            advanceToNextTurnMain(driver) // my turn: lore 3, chapter III

            val reflection = driver.findPermanent(you, "Reflection of Kiki-Jiki")
            withClue("chapter III returns the Saga transformed") {
                reflection shouldNotBe null
                driver.findPermanent(you, "Fable of the Mirror-Breaker") shouldBe null
                val projected = projector.project(driver.state)
                projected.isCreature(reflection!!) shouldBe true
                projected.getPower(reflection) shouldBe 2
            }

            advanceToNextTurnMain(driver) // opponent's turn
            advanceToNextTurnMain(driver) // my turn: the Reflection has been under my control since my last turn

            val abilityId = FableOfTheMirrorBreaker.backFace!!.activatedAbilities.single().id
            withClue("\"another\" — the Reflection can't copy itself") {
                driver.giveColorlessMana(you, 1)
                driver.submit(
                    ActivateAbility(you, reflection!!, abilityId, targets = listOf(ChosenTarget.Permanent(reflection)))
                ).error shouldNotBe null
            }

            driver.submit(
                ActivateAbility(you, reflection!!, abilityId, targets = listOf(ChosenTarget.Permanent(goblin)))
            ).error shouldBe null
            drain(driver)

            val goblins = driver.getPermanents(you).filter {
                driver.state.getEntity(it)?.get<CardComponent>()?.name == "Goblin Shaman Token"
            }
            withClue("a hasty token copy of the Goblin joins the original") {
                goblins.size shouldBe 2
                val copy = goblins.single { it != goblin }
                driver.state.getEntity(copy)?.has<TokenComponent>() shouldBe true
                projector.project(driver.state).hasKeyword(copy, Keyword.HASTE) shouldBe true
            }
        }
    }
}
