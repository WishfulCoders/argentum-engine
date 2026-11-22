package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Issue #2780, tested as a property instead of field by field: a viewer's output must not depend
 * on the identity of a face-down object that viewer may not look under.
 *
 * Each case forks one state into two worlds that differ only in what a face-down creature really
 * is, plays the same actions in both, and requires everything the viewer is shown at each
 * checkpoint — the projected client state and the pending decision as [DecisionMasker] renders
 * it — to be identical. A name, a mana cost, a colour or a base power that reaches the viewer
 * through any field, including one no one thought to mask, makes the two worlds differ.
 *
 * The engine mints entity ids from a counter in [GameState], so both worlds allocate the same ids
 * and the comparison needs no normalisation.
 */
class HiddenIdentityInvarianceTest : FunSpec({

    // Stand or Fall's shape (INV): the enchantment's controller separates each opponent's creatures
    // into two piles — a selection that offers the opponent's face-down creatures to the separator.
    val pileSplitter = card("Test Pile Splitter") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
            effect = Effects.ForEachPlayer(
                players = Player.EachOpponent,
                Effects.Pipeline {
                    val creatures = gather(CardSource.ControlledPermanents(player = Player.You, filter = GameObjectFilter.Creature))
                    val (pileA, pileB) = chooseAnyNumberSplit(
                        from = creatures,
                        chooser = Chooser.SourceController,
                        selectedLabel = "Pile 1",
                        remainderLabel = "Pile 2",
                        useTargetingUI = true,
                        alwaysPrompt = true,
                    )
                    val (_, cantBlock) = choosePile(pileA, pileB, chooser = Chooser.Controller)
                    run(Effects.ForEachInCollection(collection = cantBlock, effect = Effects.CantBlock(EffectTarget.IterationEntity)))
                }
            )
        }
    }

    data class Rendered(val state: ClientGameState, val decision: PendingDecision?)

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCards(listOf(pileSplitter))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
    }

    fun GameTestDriver.render(viewer: EntityId): Rendered {
        val visibility = Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = cardRegistry).conditions)
        val transformer = ClientStateTransformer(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = cardRegistry))
        return Rendered(
            transformer.transform(state, viewer),
            state.pendingDecision?.let { DecisionMasker(visibility).maskFor(it, state, viewer) },
        )
    }

    fun GameTestDriver.faceDown(entityId: EntityId) =
        replaceState(state.updateEntity(entityId) { it.with(FaceDownComponent) })

    fun GameTestDriver.advanceUntilDecision() {
        repeat(50) {
            if (state.pendingDecision != null) return
            submit(PassPriority(state.priorityPlayerId ?: error("No priority and no pending decision")))
        }
        error("No pending decision; step=$currentStep")
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

    /**
     * Plays [script] from the current state twice — once as is, once with [hidden] really being
     * [otherIdentity] — and requires every checkpoint [script] renders for [viewer] to match.
     */
    fun GameTestDriver.assertBlindTo(
        hidden: EntityId,
        otherIdentity: String,
        viewer: EntityId,
        script: GameTestDriver.(checkpoint: () -> Unit) -> Unit,
    ) {
        val fork = state
        fun play(world: GameState): List<Rendered> {
            replaceState(world)
            val seen = mutableListOf<Rendered>()
            script { seen += render(viewer) }
            return seen
        }
        val original = play(fork)
        val swapped = play(withIdentity(fork, hidden, otherIdentity))
        original.size shouldBe swapped.size
        original.zip(swapped).forEach { (a, b) -> a shouldBe b }
        // The identities really were different in the two worlds.
        fork.getEntity(hidden)?.get<CardComponent>()?.name shouldNotBe otherIdentity
    }

    test("separating an opponent's creatures into piles does not reveal a face-down one") {
        val driver = newDriver()
        val separator = driver.activePlayer!!
        val opponent = driver.getOpponent(separator)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(separator, "Test Pile Splitter")
        val hidden = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.faceDown(hidden)
        val plain = driver.putCreatureOnBattlefield(opponent, "Llanowar Elves")

        driver.assertBlindTo(hidden, "Savannah Lions", viewer = separator) { checkpoint ->
            advanceUntilDecision()
            val split = state.pendingDecision as SelectCardsDecision
            split.playerId shouldBe separator
            checkpoint()
            submitDecision(separator, CardsSelectedResponse(split.id, listOf(hidden)))
            // The opponent now picks a pile; the separator watches.
            state.pendingDecision?.playerId shouldBe opponent
            checkpoint()
            plain shouldNotBe hidden
        }
    }

    test("the combat-damage board does not reveal a face-down attacker to the defender") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hidden = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.faceDown(hidden)
        driver.removeSummoningSickness(hidden)
        val blockerA = driver.putCreatureOnBattlefield(defender, "Llanowar Elves")
        val blockerB = driver.putCreatureOnBattlefield(defender, "Trample Beast")

        driver.assertBlindTo(hidden, "Savannah Lions", viewer = defender) { checkpoint ->
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            declareAttackers(attacker, listOf(hidden), defender)
            passPriorityUntil(Step.DECLARE_BLOCKERS)
            declareBlockers(defender, mapOf(blockerA to listOf(hidden), blockerB to listOf(hidden)))
            (state.pendingDecision as? OrderObjectsDecision)?.let {
                submitDecision(it.playerId, OrderedResponse(it.id, listOf(blockerA, blockerB)))
            }
            advanceUntilDecision()
            (state.pendingDecision is CombatResolutionDecision) shouldBe true
            checkpoint()
        }
    }

    test("combat does not reveal a face-down blocker to the attacker") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val trampler = driver.putCreatureOnBattlefield(attacker, "Trample Beast")
        driver.removeSummoningSickness(trampler)
        val hidden = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.faceDown(hidden)
        val plain = driver.putCreatureOnBattlefield(defender, "Llanowar Elves")

        driver.assertBlindTo(hidden, "Savannah Lions", viewer = attacker) { checkpoint ->
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            declareAttackers(attacker, listOf(trampler), defender)
            passPriorityUntil(Step.DECLARE_BLOCKERS)
            declareBlockers(defender, mapOf(hidden to listOf(trampler), plain to listOf(trampler)))
            checkpoint()
            (state.pendingDecision as? OrderObjectsDecision)?.let {
                submitDecision(it.playerId, OrderedResponse(it.id, listOf(hidden, plain)))
            }
            advanceUntilDecision()
            (state.pendingDecision is CombatResolutionDecision) shouldBe true
            checkpoint()
        }
    }

    test("the Ring badge does not reveal a face-down Ring-bearer to the opponent") {
        val driver = newDriver()
        val bearerController = driver.activePlayer!!
        val viewer = driver.getOpponent(bearerController)
        val hidden = driver.putCreatureOnBattlefield(bearerController, "Centaur Courser")
        driver.faceDown(hidden)
        driver.replaceState(
            driver.state
                .updateEntity(hidden) { it.with(RingBearerComponent(bearerController)) }
                .updateEntity(bearerController) { it.with(TheRingComponent(temptCount = 1)) }
        )

        driver.assertBlindTo(hidden, "Savannah Lions", viewer = viewer) { checkpoint -> checkpoint() }
    }
})
