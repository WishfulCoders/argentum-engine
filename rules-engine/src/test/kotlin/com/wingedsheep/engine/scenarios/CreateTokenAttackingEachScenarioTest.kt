package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BeingAttackedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [CreateTokenEffect.attackingEach] — "for each opponent, create a … token that's tapped and
 * attacking that player or a planeswalker they control" (Adeline, Resplendent Cathar; myriad's
 * shape, CR 702.116a).
 *
 * Pins:
 * - CR 508.4 — the effect specifies the side, the token's controller picks within it as the token
 *   enters; a decision only when that player controls a planeswalker;
 * - a token attacking a planeswalker attacks *it* (its combat damage removes loyalty, CR 306.8) and
 *   marks it attacked, so CR 506.4 can later remove it from combat;
 * - "for each opponent" is one batch per opponent, each batch attacking its own opponent (CR 802);
 * - entering attacking isn't attacking: "whenever a creature attacks" doesn't trigger (CR 508.3a);
 * - an effect with no `attackingEach` keeps the old defender (no behaviour change).
 */
class CreateTokenAttackingEachScenarioTest : ScenarioTestBase() {

    private val tokenEffect = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
        creatureTypes = setOf("Human"),
        tapped = true,
        attacking = true,
        attackingEach = Player.EachOpponent,
    )

    private val captain = card("Test Cathar Captain") {
        manaCost = "{1}{W}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
        oracleText = "Whenever you attack, for each opponent, create a 1/1 white Human creature token that's tapped and attacking that player or a planeswalker they control."
        triggeredAbility {
            trigger = Triggers.you.attacks()
            effect = tokenEffect
        }
    }

    /** Counts its own controller's "a creature you control attacks" triggers, as life. */
    private val watcher = card("Test Attack Watcher") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human"
        power = 0
        toughness = 1
        oracleText = "Whenever a creature you control attacks, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.a(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.youControl()).attacks()
            effect = Effects.GainLife(1)
        }
    }

    private fun TestGame.humanTokens(): List<EntityId> = state.getBattlefield().filter {
        state.getEntity(it)?.has<TokenComponent>() == true &&
            state.getEntity(it)?.get<CardComponent>()?.name == "Human Token"
    }

    private fun TestGame.loyalty(name: String): Int =
        findPermanent(name)?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) } ?: 0

    private fun TestGame.resolveOrderingOnly() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                null -> passPriority().error shouldBe null
                else -> return
            }
        }
    }

    private fun attackWithCaptain(vararg opponent: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Cathar Captain", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponent.forEach { builder.withCardOnBattlefield(2, it) }
        val game = builder.build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Test Cathar Captain" to 2)).error shouldBe null
        game.resolveOrderingOnly()
        return game
    }

    init {
        cardRegistry.register(captain)
        cardRegistry.register(watcher)

        context("CR 508.4 — the side is specified; a choice only when it offers a planeswalker") {

            test("an opponent with no planeswalker: no decision, the token attacks that player") {
                val game = attackWithCaptain()
                game.hasPendingDecision() shouldBe false
                val token = game.humanTokens().single()
                val entity = game.state.getEntity(token)!!
                entity.get<AttackingComponent>()?.defenderId shouldBe game.player2Id
                entity.has<TappedComponent>() shouldBe true
            }

            test("an opponent with a planeswalker: the controller chooses, and may pick the planeswalker") {
                val game = attackWithCaptain("Ajani Goldmane")
                val ajani = game.findPermanent("Ajani Goldmane")!!
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the token's controller picks, CR 508.4") { decision.playerId shouldBe game.player1Id }
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.player2Id, ajani)
                withClue("no token exists until its defender is settled") { game.humanTokens() shouldBe emptyList() }

                game.selectTargets(listOf(ajani)).error shouldBe null
                val token = game.humanTokens().single()
                game.state.getEntity(token)!!.get<AttackingComponent>()!!.let {
                    it.defenderId shouldBe ajani
                    withClue("the defending player of a planeswalker attack is its controller") {
                        it.defendingPlayerId shouldBe game.player2Id
                    }
                }
                withClue("the planeswalker is marked attacked, for CR 506.4") {
                    game.state.getEntity(ajani)!!.has<BeingAttackedComponent>() shouldBe true
                }

                val loyalty = game.loyalty("Ajani Goldmane")
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the unblocked token's combat damage removed loyalty (CR 306.8)") {
                    game.loyalty("Ajani Goldmane") shouldBe loyalty - 1
                }
                withClue("the Captain's 2 went to the player it attacked") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("picking the player attacks the player") {
                val game = attackWithCaptain("Ajani Goldmane")
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.state.getEntity(game.humanTokens().single())!!
                    .get<AttackingComponent>()!!.defenderId shouldBe game.player2Id
            }
        }

        context("CR 508.3a — entering attacking is not attacking") {

            test("a 'whenever a creature you control attacks' trigger ignores the token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Test Cathar Captain", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Attack Watcher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Test Cathar Captain" to 2)).error shouldBe null
                game.resolveOrderingOnly()
                game.humanTokens().size shouldBe 1
                withClue("one trigger, for the declared Captain only") { game.getLifeTotal(1) shouldBe 21 }
            }
        }

        context("CR 802 — for each opponent, a batch attacking that opponent") {

            test("three players: one token per opponent, each attacking its own opponent or their planeswalker") {
                val deck = Deck(cards = List(40) { "Grizzly Bears" })
                val init = GameInitializer(cardRegistry).initializeGame(
                    GameConfig(
                        players = (1..3).map { PlayerConfig("Player $it", deck, 20) },
                        skipMulligans = true,
                        startingPlayerIndex = 0,
                    )
                )
                val (a, b, c) = init.playerIds
                val (state, walker) = init.state.withPlaneswalker(c)

                val paused = services.effectExecutorRegistry.execute(
                    state, tokenEffect, EffectContext(sourceId = null, controllerId = a)
                )
                withClue("B has no planeswalker, so only C's token needs a pick") {
                    paused.outcome.shouldBeInstanceOf<Outcome.Paused>()
                    val decision = paused.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                    decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(c, walker)
                }
                val decisionId = paused.state.pendingDecision!!.id
                val resumed = actionProcessor.process(
                    paused.state, SubmitDecision(a, TargetsResponse(decisionId, mapOf(0 to listOf(walker))))
                ).result
                resumed.error shouldBe null

                val tokens = resumed.state.getBattlefield().filter {
                    resumed.state.getEntity(it)?.has<TokenComponent>() == true
                }
                tokens.map { resumed.state.getEntity(it)!!.get<AttackingComponent>()!!.defenderId } shouldContainExactlyInAnyOrder
                    listOf(b, walker)
                tokens.forEach { resumed.state.getEntity(it)!!.get<ControllerComponent>()!!.playerId shouldBe a }
            }
        }

        context("No behaviour change without attackingEach") {

            test("a plain 'tapped and attacking' token still attacks the defending player, no decision") {
                val plain = tokenEffect.copy(attackingEach = null)
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ajani Goldmane")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                val result = services.effectExecutorRegistry.execute(
                    game.state, plain, EffectContext(sourceId = game.findPermanent("Grizzly Bears"), controllerId = game.player1Id)
                )
                result.outcome.shouldBeInstanceOf<Outcome.Done>()
                val token = result.state.getBattlefield().single {
                    result.state.getEntity(it)?.has<TokenComponent>() == true
                }
                result.state.getEntity(token)!!.get<AttackingComponent>()!!.defenderId shouldBe game.player2Id
            }
        }
    }

    /** Put a vanilla planeswalker under [owner]'s control onto the battlefield. */
    private fun GameState.withPlaneswalker(owner: EntityId): Pair<GameState, EntityId> {
        val ajani = cardRegistry.requireCard("Ajani Goldmane")
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = ajani.name,
                name = ajani.name,
                manaCost = ajani.manaCost,
                typeLine = ajani.typeLine,
                ownerId = owner,
            ),
            OwnerComponent(owner),
            ControllerComponent(owner),
            CountersComponent().withAdded(CounterType.LOYALTY, 4),
        )
        return withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id) to id
    }
}
