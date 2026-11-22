package com.wingedsheep.engine.mechanics.ante

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.OwnershipChangedEvent
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.zones.ChangeOwnershipExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OriginalOwnerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChangeOwnershipEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Playing for ante (CR 407) at the engine level: the opening ante, the settlement of a finished
 * game, and the ownership change the ante cards make. The cards themselves have their own
 * scenario tests.
 */
class AnteTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }
    val deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20)

    fun start(playingForAnte: Boolean, seed: Long = 7L) = GameInitializer(registry).initializeGame(
        GameConfig(
            players = listOf(PlayerConfig("Alice", deck), PlayerConfig("Bob", deck)),
            skipMulligans = true,
            seed = seed,
            playingForAnte = playingForAnte,
        )
    )

    fun GameState.ante(player: EntityId) = getZone(ZoneKey(player, Zone.ANTE))

    context("the opening ante (CR 407.2)") {
        test("each player antes one card of their own deck before the opening draw") {
            val result = start(playingForAnte = true)
            val state = result.state

            state.playingForAnte shouldBe true
            for (player in result.playerIds) {
                val anted = state.ante(player)
                anted.size shouldBe 1
                state.getEntity(anted.single())?.get<OwnerComponent>()?.playerId shouldBe player
                state.getZone(ZoneKey(player, Zone.HAND)).size shouldBe 7
                state.getZone(ZoneKey(player, Zone.LIBRARY)).size shouldBe 32
            }
        }

        test("a game not played for ante has no ante and draws from the full deck") {
            val result = start(playingForAnte = false)

            result.state.playingForAnte shouldBe false
            for (player in result.playerIds) {
                result.state.ante(player).shouldBeEmpty()
                result.state.getZone(ZoneKey(player, Zone.LIBRARY)).size shouldBe 33
            }
        }

        test("the anted cards are part of the seeded game, so a replay antes the same cards") {
            val first = start(playingForAnte = true, seed = 42L)
            val second = start(playingForAnte = true, seed = 42L)

            first.playerIds.map { first.state.ante(it) } shouldBe second.playerIds.map { second.state.ante(it) }
        }
    }

    context("settling a finished game (CR 407.2)") {
        test("the winner becomes the owner of everything in the ante") {
            val result = start(playingForAnte = true)
            val (alice, bob) = result.playerIds
            val bobsCard = result.state.ante(bob).single()
            val over = result.state.copy(gameOver = true, winnerId = alice)

            val transfers = AnteSettlement.settle(over)

            // Alice's own anted card simply comes back to her; only Bob's changes hands.
            transfers.size shouldBe 1
            transfers.single().cardId shouldBe bobsCard
            transfers.single().fromPlayerId shouldBe bob
            transfers.single().toPlayerId shouldBe alice
            transfers.single().cause shouldBe AnteTransferCause.ANTE
        }

        test("a draw leaves the ante with its owners") {
            val result = start(playingForAnte = true)
            val over = result.state.copy(gameOver = true, winnerId = null)

            AnteSettlement.settle(over).shouldBeEmpty()
        }

        test("a game in progress settles nothing from the ante") {
            AnteSettlement.settle(start(playingForAnte = true).state).shouldBeEmpty()
        }

        test("a game not played for ante settles nothing") {
            val result = start(playingForAnte = false)
            AnteSettlement.settle(result.state.copy(gameOver = true, winnerId = result.playerIds[0])).shouldBeEmpty()
        }
    }

    context("changing a card's owner (CR 407.3)") {
        test("the card is refiled under its new owner without changing zones, and the change is for keeps") {
            val result = start(playingForAnte = true)
            val (alice, bob) = result.playerIds
            val card = result.state.ante(bob).single()
            val identityBefore = result.state.objectIdentities.getValue(card).generation

            val changed = ChangeOwnershipExecutor().execute(
                result.state,
                ChangeOwnershipEffect(EffectTarget.SpecificEntity(card), EffectTarget.Controller),
                EffectContext(sourceId = null, controllerId = alice),
            )

            val state = changed.state
            state.getEntity(card)?.get<OwnerComponent>()?.playerId shouldBe alice
            state.getEntity(card)?.get<CardComponent>()?.ownerId shouldBe alice
            state.getEntity(card)?.get<OriginalOwnerComponent>()?.playerId shouldBe bob
            (card in state.ante(alice)) shouldBe true
            (card in state.ante(bob)) shouldBe false
            state.objectIdentities.getValue(card).generation shouldBe identityBefore
            changed.events.single().shouldBeInstanceOf<OwnershipChangedEvent>().newOwnerId shouldBe alice

            // Still in the ante with no winner yet, the change itself already counts.
            val transfer = AnteSettlement.settle(state).single()
            transfer.fromPlayerId shouldBe bob
            transfer.toPlayerId shouldBe alice
            transfer.cause shouldBe AnteTransferCause.OWNERSHIP
        }

        test("a card that comes back to its first owner reports nothing") {
            val result = start(playingForAnte = true)
            val (alice, bob) = result.playerIds
            val card = result.state.ante(bob).single()
            val executor = ChangeOwnershipExecutor()
            val effect = ChangeOwnershipEffect(EffectTarget.SpecificEntity(card), EffectTarget.Controller)

            val away = executor.execute(result.state, effect, EffectContext(sourceId = null, controllerId = alice)).state
            val home = executor.execute(away, effect, EffectContext(sourceId = null, controllerId = bob)).state

            home.getEntity(card)?.get<OriginalOwnerComponent>() shouldBe null
            AnteSettlement.settle(home).shouldBeEmpty()
        }

        test("the winner's claim on the ante beats an earlier ownership change") {
            val result = start(playingForAnte = true)
            val (alice, bob) = result.playerIds
            val card = result.state.ante(alice).single()
            // Bob takes Alice's anted card mid-game, but Alice wins — the ante is hers again.
            val stolen = ChangeOwnershipExecutor().execute(
                result.state,
                ChangeOwnershipEffect(EffectTarget.SpecificEntity(card), EffectTarget.Controller),
                EffectContext(sourceId = null, controllerId = bob),
            ).state
            stolen.getEntity(card)?.get<OwnerComponent>()?.playerId shouldNotBe alice

            val settled = AnteSettlement.settle(stolen.copy(gameOver = true, winnerId = alice))

            settled.none { it.cardId == card } shouldBe true
        }
    }
})
