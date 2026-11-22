/**
 * At-a-glance summaries of your own decks (`POST /api/decks/summaries`) — cover art, colours,
 * curve, creature/spell/land split — the same shape the starter decks carry.
 *
 * For surfaces that show decks without the card catalogue: the landing page's launch panel. The
 * catalogue is ~13 MB of JSON; a summary is a few hundred bytes a deck, computed server-side from
 * the registry. Summaries are cached per page by the deck's *content* (list, commander, pins,
 * chosen cover), so editing a deck refetches it and nothing else.
 */
import { useEffect, useMemo, useState } from 'react'
import type { PrintingRef } from '@/types'
import type { SavedDeck } from '@/store/deckLibrary'
import { mergeCommanderIntoCards } from '@/store/deckLibrary'
import type { StarterDeckSummary } from '@/store/useStarterDecks'

/** The server's summary shape — shared with the starter decks. */
export type DeckGlance = StarterDeckSummary

interface SummaryRequestDeck {
  cards: Record<string, number>
  commander?: string
  printings?: Record<string, PrintingRef>
  coverCard?: string
}

const cache = new Map<string, DeckGlance>()

function requestFor(deck: SavedDeck): SummaryRequestDeck {
  const printings: Record<string, PrintingRef> = {}
  for (const e of deck.entries ?? []) if (e.printing) printings[e.name] = e.printing
  if (deck.commander && deck.commanderPrinting) printings[deck.commander] = deck.commanderPrinting
  if (deck.commander && deck.secondCommander && deck.secondCommanderPrinting) {
    printings[deck.secondCommander] = deck.secondCommanderPrinting
  }
  return {
    // Both commanders of a partner pair are part of the deck list the summary describes.
    cards: mergeCommanderIntoCards(deck.cards, deck.commander, deck.commander ? deck.secondCommander : null),
    ...(deck.commander ? { commander: deck.commander } : {}),
    ...(Object.keys(printings).length > 0 ? { printings } : {}),
    ...(deck.coverCard ? { coverCard: deck.coverCard } : {}),
  }
}

/** Deck id → summary, filled in as the batch answers. Decks the server couldn't summarize are absent. */
export function useDeckSummaries(decks: readonly SavedDeck[]): Record<string, DeckGlance> {
  const keyed = useMemo(
    () => decks.map((d) => {
      const request = requestFor(d)
      return { id: d.id, key: JSON.stringify(request), request }
    }),
    [decks],
  )
  const [, setTick] = useState(0)

  useEffect(() => {
    const missing = keyed.filter((k) => !cache.has(k.key))
    if (missing.length === 0) return
    let cancelled = false
    // Keyed by position on the wire; the content key stays client-side.
    const body = { decks: Object.fromEntries(missing.map((k, i) => [String(i), k.request])) }
    void fetch('/api/decks/summaries', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
      .then((r) => (r.ok ? (r.json() as Promise<Record<string, DeckGlance>>) : ({} as Record<string, DeckGlance>)))
      .then((answer) => {
        missing.forEach((k, i) => {
          const glance = answer[String(i)]
          if (glance) cache.set(k.key, glance)
        })
        if (!cancelled) setTick((t) => t + 1)
      })
      .catch(() => {})
    return () => { cancelled = true }
  }, [keyed])

  const out: Record<string, DeckGlance> = {}
  for (const k of keyed) {
    const glance = cache.get(k.key)
    if (glance) out[k.id] = glance
  }
  return out
}
