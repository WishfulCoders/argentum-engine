/**
 * The server's starter decks (`GET /api/decks/examples`), for surfaces that only need their names.
 *
 * The launch panel offers them as "Your deck" to someone with no decks of their own, so a first
 * game against the AI needs no detour through the lobby's deck picker. Fetched once per page and
 * shared: the list is static for a server build. The deck picker keeps its own fetch, since it
 * needs the full lists anyway.
 */
import { useEffect, useState } from 'react'

/** What the server says a starter deck is, at a glance (`DecksController.DeckSummaryDTO`). */
export interface StarterDeckSummary {
  /** W/U/B/R/G, most-represented first. */
  readonly colors: readonly string[]
  readonly cardCount: number
  readonly creatures: number
  readonly spells: number
  readonly lands: number
  /** Non-land cards per mana value 0..7 (7 = seven or more). */
  readonly curve: readonly number[]
  readonly coverCard: string | null
  readonly coverImageUri: string | null
  readonly keyCards: readonly string[]
}

export interface StarterDeck {
  readonly id: string
  readonly name: string
  readonly description: string
  /** A one-line tip on how to play it. Absent on an older server. */
  readonly note?: string | null
  readonly cards: Record<string, number>
  /** The format it is built for; null = no hint (a 60-card casual list). */
  readonly format?: string | null
  readonly commander?: string | null
  /** Absent on a server older than the summary. */
  readonly summary?: StarterDeckSummary | null
}

let cached: readonly StarterDeck[] | null = null
let inFlight: Promise<readonly StarterDeck[]> | null = null

function load(): Promise<readonly StarterDeck[]> {
  if (cached) return Promise.resolve(cached)
  inFlight ??= fetch('/api/decks/examples')
    .then((r) => (r.ok ? r.json() as Promise<StarterDeck[]> : []))
    .catch(() => [] as StarterDeck[])
    .then((list) => {
      cached = list
      inFlight = null
      return list
    })
  return inFlight
}

/** The starter decks, or null while they load. */
export function useStarterDecks(): readonly StarterDeck[] | null {
  const [decks, setDecks] = useState<readonly StarterDeck[] | null>(cached)
  useEffect(() => {
    if (cached) return
    let cancelled = false
    void load().then((list) => { if (!cancelled) setDecks(list) })
    return () => { cancelled = true }
  }, [])
  return decks
}

export function starterDeckSize(deck: StarterDeck): number {
  return Object.values(deck.cards).reduce((a, b) => a + b, 0)
}
