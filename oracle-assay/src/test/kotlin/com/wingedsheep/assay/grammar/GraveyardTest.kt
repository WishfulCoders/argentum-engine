package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GraveyardTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line)
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun requirement(line: String): TargetObject =
        fragment(line).script.targetRequirements.single()
            .shouldBeInstanceOf<TargetObject>()

    "return up to one target from your graveyard to your hand" {
        val line = "Return up to one target creature card from your graveyard to your hand."
        val parsed = fragment(line).script

        requirement(line).optional shouldBe true
        parsed.spellEffect shouldBe Effects.Move(Targets.bound(), Zone.HAND)
        roundTrips(line)
    }

    "return up to one target from your graveyard to the battlefield" {
        val line = "Return up to one target artifact card from your graveyard to the battlefield."
        val parsed = fragment(line).script

        requirement(line).optional shouldBe true
        parsed.spellEffect shouldBe Effects.PutOntoBattlefieldFromGraveyard(Targets.bound())
        roundTrips(line)
    }

    "ordinary graveyard targets remain mandatory" {
        val line = "Return target creature card from your graveyard to your hand."

        requirement(line).optional shouldBe false
        roundTrips(line)
    }

    "a triggered optional graveyard target belongs to the triggered ability" {
        val line = "When ~ enters, return up to one target creature card from your graveyard to your hand."
        val parsed = fragment(line).script
        val triggered = parsed.triggeredAbilities.single()

        triggered.targetRequirement.shouldBeInstanceOf<TargetObject>().optional shouldBe true
        parsed.targetRequirements shouldBe emptyList()
        roundTrips(line)
    }

    "graveyard cards can be put on top of your library" {
        val line = "Put target creature card from your graveyard on top of your library."
        val parsed = fragment(line).script

        requirement(line).optional shouldBe false
        parsed.spellEffect shouldBe Effects.PutOnTopOfLibrary(Targets.bound())
        roundTrips(line)
    }

    "graveyard cards can be put on the bottom of your library" {
        val line = "Put target card from your graveyard on the bottom of your library."
        val parsed = fragment(line).script

        requirement(line).optional shouldBe false
        parsed.spellEffect shouldBe Effects.PutOnBottomOfLibrary(Targets.bound())
        roundTrips(line)
    }

    "graveyard target requirements specify zone and ownership" {
        val target = requirement("Return target creature card from your graveyard to your hand.")
        target.filter.zone shouldBe Zone.GRAVEYARD
        target.filter.baseFilter shouldBe GameObjectFilter.Creature.ownedByYou()
    }
})
