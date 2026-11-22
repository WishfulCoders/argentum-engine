package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.CapturedObjectBinding
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "For each permanent exiled this way, its controller reveals cards until they reveal a card that
 * shares a card type with it" (Chaotic Transformation): a loop's `IterationEntity` that this very
 * resolution moved off the battlefield is read as it last existed there (CR 608.2h) — the snapshot
 * the move captured ([EffectContext.departedPermanents]) — for its card types and for "its
 * controller". A card that left the battlefield in some *earlier* event is a new object
 * (CR 400.7) and keeps reading its printed characteristics.
 */
class ExiledThisWayLastKnownTest : FunSpec({

    /** Exile target permanent; its controller reveals until a card sharing a type with it. */
    val reshape = card("Test Exile Reshape") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            target(TargetFilter.Permanent)
            effect = Effects.Pipeline {
                val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
                run(Effects.ForEachInCollection(exiled, Effects.Pipeline {
                    val (found, _) = gatherUntilMatch(
                        GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.IterationEntity),
                        player = Player.ControllerOfIterationEntity,
                    )
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.ControllerOfIterationEntity))
                    run(Effects.ShuffleLibrary(EffectTarget.PlayerRef(Player.ControllerOfIterationEntity)))
                }))
            }
        }
    }

    /** The same, but the exile sits behind a "you may" — so a prompt pauses between move and loop. */
    val mayReshape = card("Test May Exile Reshape") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            target(TargetFilter.Permanent)
            effect = Effects.Pipeline {
                val chosen = gather(CardSource.ChosenTargets)
                run(Effects.May(Effects.Pipeline {
                    move(chosen, CardDestination.ToZone(Zone.EXILE))
                }))
                val exiled = filter(chosen, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
                run(Effects.ForEachInCollection(exiled, Effects.Pipeline {
                    val (found, _) = gatherUntilMatch(
                        GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.IterationEntity),
                    )
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
                }))
            }
        }
    }

    /** For each land card in your graveyard, reveal until a card sharing a type with it. */
    val graveyardEcho = card("Test Graveyard Echo") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land))
                run(Effects.ForEachInCollection(lands, Effects.Pipeline {
                    val (found, _) = gatherUntilMatch(
                        GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.IterationEntity),
                    )
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
                }))
            }
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(reshape, mayReshape, graveyardEcho))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(spell: String, targets: List<EntityId> = emptyList()) {
        val me = activePlayer!!
        val cardId = putCardInHand(me, spell)
        giveMana(me, Color.RED, 1)
        castSpell(me, cardId, targets).outcome shouldBe Outcome.Done
        bothPass()
        stackSize shouldBe 0
    }

    fun GameTestDriver.battlefieldOf(player: EntityId) = state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
    fun GameTestDriver.libraryOf(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))

    test("an animated land exiled this way still shares Creature — its printed type line is only Land") {
        val d = driver(); val me = d.activePlayer!!
        val factory = d.putLandOnBattlefield(me, "Mishra's Factory")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, factory, TestCards.all.first { it.name == "Mishra's Factory" }.activatedAbilities[1].id))
        d.bothPass()
        d.state.projectedState.isCreature(factory) shouldBe true

        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.cast("Test Exile Reshape", listOf(factory))

        d.getExile(me) shouldContain factory
        // Printed Land alone would have skipped the Bears for the Forest under it.
        d.battlefieldOf(me) shouldContain bears
    }

    test("the departure snapshot survives a prompted may between the exile and the loop") {
        val d = driver(); val me = d.activePlayer!!
        val factory = d.putLandOnBattlefield(me, "Mishra's Factory")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, factory, TestCards.all.first { it.name == "Mishra's Factory" }.activatedAbilities[1].id))
        d.bothPass()
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")

        val spell = d.putCardInHand(me, "Test May Exile Reshape")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, spell, listOf(factory)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.pendingDecision shouldNotBe null // the may paused between the exile and the loop
        d.submitYesNo(me, true).outcome shouldBe Outcome.Done
        d.stackSize shouldBe 0

        d.getExile(me) shouldContain factory
        d.battlefieldOf(me) shouldContain bears
    }

    test("a token exiled this way is read from its snapshot, and the reveal finds a card sharing its type") {
        val d = driver(); val me = d.activePlayer!!
        val token = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.addComponent(token, TokenComponent)
        val ornithopter = d.putCardOnTopOfLibrary(me, "Ornithopter")
        d.cast("Test Exile Reshape", listOf(token))

        d.battlefieldOf(me) shouldContain ornithopter
        // The token ceased to exist once state-based actions were checked (CR 704.5d).
        d.getExile(me) shouldNotContain token
    }

    test("a stolen permanent exiled this way: its last controller reveals from their own library") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theirBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val treason = d.putCardInHand(me, "Act of Treason")
        d.giveMana(me, Color.RED, 1); d.giveColorlessMana(me, 2)
        d.castSpell(me, treason, listOf(theirBears)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getController(theirBears) shouldBe me

        val myBears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val oppTop = d.putCardOnTopOfLibrary(opp, "Grizzly Bears")
        d.cast("Test Exile Reshape", listOf(theirBears))

        d.getExile(opp) shouldContain theirBears
        d.battlefieldOf(me) shouldContain myBears
        d.battlefieldOf(opp) shouldNotContain oppTop
        d.libraryOf(opp) shouldContain oppTop
    }

    test("a card that left the battlefield in an earlier event reads its printed types, not that exit snapshot") {
        val d = driver(); val me = d.activePlayer!!
        val factory = d.putCardInGraveyard(me, "Mishra's Factory")
        // It died last turn while animated — the card still carries that battlefield exit.
        d.addComponent(factory, LastKnownPermanentComponent(
            EntitySnapshot(entityId = factory, controllerId = me, typeLine = TypeLine.parse("Artifact Creature Land — Assembly-Worker"))
        ))
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.cast("Test Graveyard Echo")

        d.libraryOf(me) shouldContain bears
        d.battlefieldOf(me).map { d.getCardName(it) } shouldContain "Forest"
    }

    test("a departed snapshot answers for a loop object that no longer exists at all") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val vanished = EntityId.generate() // a token gone from the game
        val snapshot = EntitySnapshot(entityId = vanished, controllerId = opp, typeLine = TypeLine.parse("Artifact"))
        val context = EffectContext(
            sourceId = null,
            controllerId = me,
            departedPermanents = listOf(snapshot),
            objectReferences = ObjectReferenceEnvironment(iteration = CapturedObjectBinding(vanished, null)),
        )
        val filter = GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.IterationEntity)
        val artifact = d.putPermanentOnBattlefield(me, "Ornithopter")
        val land = d.putLandOnBattlefield(me, "Forest")
        val predicates = d.services.predicateEvaluator
        val predicateContext = PredicateContext.fromEffectContext(context)

        predicates.matches(d.state, d.state.projectedState, artifact, filter, predicateContext) shouldBe true
        predicates.matches(d.state, d.state.projectedState, land, filter, predicateContext) shouldBe false
        context.resolvePlayerTarget(EffectTarget.PlayerRef(Player.ControllerOfIterationEntity), d.state) shouldBe opp
    }
})
