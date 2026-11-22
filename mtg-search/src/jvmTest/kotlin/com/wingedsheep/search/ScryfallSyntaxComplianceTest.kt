package com.wingedsheep.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Compliance tests against the published Scryfall syntax reference at
 * https://scryfall.com/docs/syntax — every example query in that document
 * is exercised here so we know exactly which features we support, which we
 * accept-but-no-op, and which we reject with a clean parse error.
 *
 * Three groups:
 *   1. SUPPORTED — examples whose Scryfall semantics we implement. We assert
 *      the predicate runs without errors and produces a sensible result on
 *      the [Fixtures.CARDS] catalog.
 *   2. PARSED-BUT-NO-OP — syntactic features we accept (so users coming from
 *      Scryfall don't see "Unknown filter" surprises) but whose data we don't
 *      stamp onto our catalog yet (loyalty, frame, language, …). These are
 *      expected to either match nothing or report a "not surfaced" message.
 *   3. UNSUPPORTED — features we deliberately don't implement (artist /
 *      flavor / watermark / cube / display modifiers / price / … — the user
 *      explicitly excluded these from scope). The parser MUST emit an
 *      "Unknown filter" diagnostic with a span. Silent fallthrough is the
 *      anti-pattern this test guards against.
 *
 * When we add support for one of the parsed-but-no-op or unsupported keys,
 * move its test from group 3 to group 1 with a real assertion.
 */
