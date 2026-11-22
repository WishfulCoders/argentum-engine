package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.MirrormindCrown
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TokenEntryGroupContinuationTest : FunSpec({
    fun observer(name: String, riots: Boolean = false) = card(name) {
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        if (riots) riot()
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Any.sourceItself() or GameObjectFilter.Creature.youControl()).enters()
            effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
        }
    }
    val watcher = observer("Continuation Entry Observer")
    val rioter = observer("Riot Continuation Entry Observer", true)
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(watcher, rioter, MirrormindCrown))
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        repeat(30) {
            if (d.pendingDecision == null && d.stackSize > 0) d.bothPass().error shouldBe null
        }
    }
    fun quest(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0
    fun tokens(d: GameTestDriver) = d.getPermanents(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
    fun chooseHaste(d: GameTestDriver) {
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = choice.options.indexOfFirst { it.contains("haste", ignoreCase = true) }
        (index >= 0) shouldBe true
        d.submitDecision(d.player1, com.wingedsheep.engine.core.OptionChosenResponse(choice.id, index)).error shouldBe null
    }

    test("two riot observer copies retain one entry instruction across separate as-enters questions") {
        val d = driver()
        val original = d.putCreatureOnBattlefield(d.player1, rioter.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.CreateTokenCopyOfTarget(EffectTarget.SpecificEntity(original), count = 2),
            EffectContext(sourceId = original, controllerId = d.player1))
        result.error shouldBe null
        d.replaceState(result.state)
        chooseHaste(d)
        chooseHaste(d)
        resolve(d)
        val copies = tokens(d)
        copies.size shouldBe 2
        copies.forEach { quest(d, it) shouldBe 2 }
    }

    test("sequential copy instructions with a riot pause do not become one entry group") {
        val d = driver()
        val original = d.putCreatureOnBattlefield(d.player1, rioter.name)
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Composite(listOf(
                Effects.CreateTokenCopyOfTarget(EffectTarget.SpecificEntity(bear)),
                Effects.CreateTokenCopyOfTarget(EffectTarget.SpecificEntity(original))
            )), EffectContext(sourceId = original, controllerId = d.player1))
        result.error shouldBe null
        d.replaceState(result.state)
        chooseHaste(d)
        resolve(d)
        val copy = tokens(d).single { d.getCardName(it) == rioter.name }
        quest(d, copy) shouldBe 1
    }

    test("optional attached-copy replacement retains simultaneous observers after its yes-no question") {
        val d = driver()
        val original = d.putCreatureOnBattlefield(d.player1, watcher.name)
        val crown = d.putPermanentOnBattlefield(d.player1, MirrormindCrown.name)
        d.addComponent(crown, AttachedToComponent(original))
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.CreateTokensTogether(
                Effects.CreateToken(1, 1, creatureTypes = setOf("Goblin")),
                Effects.CreateToken(2, 2, creatureTypes = setOf("Bear"))
            ), EffectContext(sourceId = original, controllerId = d.player1))
        result.error shouldBe null
        d.replaceState(result.state)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        resolve(d)
        val copies = tokens(d)
        copies.size shouldBe 2
        copies.forEach { quest(d, it) shouldBe 2 }
    }
})
