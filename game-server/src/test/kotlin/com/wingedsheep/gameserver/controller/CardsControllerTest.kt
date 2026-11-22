package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class CardsControllerTest : FunSpec({

    val registry = CardRegistry().apply { register(PortalSet.cards.map { it.copy(setCode = "POR") }) }
    val controller = CardsController(registry, PrintingRegistry())

    test("the catalog carries printed power and toughness, which pow/tou search reads") {
        val snake = controller.getCards().single { it.name == "Anaconda" }
        snake.power shouldBe "3"
        snake.toughness shouldBe "3"
    }

    test("pow/tou search filters the catalog by printed power") {
        val result = controller.search("pow=3 tou=3 t:snake")
        result.errors shouldBe emptyList()
        result.cards.map { it.name } shouldContain "Anaconda"
        controller.search("pow>=4 t:snake").cards.map { it.name } shouldNotContain "Anaconda"
    }
})
