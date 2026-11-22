package com.wingedsheep.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LexerParserTest : StringSpec({

    "tokenises a simple key:value term" {
        val tok = Lexer.lex("t:creature").single()
        tok.kind shouldBe TokenKind.Term
        tok.key shouldBe "t"
        tok.op shouldBe Op.EQ
        tok.value shouldBe "creature"
        tok.negate shouldBe false
        tok.exact shouldBe false
        tok.regex shouldBe false
    }

    "a leading dash negates and the key span skips it" {
        val tok = Lexer.lex("-t:goblin").single()
        tok.negate shouldBe true
        tok.value shouldBe "goblin"
        tok.keySpan shouldBe Span(1, 2)
    }

    "recognises the exact-name shortcut" {
        val tok = Lexer.lex("!\"Lightning Bolt\"").single()
        tok.exact shouldBe true
        tok.key shouldBe null
        tok.value shouldBe "Lightning Bolt"
    }

    "recognises regex literals as values" {
        val tok = Lexer.lex("name:/^bolt/i").single()
        tok.regex shouldBe true
        tok.regexFlags shouldBe "i"
        tok.value shouldBe "^bolt"
    }

    "parens are their own tokens, even without whitespace, but literal inside quotes" {
        Lexer.lex("(c:r or c:b)").map { it.kind } shouldBe
            listOf(TokenKind.LParen, TokenKind.Term, TokenKind.Or, TokenKind.Term, TokenKind.RParen)
        Lexer.lex("name:\"who/what(when)\"").single().value shouldBe "who/what(when)"
    }

    "and/or/not are keywords only when standalone" {
        Lexer.lex("t:creature and t:goblin").map { it.kind } shouldBe
            listOf(TokenKind.Term, TokenKind.And, TokenKind.Term)
        Lexer.lex("t:not").single().value shouldBe "not"
    }

    "a bare /foo/ is a name, since slashes appear in card names" {
        val tok = Lexer.lex("/foo/").single()
        tok.key shouldBe null
        tok.regex shouldBe false
    }

    "empty input parses to no tree" {
        Parser.parse("").ast shouldBe null
        Parser.parse("   ").ast shouldBe null
    }

    "implicit AND flattens; OR binds looser; parens group" {
        Parser.parse("t:creature c:r cmc<=3").ast.shouldBeInstanceOf<AndNode>().children shouldHaveSize 3
        val or = Parser.parse("a b or c d").ast.shouldBeInstanceOf<OrNode>()
        or.children.map { it::class } shouldBe listOf(AndNode::class, AndNode::class)
        Parser.parse("(a or b) c").ast.shouldBeInstanceOf<AndNode>().children[0].shouldBeInstanceOf<OrNode>()
    }

    "structural errors are reported but the rest still parses" {
        val unmatched = Parser.parse("(t:creature")
        (unmatched.ast != null) shouldBe true
        unmatched.errors.single().message shouldBe "Unmatched `(`."
        Parser.parse("t:creature)").errors.single().message shouldBe "Unmatched `)`."
        Parser.parse("()").errors.single().message shouldBe "Empty group `()`."
    }

    "dash and not both lower to a NOT node" {
        Parser.parse("-t:creature").ast.shouldBeInstanceOf<NotNode>()
        Parser.parse("not t:creature").ast.shouldBeInstanceOf<NotNode>()
    }

    "advanced queries are the ones with or, not, or parens" {
        Parser.isAdvancedQuery("t:creature c:r cmc<=3") shouldBe false
        Parser.isAdvancedQuery("-t:creature") shouldBe false
        Parser.isAdvancedQuery("t:creature or t:planeswalker") shouldBe true
        Parser.isAdvancedQuery("(c:r or c:b) t:creature") shouldBe true
        Parser.isAdvancedQuery("not t:creature") shouldBe true
    }
})
