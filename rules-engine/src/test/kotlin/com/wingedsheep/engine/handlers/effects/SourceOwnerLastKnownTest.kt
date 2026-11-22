package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class SourceOwnerLastKnownTest : FunSpec({
    val source = EntityId.generate()
    val owner = EntityId.generate()
    val thief = EntityId.generate()
    val snapshot = EntitySnapshot(entityId = source, ownerId = owner, controllerId = thief)
    val context = EffectContext(sourceId = source, controllerId = thief)
    fun ownerOf(ctx: EffectContext) = TargetResolutionUtils.resolvePlayerRef(Player.OwnerOfSource, ctx, GameState())

    test("departed activated source resolves owner independently of controller") {
        ownerOf(context.copy(lastKnownSourceSnapshot = snapshot)) shouldBe owner
    }
    test("departed leave-trigger source resolves owner from the carried event snapshot") {
        ownerOf(context.copy(triggerContext = TriggerContext(triggeringLastKnownSnapshot = snapshot))) shouldBe owner
    }
    test("unrelated departed triggering creature cannot substitute for the source owner") {
        ownerOf(context.copy(triggerContext = TriggerContext(
            triggeringLastKnownSnapshot = snapshot.copy(entityId = EntityId.generate()),
        ))) shouldBe null
    }
    test("mismatched source snapshot cannot substitute for the source owner") {
        ownerOf(context.copy(lastKnownSourceSnapshot = snapshot.copy(entityId = EntityId.generate()))) shouldBe null
    }
    test("last-known owner survives serialization of the resolving context") {
        val encoded = Json.encodeToString(EffectContext.serializer(), context.copy(lastKnownSourceSnapshot = snapshot))
        ownerOf(Json.decodeFromString(EffectContext.serializer(), encoded)) shouldBe owner
    }
    test("a stolen token's self-sacrifice cost captures its owner for the later draw") {
        val card = card("Owner Read Test Token") {
            manaCost = "{0}"
            typeLine = "Creature — Robot"
            power = 2
            toughness = 2
            activatedAbility {
                cost = Costs.SacrificeSelf
                effect = Effects.DrawCards(
                    1, EffectTarget.PlayerRef(Player.OwnerOfSource),
                )
            }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + card)
        d.initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val token = d.putCreatureOnBattlefield(d.player2, card.name)
        d.addComponent(token, TokenComponent)
        val stolen = d.services.effectExecutorRegistry.execute(
            d.state, Effects.GainControl(EffectTarget.Self),
            EffectContext(sourceId = token, controllerId = d.player1),
        )
        stolen.error shouldBe null
        d.replaceState(stolen.state)
        d.state.projectedState.getController(token) shouldBe d.player1
        val hand = d.getHandSize(d.player2)
        d.submit(ActivateAbility(d.player1, token, card.activatedAbilities.single().id))
            .error shouldBe null
        d.state.getEntity(token) shouldBe null
        d.bothPass()
        d.getHandSize(d.player2) shouldBe hand + 1
    }

})
