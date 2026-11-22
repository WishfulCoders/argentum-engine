/**
 * Deck snapshots for the deck builder's AI assistance: what Auto-build replaced, so the player can
 * undo it, and whether the deck has moved on since (also how card scores know they're stale).
 */

/** A deck as the builder stores it: non-land card names (one entry per copy) + basic-land counts. */
export interface DeckSnapshot {
  readonly deck: readonly string[]
  readonly landCounts: Readonly<Record<string, number>>
}

/** The deck before an Auto-build and the deck it produced; undo is offered while the deck is still `applied`. */
export interface AssistUndo {
  readonly previous: DeckSnapshot
  readonly applied: DeckSnapshot
}

/** Order-insensitive key for a deck; zero land counts don't count. */
export function deckKey(snapshot: DeckSnapshot): string {
  const cards = [...snapshot.deck].sort().join('\u0000')
  const lands = Object.entries(snapshot.landCounts)
    .filter(([, n]) => n > 0)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([name, n]) => `${name}=${n}`)
    .join('\u0000')
  return `${cards}\u0001${lands}`
}

export function sameDeck(a: DeckSnapshot, b: DeckSnapshot): boolean {
  return deckKey(a) === deckKey(b)
}
