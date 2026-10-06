package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/** "Each opponent can cast spells only any time they could cast a sorcery." — Teferi, Time Raveler. */
private val SorceryLock = card("Test Sorcery Lock") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Each opponent can cast spells only any time they could cast a sorcery."
    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            condition = Conditions.Not(Conditions.CouldCastSorcery()),
            conditionFromCaster = true,
        )
    }
    metadata { rarity = Rarity.RARE; collectorNumber = "T1" }
}

/** Borne Upon a Wind's permission: every spell its caster casts this turn has flash. */
private val FlashForAll = card("Test Flash For All") {
    manaCost = "{0}"
    typeLine = "Sorcery"
    oracleText = "You may cast spells this turn as though they had flash."
    spell { effect = Effects.GrantFlashToSpells(spellFilter = GameObjectFilter.Any) }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T2" }
}

/**
 * `PlayersCantCastSpells` gated on `Not(PlayerCouldCastSorcery)` read from the caster's seat — the
 * CR 307.1 sorcery timing (a main phase of your turn, stack empty) imposed on every spell an
 * opponent casts. Each test pins one clause, plus the two Teferi, Time Raveler rulings this shape
 * has to honour (a flash permission doesn't override it; a spell cast during a resolution is never
 * at sorcery timing, CR 608.2).
 */
class CastOnlyAtSorceryTimingTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(SorceryLock, FlashForAll))

        fun castable(game: TestGame, playerNumber: Int, name: String): Boolean {
            val cardId = game.findCardsInHand(playerNumber, name).single()
            return game.getLegalActions(playerNumber).any { info ->
                (info.action as? com.wingedsheep.engine.core.CastSpell)?.cardId == cardId
            }
        }

        test("an opponent can't cast an instant on the lock controller's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castable(game, 2, "Shock") shouldBe false
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.error shouldNotBe null
        }

        test("an opponent can cast an instant in their own main phase with an empty stack") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castable(game, 2, "Shock") shouldBe true
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
        }

        test("not in their own non-main step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()

            castable(game, 2, "Shock") shouldBe false
        }

        test("not while the stack is non-empty, even in their own main phase") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardsInHand(2, "Shock", 2)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            withClue("the caster holds priority with their own Shock on the stack") {
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldNotBe null
            }
        }

        test("the lock's controller is not restricted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castable(game, 1, "Shock") shouldBe true
        }

        test("a flash permission doesn't override the restriction (CR 101.2, the Teferi ruling)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(2, "Test Flash For All")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Test Flash For All").error shouldBe null
            game.resolveStack()
            // Move to combat: a flash-granted creature would be castable there without the lock.
            game.state = game.state.copy(phase = Phase.COMBAT, step = Step.BEGIN_COMBAT, priorityPlayerId = game.player2Id)
            castable(game, 2, "Grizzly Bears") shouldBe false
        }

        test("a spell cast during a resolution is never at sorcery timing (CR 608.2): discover puts it into hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Sorcery Lock")
                .withCardInHand(2, "Geological Appraiser")
                .withLandsOnBattlefield(2, "Mountain", 4)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Geological Appraiser").error shouldBe null
            game.resolveStack()
            var guard = 0
            while (game.state.pendingDecision is YesNoDecision && guard++ < 3) {
                game.answerYesNo(true)
                game.resolveStack()
            }

            val bearsZone = game.state.zones.entries.firstOrNull { (_, ids) ->
                ids.any { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears" }
            }?.key
            withClue("the discovered card couldn't be cast, so it went to hand (Bears in $bearsZone, " +
                "pending ${game.state.pendingDecision}, stack ${game.state.stack.size}, guard $guard)") {
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }

        test("the static describes itself in printed words") {
            SorceryLock.script.staticAbilities.single().description shouldContain
                "can cast spells only any time they could cast a sorcery"
        }
    }
}
