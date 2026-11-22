/**
 * The card search language — Argentum's Scryfall-style queries — for the web-client.
 *
 * There is one implementation and it is not here: it is the Kotlin `:mtg-search` module, which the
 * server runs on the JVM and which `just search-js` compiles to `src/generated/mtg-search/`. This
 * file only adapts that module to the deckbuilder's types, so every search box filters exactly as
 * the server would — and still locally, on every keystroke, with no round trip.
 *
 * To change what a query means (a new key, an operator, an error message), edit
 * `mtg-search/src/commonMain` and run `just search-js`. The tests beside this file run against the
 * compiled module, so they double as the browser-side check of the Kotlin.
 */
import * as search from '@/generated/mtg-search/mtg-search.mjs'
import type { CardSummary, PrintedNamePrinting } from '../cardFilter'

export type CardPredicate = (card: CardSummary) => boolean

export interface Span {
  /** Inclusive 0-based offset into the original query. */
  start: number
  /** Exclusive end offset. */
  end: number
}

/** Replacing `span` of the query with `text` applies the error's suggestion. */
export interface QueryFix {
  span: Span
  text: string
}

export interface ParseError {
  message: string
  span: Span
  /** "Did you mean …?" */
  suggestion?: string
  /** The suggestion as an edit the search bar can apply in one click. */
  fix?: QueryFix
}

export interface ParseResult {
  predicate: CardPredicate
  errors: ParseError[]
  /** `true` for a blank query, which matches every card. */
  empty: boolean
  /**
   * The query in plain words (`name contains "bolt" and power ≥ 4`), or null when it is blank or
   * has errors. Shown under the search bar so the reader can check how their query was read.
   */
  explanation: string | null
  /** Uses `or`, the `not` keyword, or parentheses — beyond what the filter chips round-trip. */
  advanced: boolean
  /** The set an `s:CODE` term pins every result to (uppercased), for showing that set's reprint art. */
  setFilter: string | null
  /**
   * The printing a card was matched by through its *printed* name ("ademi" → the Through the
   * Omenpaths printing of Spectacular Spider-Man), or null. Null itself when the query has no
   * name term, so callers can skip the scan.
   */
  printedNameMatch: ((card: CardSummary) => PrintedNamePrinting | null) | null
}

export function parseQuery(query: string): ParseResult {
  const compiled = search.compileQuery(query)
  return {
    predicate: (card) => compiled.matches(card),
    errors: compiled.errors.map(toParseError),
    empty: compiled.isEmpty,
    explanation: compiled.explanation ?? null,
    advanced: compiled.isAdvanced,
    setFilter: compiled.setFilter ?? null,
    printedNameMatch: compiled.constrainsName
      ? (card) => {
          const printed = card.printedNamePrintings
          if (!printed || printed.length === 0) return null
          const name = compiled.matchedPrintedName(card)
          return name == null ? null : (printed.find((p) => p.name === name) ?? null)
        }
      : null,
  }
}

/**
 * How [query] splits at the top: the spans of its top-level AND terms (`t:goblin`, a bracketed
 * group, a negation), and whether a new term appended to it would AND with all of it — false when
 * `or` sits at the top. The filter panel's chips work on these.
 */
export function queryStructure(query: string): { isConjunction: boolean; terms: Span[] } {
  const compiled = search.compileQuery(query)
  const flat = compiled.topLevelTermSpans
  const terms: Span[] = []
  for (let i = 0; i + 1 < flat.length; i += 2) terms.push({ start: flat[i]!, end: flat[i + 1]! })
  return { isConjunction: compiled.isConjunction, terms }
}

/** See {@link ParseResult.advanced}; cheaper when only this is needed. */
export function isAdvancedQuery(query: string): boolean {
  return search.isAdvancedQuery(query)
}

export interface SearchKey {
  aliases: string[]
  ops: string[]
  description: string
}

/** Every filter key the language understands, straight from the matcher registry. */
export const SEARCH_KEYS: readonly SearchKey[] = search.searchKeys().map((k) => ({
  aliases: [...k.aliases],
  ops: [...k.ops],
  description: k.description,
}))

/** Apply an error's one-click fix to the query it was reported against. */
export function applyFix(query: string, fix: QueryFix): string {
  return query.slice(0, fix.span.start) + fix.text + query.slice(fix.span.end)
}

function toParseError(e: search.QueryError): ParseError {
  const out: ParseError = { message: e.message, span: { start: e.start, end: e.end } }
  if (e.suggestion != null) out.suggestion = e.suggestion
  if (e.fix != null) out.fix = { span: { start: e.fix.start, end: e.fix.end }, text: e.fix.text }
  return out
}
