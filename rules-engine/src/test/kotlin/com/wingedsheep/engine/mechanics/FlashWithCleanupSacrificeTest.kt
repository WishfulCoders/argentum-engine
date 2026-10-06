package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import io.kotest.matchers.shouldBe

/**
 * The Mirage-block instant-speed permanent (`flashWithCleanupSacrifice`): "You may cast this spell
 * as though it had flash. If you cast it any time a sorcery couldn't have been cast, the controller
 * of the permanent it becomes sacrifices it at the beginning of the next cleanup step."
 *
 * Rules pinned here:
 *  - The permission lets the card be cast whenever an instant could be (CR 702.8a) — the server
 *    offers it in the opponent's turn.
 *  - "Any time a sorcery couldn't have been cast" is CR 307.1: not your main phase, or the stack
 *    isn't empty. Cast with sorcery timing, nothing is owed.
 *  - The sacrifice is a delayed triggered ability (CR 603.7a) that triggers at the beginning of the
 *    next cleanup step and goes on the stack in that step (CR 514.3a).
 *  - It is owed only when this permission was the one used (Necromancy ruling 2022-12-08): with
 *    another effect letting the spell be cast as though it had flash, nothing is sacrificed.
 *  - It affects only the permanent the spell became: one that left the battlefield and returned is
 *    a new object (CR 603.7c / 400.7).
 */
class FlashWithCleanupSacrificeTest : ScenarioTestBase() {

    init {
        cardRegistry.register(card("Instant Ward Test") {
            manaCost = "{W}"
            typeLine = "Enchantment"
            oracleText = "You may cast this spell as though it had flash. If you cast it any time a sorcery " +
                "couldn't have been cast, the controller of the permanent it becomes sacrifices it at the " +
                "beginning of the next cleanup step."
            flashWithCleanupSacrifice = true
        })
        cardRegistry.register(card("Instant Bear Test") {
            manaCost = "{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
            flashWithCleanupSacrifice = true
        })
        cardRegistry.register(card("Enchantment Flash Granter Test") {
            manaCost = "{2}"
            typeLine = "Artifact"
            staticAbility { ability = GrantFlashToSpellType(GameObjectFilter.Enchantment, controllerOnly = true) }
        })

        fun opponentsTurn(vararg extra: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Instant Ward Test")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Forest", 1)
            .apply { extra.forEach { withCardOnBattlefield(1, it) } }
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withPriorityPlayer(1)
            .build()

        fun TestGame.castWard() {
            castSpell(1, "Instant Ward Test").error shouldBe null
            resolveStack()
            isOnBattlefield("Instant Ward Test") shouldBe true
        }

        test("the server offers it at instant speed, and cast that way it is sacrificed in the next cleanup step") {
            val game = opponentsTurn()
            val ward = game.findCardsInHand(1, "Instant Ward Test").single()
            game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == ward } shouldBe true

            game.castSpell(1, "Instant Ward Test").error shouldBe null
            game.state.getEntity(ward)?.get<SpellOnStackComponent>()?.sacrificeAtNextCleanup shouldBe true
            game.resolveStack()
            game.isOnBattlefield("Instant Ward Test") shouldBe true

            // The cleanup step stops for the delayed trigger, with the active player to act.
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.state.stack.size shouldBe 1
            game.state.priorityPlayerId shouldBe game.player2Id
            game.resolveStack()
            game.isOnBattlefield("Instant Ward Test") shouldBe false
            game.isInGraveyard(1, "Instant Ward Test") shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
        }

        test("cast with sorcery timing nothing is owed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Instant Ward Test")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ward = game.findCardsInHand(1, "Instant Ward Test").single()
            game.castSpell(1, "Instant Ward Test").error shouldBe null
            game.state.getEntity(ward)?.get<SpellOnStackComponent>()?.sacrificeAtNextCleanup shouldBe false
            game.resolveStack()
            game.state.delayedTriggers shouldBe emptyList()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.isOnBattlefield("Instant Ward Test") shouldBe true
        }

        test("in your own main phase with a spell on the stack a sorcery couldn't be cast, so it is owed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Instant Ward Test")
                .withCardInHand(1, "Giant Growth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            val ward = game.findCardsInHand(1, "Instant Ward Test").single()
            game.castSpell(1, "Instant Ward Test").error shouldBe null
            game.state.getEntity(ward)?.get<SpellOnStackComponent>()?.sacrificeAtNextCleanup shouldBe true
            game.resolveStack()
            game.isOnBattlefield("Instant Ward Test") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.resolveStack()
            game.isInGraveyard(1, "Instant Ward Test") shouldBe true
        }

        test("cast under another 'as though it had flash' permission, it is not sacrificed") {
            val game = opponentsTurn("Enchantment Flash Granter Test")
            val ward = game.findCardsInHand(1, "Instant Ward Test").single()
            game.castSpell(1, "Instant Ward Test").error shouldBe null
            game.state.getEntity(ward)?.get<SpellOnStackComponent>()?.sacrificeAtNextCleanup shouldBe false
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.isOnBattlefield("Instant Ward Test") shouldBe true
        }

        test("a permanent that left the battlefield and returned is a new object the trigger leaves alone") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Instant Bear Test")
                .withCardInHand(1, "Cloudshift")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withPriorityPlayer(1)
                .build()
            game.castSpell(1, "Instant Bear Test").error shouldBe null
            game.resolveStack()
            val bear = game.findPermanent("Instant Bear Test")!!
            // Still the opponent's main phase; get priority back to the Bear's controller.
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority()
            game.state.step shouldBe Step.PRECOMBAT_MAIN
            game.state.priorityPlayerId shouldBe game.player1Id
            game.castSpell(1, "Cloudshift", bear).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Instant Bear Test") shouldBe true

            // The delayed trigger still fires, but the returned Bear is a new object.
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.resolveStack()
            game.isOnBattlefield("Instant Bear Test") shouldBe true
            game.isInGraveyard(1, "Instant Bear Test") shouldBe false
        }
    }
}