class ScryfallSyntaxComplianceTest : StringSpec({

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    fun parse(q: String) = SearchService.parse(q)
    fun run(q: String): List<String> = SearchService.search(Fixtures.CARDS, q).map { it.name }
    fun ok(q: String) {
        val r = parse(q)
        r.errors shouldBe emptyList()
    }
    fun rejected(q: String, hint: String = "Unknown filter") {
        val r = parse(q)
        // Rejected queries must surface at least one diagnostic. We don't pin
        // the exact wording (suggestions move with the dictionary), only that
        // a relevant error is present and the user is not left guessing.
        r.errors.shouldNotBe(emptyList<ParseError>())
        r.errors.any { it.message.contains(hint, ignoreCase = true) || it.message.contains("Unsupported", ignoreCase = true) } shouldBe true
    }

    // =======================================================================
    // 1. SUPPORTED — every example we implement
    // =======================================================================

    // --- Color & color identity -------------------------------------------
    "color: c (single letter)" { ok("c:r"); run("c:r") shouldContain "Lightning Bolt" }
    "color: alias `color:`" { ok("color:blue"); run("color:blue") shouldContain "Counterspell" }
    "color: alias `id:`" { ok("id:r"); run("id:r") shouldContain "Lightning Bolt" }
    "color: alias `identity:`" { ok("identity:r") }
    "color: multiple letters c:rg" { ok("c:rg") }
    "color: guild name azorius" { ok("c:azorius") }
    "color: shard name bant" { ok("c:bant") }
    "color: wedge name abzan" { ok("c:abzan") }
    "color: colorless" { ok("c:colorless"); ok("c:c") }
    "color: multicolor" { ok("c:m"); ok("c:multicolor") }
    "color: count `c=2`" { ok("c=2"); run("c=2") shouldContain "Niv-Mizzet, Parun" }
    "color: comparator `color>=uw -c:red`" { ok("color>=uw -c:red") }
    "color: identity for lands" { ok("id:c t:land") }
    "color: c=2 is:bear" { ok("c=2 is:bear") }
    "color: id<=esper t:instant" { ok("id<=esper t:instant") }

    // --- Card types --------------------------------------------------------
    "type: t:legendary" { ok("t:legendary"); run("t:legendary") shouldContain "Niv-Mizzet, Parun" }
    "type: alias `type:`" { ok("type:creature") }
    "type: t:fish (subtype substring)" { ok("t:fish") }
    "type: t:merfolk t:legend (composite)" { ok("t:merfolk t:legend") }
    "type: t:goblin -t:creature (negated)" { ok("t:goblin -t:creature") }

    // --- Card text (oracle) -----------------------------------------------
    "oracle: o:draw t:creature" { ok("o:draw t:creature") }
    "oracle: alias `oracle:`" { ok("oracle:flying") }
    "oracle: kw:flying" { ok("kw:flying"); run("kw:flying") shouldContain "Serra Angel" }
    "oracle: alias keyword:" { ok("keyword:flying") }
    "oracle: kw:flying -t:creature" { ok("kw:flying -t:creature") }
    "oracle: regex name:/\\bizzet\\b/" { ok("name:/\\bizzet\\b/"); run("name:/Niv/").shouldContain("Niv-Mizzet, Parun") }

    // --- Mana cost & mana value -------------------------------------------
    "mana: alias `m:`" { ok("m:{R}") }
    "mana: alias `mana`" { ok("mana:{G}") }
    "mana: literal {G}" { ok("m:{G}"); run("m:{G}") shouldContain "Llanowar Elves" }
    "mana: bare letters G" { ok("m:g") }
    "mana: hybrid {2/G} accepted literally" { ok("m:{2/G}") }
    "mana: phyrexian {R/P} accepted literally" { ok("m:{R/P}") }
    "mana: 2WW bare letters" { ok("m:3WW") }
    "mana: m:{G}{U}" { ok("m:{G}{U}") }
    "mana: m>3WU" { ok("m>3WU") }
    "mana value: alias `mv` and `manavalue`" { ok("mv:1"); ok("manavalue:1") }
    "mana value: cmc alias" { ok("cmc:1") }
    "mana value: comparators" { ok("mv>=5"); ok("mv<=2"); ok("mv!=3"); ok("mv<5"); ok("mv>0") }
    "mana value: c:u mv=5" { ok("c:u mv=5") }

    // --- Power / toughness -------------------------------------------------
    "pt: pow>=8" { ok("pow>=8") }
    "pt: pow>tou c:w t:creature (cross-field)" { ok("pow>tou c:w t:creature") }
    "pt: alias power: / toughness:" { ok("power:4"); ok("toughness:4") }

    // --- Layout ------------------------------------------------------------
    "layout: is:dfc" { ok("is:dfc"); run("is:dfc") shouldContain "Delver of Secrets" }
    "layout: is:mdfc" { ok("is:mdfc") }
    "layout: is:transform" { ok("is:transform") }
    "layout: layout:transform" { ok("layout:transform") }

    // --- Other is: shortcuts we support -----------------------------------
    "is: spell" { ok("is:spell") }
    "is: permanent" { ok("is:permanent") }
    "is: vanilla" { ok("is:vanilla") }
    "is: bear (2/2 for 2)" { ok("is:bear") }
    "is: legendary (supertype shortcut)" { ok("is:legendary") }
    "is: historic (legendary | artifact | saga)" { ok("is:historic") }
    "is: frenchvanilla" { ok("is:frenchvanilla") }
    "is: monocolor / multicolor / colorless" { ok("is:monocolor"); ok("is:multicolor"); ok("is:colorless") }

    // --- Rarity ------------------------------------------------------------
    "rarity: r:common" { ok("r:common"); run("r:common") shouldContain "Lightning Bolt" }
    "rarity: alias `rarity:`" { ok("rarity:common") }
    "rarity: every rarity name" {
        for (r in listOf("common", "uncommon", "rare", "mythic", "special", "bonus")) ok("r:$r")
    }
    "rarity: r:common t:artifact" { ok("r:common t:artifact") }
    "rarity: r>=r (comparators)" { ok("r>=r") }

    // --- Sets --------------------------------------------------------------
    "set: s:lea" { ok("s:lea"); run("s:lea") shouldContain "Lightning Bolt" }
    "set: aliases e: edition: set:" { ok("e:lea"); ok("edition:lea"); ok("set:lea") }

    // --- Format legality ---------------------------------------------------
    "format: f:standard" { ok("f:standard") }
    "format: alias `format:` and `legal:`" { ok("format:modern"); ok("legal:modern") }
    "format: every format name parses" {
        val formats = listOf(
            "standard", "future", "historic", "timeless", "gladiator", "pioneer",
            "pauper", "penny", "modern", "legacy", "commander", "oathbreaker",
            "standardbrawl", "brawl", "alchemy", "paupercommander", "duel",
            "oldschool", "premodern", "predh", "vintage",
        )
        for (f in formats) ok("f:$f")
    }
    "format: c:g t:creature f:pauper" { ok("c:g t:creature f:pauper") }

    // --- Names: exact + regex ---------------------------------------------
    "name: bareword (substring)" { ok("lightning") }
    "name: !exact" { ok("!fire"); run("!\"lightning bolt\"") shouldContain "Lightning Bolt" }
    "name: !\"sift through sands\"" { ok("!\"sift through sands\"") }
    "name: regex slashes" { ok("name:/^[a-z]+\$/") }
    "name: regex t:creature o:/^{T}:/" { ok("t:creature o:/^\\{T\\}:/") }

    // --- Boolean and grouping ---------------------------------------------
    "bool: t:fish or t:bird" { ok("t:fish or t:bird") }
    "bool: OR uppercase" { ok("t:fish OR t:bird") }
    "bool: parens with OR" { ok("t:legendary (t:goblin or t:elf)") }
    "bool: through (depths or sands or mists)" { ok("through (depths or sands or mists)") }
    "bool: leading dash negation" { ok("-fire c:r t:instant") }
    "bool: o:changeling -t:creature" { ok("o:changeling -t:creature") }
    "bool: not keyword" { ok("not t:creature") }
    "bool: deeply nested" { ok("(c:r or c:b) (t:creature or t:planeswalker) -is:legendary") }

    // =======================================================================
    // 2. PARSED-BUT-NOT-IMPLEMENTED — accepted but data not surfaced yet
    // =======================================================================
    //
    // These exist on Scryfall but our catalog doesn't carry the underlying
    // field. We accept the syntax (no "Unknown filter" surprise) and the
    // matcher returns nothing — adding the data later flips the test from
    // "matches nothing" to "matches the right card" without changing syntax.

    "loy: t:planeswalker loy=3 (loyalty not surfaced — matches nothing)" {
        ok("t:planeswalker loy=3")
        run("t:planeswalker loy=3") shouldBe emptyList()
    }
    "loy: alias `loyalty:`" { ok("loyalty:3") }

    // =======================================================================
    // 3. UNSUPPORTED — must emit a clean diagnostic, never silently match all
    // =======================================================================

    "rejected: artist a:" { rejected("a:proce") }
    "rejected: artist alias artist:" { rejected("artist:avon") }
    "rejected: artists>1" { rejected("artists>1") }
    "rejected: flavor ft:" { rejected("ft:mishra") }
    "rejected: flavor: alias" { rejected("flavor:mishra") }
    "rejected: watermark wm:" { rejected("wm:orzhov") }
    "rejected: watermark: alias" { rejected("watermark:orzhov") }
    "rejected: border:" { rejected("border:white") }
    "rejected: frame:" { rejected("frame:1993") }
    "rejected: stamp:" { rejected("stamp:oval") }
    "rejected: prices usd" { rejected("usd>=0.50") }
    "rejected: prices eur" { rejected("eur<5") }
    "rejected: prices tix" { rejected("tix>15.00") }
    "rejected: cheapest:" { rejected("cheapest:usd") }
    "rejected: cube:" { rejected("cube:vintage") }
    "rejected: game:" { rejected("game:paper") }
    "rejected: year=" { rejected("year=2025") }
    "rejected: date>=" { rejected("date>=2015-08-18") }
    "rejected: art tag" { rejected("art:squirrel") }
    "rejected: atag:" { rejected("atag:squirrel") }
    "rejected: arttag:" { rejected("arttag:squirrel") }
    "rejected: function:" { rejected("function:removal") }
    "rejected: otag:" { rejected("otag:removal") }
    "rejected: oracletag:" { rejected("oracletag:removal") }
    "rejected: lang:" { rejected("lang:japanese") }
    "rejected: language:" { rejected("language:japanese") }
    "rejected: in:" { rejected("in:rare") }
    "rejected: cn: collector number" { rejected("cn>50") }
    "rejected: number: alias" { rejected("number:50") }
    "rejected: block b:" { rejected("b:wwk") }
    "rejected: block: alias" { rejected("block:wwk") }
    "rejected: st: set type" { rejected("st:core") }
    "rejected: banned:" { rejected("banned:legacy") }
    "rejected: restricted:" { rejected("restricted:vintage") }
    "rejected: new: prefix" { rejected("new:art") }
    "rejected: has:" { rejected("has:watermark") }
    "rejected: include:" { rejected("include:extras") }
    "rejected: devotion:" { rejected("devotion:{u/b}{u/b}") }
    "rejected: produces:" { rejected("produces:wu") }
    "rejected: prints=" { rejected("prints=1") }
    "rejected: sets=" { rejected("sets=1") }
    "rejected: paperprints=" { rejected("paperprints=1") }
    "rejected: papersets=" { rejected("papersets=1") }
    "rejected: unique: display modifier" { rejected("unique:prints") }
    "rejected: order:" { rejected("order:cmc") }
    "rejected: direction:" { rejected("direction:asc") }
    "rejected: prefer:" { rejected("prefer:newest") }
    "rejected: display:" { rejected("display:grid") }
    "rejected: not: prefix (we use - or `not` keyword)" { rejected("not:reprint") }
    "rejected: parity manavalue:even" {
        // We accept `mv:even` syntactically as "value `even` for cmc" but
        // even/odd aren't numbers — the matcher complains.
        rejected("manavalue:even", hint = "Expected")
    }
    "rejected: is:reprint (print metadata not stamped)" { rejected("is:reprint", hint = "not yet supported") }
    "rejected: is:hybrid (mana-symbol metadata not stamped)" { rejected("is:hybrid", hint = "not yet supported") }
    "rejected: is:phyrexian" { rejected("is:phyrexian", hint = "not yet supported") }
    "rejected: is:funny" { rejected("is:funny", hint = "not yet supported") }
    "rejected: is:bikeland (named land cycle)" { rejected("is:bikeland", hint = "not yet supported") }
    "rejected: is:digital" { rejected("is:digital", hint = "not yet supported") }
    "rejected: is:promo" { rejected("is:promo", hint = "not yet supported") }
    "rejected: is:foil" { rejected("is:foil", hint = "not yet supported") }
})
