package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.tooling.CardLinter
import com.wingedsheep.sdk.tooling.LintSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Corpus-wide gate for the structural card lint (sdk-analysis §1.1, [CardLinter]): every
 * registered card's name-based references — pipeline collections/variables, target bindings,
 * choice slots — must resolve. A typo'd `storeAs`/`from` pair, a `ContextTarget` index into the
 * wrong ability, or a `CastChoiceMade` with no declaring kicker used to be a silent no-op found
 * (at best) in playtesting; this test makes it a build failure naming the exact card and step.
 *
 * **Errors** fail the build unless the card is listed in
 * `src/test/resources/lint-allowlist.txt` (one `ErrorType|Card Name` per line) — intentional
 * exceptions become a visible, burn-downable file. Stale allowlist entries fail too, so the
 * file can only shrink. **Warnings** (cross-resolution reads, unused stores) are printed for
 * review but never fail.
 */
class CardLintTest : FunSpec({

    val allowlist: Set<String> =
        (javaClass.getResource("/lint-allowlist.txt")?.readText() ?: "")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()

    val findings = MtgSetCatalog.all.flatMap { set ->
        set.cards.flatMap { card ->
            CardLinter.lint(card).map { Triple(set.code, it::class.simpleName!!, it) }
        }
    }

    test("every card's pipeline variables, target bindings, and choice slots resolve") {
        val errors = findings.filter { (_, _, finding) -> finding.severity == LintSeverity.ERROR }
        val keyed = errors.map { (setCode, type, finding) ->
            "$type|${finding.cardName}" to "[$setCode] ${finding.message}"
        }
        val unexpected = keyed.filter { (key, _) -> key !in allowlist }.map { it.second }
        if (unexpected.isNotEmpty()) {
            println("=== CardLinter errors: ${unexpected.size} ===")
            unexpected.forEach { println("  $it") }
        }
        unexpected.shouldBeEmpty()
    }

    test("the lint allowlist contains no stale entries") {
        val errorKeys = findings
            .filter { (_, _, finding) -> finding.severity == LintSeverity.ERROR }
            .map { (_, type, finding) -> "$type|${finding.cardName}" }
            .toSet()
        allowlist.filter { it !in errorKeys }.shouldBeEmpty()
    }

    test("print lint warnings for review (never fails)") {
        val warnings = findings.filter { (_, _, f) -> f.severity == LintSeverity.WARNING }
        if (warnings.isNotEmpty()) {
            val byType = warnings.groupBy { it.second }
            println("=== CardLinter warnings: ${warnings.size} across ${byType.size} categories ===")
            for ((type, entries) in byType) {
                println("--- $type (${entries.size}) ---")
                entries.forEach { (setCode, _, f) -> println("  [$setCode] ${f.message}") }
            }
        }
    }
})
