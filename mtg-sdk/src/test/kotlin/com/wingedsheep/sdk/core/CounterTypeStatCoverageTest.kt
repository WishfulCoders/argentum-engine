package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * The stat counters (CR 122.1a — `+1/+1`, `-1/-0`, `+0/+2`) are the kinds whose printed spelling is
 * not a word, so neither direction of the id ↔ printed mapping can fall out of `lowercase()`. Both
 * are derived from the id's shape rather than listed by hand, because a hand-written list is how
 * `+2/+0` and `+0/+2` (Frankenstein's Monster) once came to be declared and unreachable by name.
 * This pins that every stat-shaped kind the SDK knows survives the round trip.
 */
class CounterTypeStatCoverageTest : DescribeSpec({

    // "+1/+1", "-2/-2", "+0/+2" — a sign, a digit, a slash, a sign, a digit.
    val statSpelling = Regex("""^[+-]\d/[+-]\d$""")
    val statKinds = CounterType.KNOWN.filter { statSpelling.matches(it.printed) }

    describe("the stat counters") {

        it("finds all eleven by their printed shape") {
            statKinds.size shouldBe 11
            statKinds shouldContainAll listOf(CounterType.PLUS_ONE_PLUS_ONE, CounterType.MINUS_TWO_MINUS_TWO)
        }

        it("reads every stat kind back from its printed spelling") {
            statKinds.filter { CounterType.of(it.printed) != it }.shouldBeEmpty()
        }

        it("spells the asymmetric kinds that once regressed") {
            CounterType.of("+2/+0") shouldBe CounterType.PLUS_TWO_PLUS_ZERO
            CounterType.of("+0/+2") shouldBe CounterType.PLUS_ZERO_PLUS_TWO
            CounterType.of("-1/-0") shouldBe CounterType.MINUS_ONE_MINUS_ZERO
            CounterType.MINUS_ZERO_MINUS_ONE.printed shouldBe "-0/-1"
        }
    }
})
