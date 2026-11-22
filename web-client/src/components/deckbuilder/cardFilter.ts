/**
 * cardFilter — the deckbuilder's card shape plus the query language's entry points.
 *
 * The query language itself is the Kotlin `:mtg-search` module compiled to JavaScript (see
 * `./query`). This file:
 *   - re-exports the user-facing entry points,
 *   - declares the `CardSummary` shape (mirrors `CardsController.CardSummaryDTO`),
 *   - keeps the chip helpers (`addToken`/`removeToken`/`hasToken`/`toggleToken`/`appendTerm`)
 *     the filter panel uses to round-trip its chips with the free-text bar. They work on the
 *     query's top-level terms as the parser reads them, so they stay correct for any query.
 */

// ---------------------------------------------------------------------------
// Card shape — mirrors `CardsController.CardSummaryDTO` on the backend.
// ---------------------------------------------------------------------------

export interface PrintedNamePrinting {
  name: string
  setCode: string
  collectorNumber: string
  imageUri: string | null
  backFaceImageUri: string | null
}

export interface CardSummary {
  name: string
  manaCost: string
  cmc: number
  /** Printed mana-cost colors. Used for mana curve, pip stats, and the `cost:` operator. */
  colors: string[]
  /**
   * Color identity (CR 903.4) — drives the `c:` / `color:` filter. Includes oracle-text
   * colored symbols and basic-land-subtype colors, so a card with off-color activation costs
   * correctly surfaces under its full identity. The server stamps this from the authoritative
   * Scryfall override when one exists.
   */
  colorIdentity: string[]
  cardTypes: string[]
  supertypes: string[]
  subtypes: string[]
  basicLand: boolean
  rarity: string
  setCode: string | null
  collectorNumber: string | null
  /**
   * All sets this card has a printing in (canonical + reprints). Used by the `s:`/`set:`
   * query so a reprint surfaces under its new set code even when [setCode] still points
   * at the original printing's set. Defaults to empty for legacy fixtures.
   */
  printingSetCodes?: string[]
  /**
   * Printings whose printed name differs from [name] (Through the Omenpaths' "Ademi of the
   * Silkchutes" for Spectacular Spider-Man). The name matcher checks these too, and a search
   * that matched only a printed name shows that printing's art.
   */
  printedNamePrintings?: PrintedNamePrinting[]
  oracleText?: string | null
  /** Printed power as text — `"2"`, `"*"`, `"1+*"`. */
  power?: string | null
  toughness?: string | null
  /** Printed starting loyalty of a planeswalker; drives `loy:`. */
  loyalty?: string | null
  imageUri?: string | null
  keywords?: string[]
  /** Server-stamped legal formats (uppercase: STANDARD, MODERN, COMMANDER, …). */
  legalFormats?: string[]
  isDoubleFaced?: boolean
  backFaceName?: string | null
  backFaceImageUri?: string | null
  /**
   * Printed layout (`NORMAL`, `SPLIT`, `ADVENTURE`, …). `SPLIT` cards (Pain // Suffering, Rooms
   * like Unholy Annex // Ritual Chamber) have a single sideways-printed image, so the hover
   * preview rotates them 90° to read landscape. Defaults to `NORMAL` for legacy fixtures.
   */
  layout?: string
  /**
   * Front printed sideways — split layouts and battles (`CardDefinition.isLandscapePrint`). The
   * hover preview rotates it to landscape; a flipped battle's back face stays portrait.
   */
  isLandscape?: boolean
  /**
   * Whether the card can be a commander on its own (the server's `CommanderEligibility`: a
   * legendary creature, or a card that says it can be your commander). Absent on legacy fixtures.
   */
  canBeCommander?: boolean
  /**
   * The card's printed partner abilities (CR 702.124) — "Partner", "Partner—Friends forever",
   * "Partner with <name>", "Choose a Background", "Doctor's companion". Empty or absent when none.
   */
  partnerAbilities?: string[]
  /**
   * The card's companion condition (CR 702.139) as printed after "Companion —"; absent when it has
   * no companion ability. Its presence lets the sideboard offer to make it the deck's companion.
   */
  companionCondition?: string
}

import { queryStructure } from './query'

export type { CardPredicate, ParseResult, ParseError } from './query'
export { isAdvancedQuery, parseQuery } from './query'

// ---------------------------------------------------------------------------
// Menu ↔ query helpers
//
// The filter panel toggles call these to keep the text query in sync with the chips. They work
// on the query's *top-level* terms, as the shared parser reads them: in
// `t:goblin (c:r or c:g)` the terms are `t:goblin` and `(c:r or c:g)`, so a chip never reaches
// inside a group the user wrote. Adding a term to a query with `or` at the top brackets it first
// — `t:elf or t:goblin` + `c:g` is `(t:elf or t:goblin) c:g` — so a chip always narrows the
// whole search, and the panel works whatever was typed.
// ---------------------------------------------------------------------------

interface TermSpan { start: number; end: number; text: string }

function topLevelTerms(query: string): TermSpan[] {
  return queryStructure(query).terms.map((t) => ({ ...t, text: query.slice(t.start, t.end) }))
}

/** The query's top-level AND terms, as typed. */
export function queryTerms(query: string): string[] {
  return topLevelTerms(query).map((t) => t.text)
}

/** AND [term] onto the whole query, bracketing it first when `or` sits at the top. */
export function appendTerm(query: string, term: string): string {
  const q = query.trim()
  if (!q) return term
  return queryStructure(q).isConjunction ? `${q} ${term}` : `(${q}) ${term}`
}

export function addToken(query: string, token: string): string {
  return hasToken(query, token) ? query : appendTerm(query, token)
}

/** Remove every top-level term matching [matches] (default: equal to a token). */
export function removeTerms(query: string, matches: (term: string) => boolean): string {
  let next = query
  for (const t of topLevelTerms(query).reverse()) {
    if (matches(t.text)) next = next.slice(0, t.start) + next.slice(t.end)
  }
  return next.replace(/\s+/g, ' ').trim()
}

export function removeToken(query: string, token: string): string {
  return removeTerms(query, (t) => t === token)
}

export function hasToken(query: string, token: string): boolean {
  return queryTerms(query).includes(token)
}

export function toggleToken(query: string, token: string): string {
  return hasToken(query, token) ? removeToken(query, token) : addToken(query, token)
}
