package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModeOption
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DamageEdge
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ModeOption
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ResolutionAttacker
import com.wingedsheep.engine.core.ResolutionBlocker
import com.wingedsheep.engine.core.ResolutionDefender
import com.wingedsheep.engine.core.ResolutionTargetKind
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.WaterbendPermanentChoice
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class StructuredDecisionPayloadTest : ScenarioTestBase() {
    private val selectionSpell = card("Policy Selection Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Composite(
                GatherCardsEffect(CardSource.FromZone(Zone.HAND), "hand"),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                    storeSelected = "selected",
                    storeRemainder = "rest",
                    prompt = "Choose two cards",
                ),
            )
        }
    }

    init {
        cardRegistry.register(selectionSpell)

        test("every PendingDecision subtype is classified and structured payloads round-trip") {
            val p1 = EntityId.of("p1")
            val p2 = EntityId.of("p2")
            val a = EntityId.of("a")
            val b = EntityId.of("b")
            val context = DecisionContext(sourceId = a, sourceName = "Source")
            val card = SearchCardInfo("Card", "{1}", "Creature — Test", colors = listOf("U"), power = 2)
            val decisions = listOf(
                ChooseTargetsDecision(
                    "targets", p1, "Choose targets", context,
                    listOf(TargetRequirementInfo(0, "target creature", 1, 2, sameOwner = true)),
                    mapOf(0 to listOf(a, b)), canCancel = true,
                ),
                SelectCardsDecision(
                    "cards", p1, "Choose cards", context, listOf(a, b), 1, 2, ordered = true,
                    cardInfo = mapOf(a to card, b to card), onePerCardName = true,
                    maxTotalManaValue = 4,
                ),
                YesNoDecision("yes", p1, "Yes?", context),
                BatchYesNoDecision("batch", p1, "All?", context, count = 2),
                ChooseModeDecision(
                    "modes", p1, "Modes", context,
                    listOf(ModeOption(0, "First"), ModeOption(1, "Second", available = false)),
                    minModes = 1, maxModes = 2,
                ),
                ChooseColorDecision("color", p1, "Color", context, setOf(Color.BLUE)),
                ChooseNumberDecision("number", p1, "Number", context, 1, 3),
                DistributeDecision(
                    "distribution", p1, "Divide", context, 3, listOf(a, b), 1,
                    mapOf(a to 2), allowPartial = false,
                ),
                OrderObjectsDecision("order", p1, "Order", context, listOf(a, b), mapOf(a to card)),
                SplitPilesDecision(
                    "split", p1, "Split", context, listOf(a, b), 2,
                    listOf("One", "Two"), mapOf(a to card),
                ),
                ChooseOptionDecision("option", p1, "Option", context, listOf("A", "B")),
                ChooseReplacementDecision(
                    "replacement", p1, "Replace", context,
                    listOf("blue"), listOf("red", "green"), allowedToByFrom = listOf(listOf(1)),
                ),
                SearchLibraryDecision(
                    "search", p1, "Search", context, listOf(a, b), 0, 1,
                    mapOf(a to card, b to card), "a creature card",
                ),
                ReorderLibraryDecision("reorder", p1, "Reorder", context, listOf(a, b), mapOf(a to card, b to card)),
                AssignDamageDecision(
                    "damage", p1, "Damage", context, a, 3, listOf(b), p2,
                    mapOf(b to 2), mapOf(b to 2, p2 to 1), hasTrample = true, hasDeathtouch = false,
                ),
                CombatResolutionDecision(
                    "combat", p1, "Combat", context, firstStrike = false,
                    attackers = listOf(
                        ResolutionAttacker(
                            a, "Attacker", 3, 3, true, false, false, false, true,
                            null, p2, listOf(b), 0,
                        ),
                    ),
                    blockers = listOf(
                        ResolutionBlocker(
                            b, "Blocker", 2, 2, false, false, false, true,
                            listOf(a), listOf(a), 0,
                        ),
                    ),
                    defenders = listOf(ResolutionDefender(p2, ResolutionTargetKind.PLAYER, "Opponent", 20)),
                    edges = listOf(
                        DamageEdge(
                            "a->b", a, b, DamageEdgeDirection.ATTACKER_TO_BLOCKER,
                            2, 3, 2, true, false, p1,
                        ),
                    ),
                ),
                SelectManaSourcesDecision(
                    "mana", p1, "Pay", context,
                    listOf(ManaSourceOption(a, "Island", setOf(Color.BLUE), false)),
                    "{1}", listOf(a), canDecline = true,
                    waterbendPermanents = listOf(WaterbendPermanentChoice(b, "Creature", true)),
                ),
                BudgetModalDecision(
                    "budget", p1, "Spend", context, 5,
                    listOf(BudgetModeOption(2, "First"), BudgetModeOption(3, "Second")),
                ),
            )

            val payloads = decisions.mapNotNull { it.toStructuredDecisionPayload() }
            payloads.size shouldBe 13
            payloads.map { it.responseType }.toSet() shouldBe setOf(
                "TargetsResponse", "CardsSelectedResponse", "ModesChosenResponse",
                "ReplacementChosenResponse", "DistributionResponse", "OrderedResponse",
                "PilesSplitResponse", "DamageAssignmentResponse", "CombatResolutionResponse",
                "ManaSourcesSelectedResponse", "BudgetModalResponse",
            )

            val json = Json { classDiscriminator = "payloadKind"; encodeDefaults = true }
            val serializer = ListSerializer(StructuredDecisionPayload.serializer())
            json.decodeFromString(serializer, json.encodeToString(serializer, payloads)) shouldBe payloads

            payloads.filterIsInstance<TargetsDecisionPayload>().single().requirements.single().let {
                it.legalTargetIds shouldContainExactly listOf(a, b)
                it.sameOwner shouldBe true
            }
            payloads.filterIsInstance<ReplacementDecisionPayload>()
                .single().allowedToByFrom shouldBe listOf(listOf(1))
        }

        test("structured options are visible only to the acting player and can be stepped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Policy Selection Test")
                .withCardsInHand(1, "Grizzly Bears", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Policy Selection Test").error shouldBe null
            game.resolveStack()

            val environment = GameEnvironment.create(cardRegistry)
            environment.restore(game.state, listOf(game.player1Id, game.player2Id))
            val actingGym = GameGymEnv(environment, perspectivePlayerIndex = 0, defaultRevealAll = false)
            val actingObservation = actingGym.observe().observation as TrainingObservation
            val pending = actingObservation.pendingDecision.shouldNotBeNull()
            val payload = pending.structuredPayload.shouldBeInstanceOf<CardsDecisionPayload>()
            payload.responseType shouldBe "CardsSelectedResponse"
            payload.minSelections shouldBe 2
            payload.maxSelections shouldBe 2

            val opponentObservation = ObservationBuilder(cardRegistry).build(
                environment.state,
                game.player2Id,
                environment.legalActions(),
            ).observation as TrainingObservation
            opponentObservation.pendingDecision.shouldNotBeNull().structuredPayload.shouldBeNull()

            val result = actingGym.submitDecision(
                CardsSelectedResponse(pending.decisionId, payload.optionIds.take(2)),
            )
            result.observation.pendingDecision.shouldBeNull()
        }
    }
}
