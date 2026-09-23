package com.wingedsheep.ai.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The profile names every run identifies its agent by. One parser serves the arena, the gym, the
 * replay harness and the game-server, so a name means the same player everywhere.
 */
class ProfileTokensTest : StringSpec({

    "AiProfiles.parse and profileFromTokens are one parser" {
        AiProfiles.parse("raceclock+timing") shouldBe profileFromTokens("raceclock+timing")
    }

    "tokens apply left to right to the default AI" {
        val p = profileFromTokens("raceclock+timing+fixing+grants+locked")
        p.discountedRaceClock shouldBe true
        p.holdRemovalForBetterTargets shouldBe true
        p.chargesForUnavailableColours shouldBe true
        p.expiringGrantsNeedACombat shouldBe true
        p.creatureValuation.lockedCreaturesAreInert shouldBe true
    }

    "a misspelled token is the caller's mistake, so the gym answers 400" {
        shouldThrow<IllegalArgumentException> { profileFromTokens("raceclock+timming") }
            .message shouldContain "unknown profile token"
    }

    // The one-sided screen's second correction slot (mtg-draft-ai `docs/50` §4): with no artifact
    // directory installed, each correction token names the file it wanted rather than falling back
    // to the default evaluator, which would make an arm silently measure nothing.
    "a correction token without its artifact names the file it needs" {
        shouldThrow<IllegalArgumentException> { profileFromTokens("raceclock+correction-actions") }
            .message shouldContain "shared-correction.json"
        shouldThrow<IllegalArgumentException> { profileFromTokens("raceclock+tcorrection-actions") }
            .message shouldContain "target-correction.json"
        shouldThrow<IllegalArgumentException> { profileFromTokens("raceclock+tcorrection") }
            .message shouldContain "target-correction.json"
    }
})
