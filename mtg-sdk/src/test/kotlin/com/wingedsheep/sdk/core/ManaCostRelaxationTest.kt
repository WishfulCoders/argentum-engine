package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The two cost relaxations behind CR 609.4b's "as though" spending permissions: "mana of any type"
 * ([ManaCost.relaxColors]) waives every color and the colorless type; "as though it were mana of
 * any color" ([ManaCost.relaxColoredRequirements]) waives only colors, so `{C}` survives.
 */
class ManaCostRelaxationTest : StringSpec({

    "any type relaxes colored and colorless pips alike" {
        ManaCost.parse("{3}{C}{R}").relaxColors() shouldBe ManaCost.parse("{5}")
    }

    "any color relaxes colored pips but keeps {C}" {
        ManaCost.parse("{3}{C}{R}").relaxColoredRequirements() shouldBe ManaCost.parse("{4}{C}")
    }

    "any color relaxes hybrid, Phyrexian and monocolored hybrid pips to one generic each" {
        ManaCost.parse("{W/U}{G/P}{2/B}").relaxColoredRequirements() shouldBe ManaCost.parse("{3}")
    }

    "any color leaves X and snow requirements alone" {
        ManaCost.parse("{X}{S}{G}").relaxColoredRequirements() shouldBe ManaCost.parse("{1}{X}{S}")
    }

    "a cost with nothing to relax is returned unchanged" {
        val cost = ManaCost.parse("{2}{C}")
        cost.relaxColoredRequirements() shouldBe cost
    }
})
