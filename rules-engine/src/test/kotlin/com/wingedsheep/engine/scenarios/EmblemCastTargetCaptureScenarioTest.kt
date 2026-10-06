package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test: an emblem's (global triggered ability's) "whenever you cast a spell that targets one
 * or more permanents, gain control of those permanents" — Dack Fayden's −6 — captures the spell's
 * matching targets when it triggers, like the battlefield and command-zone scans already did.
 *
 * Pins: the emblem's ability resolves before the spell (Dack ruling 2016-06-08), so you control the
 * creature by the time your Lightning Bolt resolves; the capture is taken at trigger time, so
 * countering the spell in response doesn't stop the control change (CR 113.7a); a spell that
 * targets only a player doesn't trigger it.
 */
class EmblemCastTargetCaptureScenarioTest : ScenarioTestBase() {

    private val emblemMaker = card("Thief's Emblem") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You get an emblem with \"Whenever you cast a spell that targets one or more permanents, gain control of those permanents.\""
        spell {
            effect = Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.you.casts(
                        requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Permanent))
                    )
                    effect = Effects.ForEachInCollection(
                        collection = CollectionSlot.TriggerCaptured,
                        effect = Effects.GainControl(EffectTarget.IterationEntity)
                    )
                },
                descriptionOverride = "Whenever you cast a spell that targets one or more permanents, gain control of those permanents."
            )
        }
    }

    private fun board() = scenario()
        .withPlayers("Thief", "Victim")
        .withCardInHand(1, "Thief's Emblem")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Force of Nature")
        .withCardInHand(2, "Counterspell")
        .withLandsOnBattlefield(2, "Island", 2)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(emblemMaker)

        test("the emblem takes the targeted permanent before the spell resolves") {
            val game = board()
            game.castSpell(1, "Thief's Emblem").error shouldBe null
            game.resolveStack()
            val force = game.findPermanent("Force of Nature")!!
            game.castSpell(1, "Lightning Bolt", force).error shouldBe null
            withClue("the emblem's trigger sits above the Bolt") { game.state.stack.size shouldBe 2 }
            game.resolveStack()
            game.state.projectedState.getController(force) shouldBe game.player1Id
        }

        test("the targets are captured when it triggers, so countering the spell doesn't stop it") {
            val game = board()
            game.castSpell(1, "Thief's Emblem").error shouldBe null
            game.resolveStack()
            val force = game.findPermanent("Force of Nature")!!
            game.castSpell(1, "Lightning Bolt", force).error shouldBe null
            game.execute(PassPriority(game.player1Id)).error shouldBe null
            game.state.priorityPlayerId shouldBe game.player2Id
            game.castSpellTargetingStackSpell(2, "Counterspell", "Lightning Bolt").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.state.projectedState.getController(force) shouldBe game.player1Id
        }

        test("a spell that targets only a player doesn't trigger it") {
            val game = board()
            game.castSpell(1, "Thief's Emblem").error shouldBe null
            game.resolveStack()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.state.projectedState.getController(game.findPermanent("Force of Nature")!!) shouldBe game.player2Id
        }
    }
}
