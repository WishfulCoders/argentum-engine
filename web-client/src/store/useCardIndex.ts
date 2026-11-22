/**
 * The `/api/cards` catalogue as a name → summary index, fetched once per page and shared.
 *
 * The lobby's deck picker needs it for validation, stats and its deck tiles' covers, and a lobby
 * can mount several pickers at once (your seat, an AI seat). The catalogue is large, so one
 * request serves them all. Surfaces that only need a deck's face use `useDeckSummaries` instead.
 */
import { useEffect, useState } from 'react'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'

export type CardIndex = Record<string, CardSummary>

const EMPTY: CardIndex = {}

let cached: CardIndex | null = null
let inFlight: Promise<CardIndex> | null = null

function load(): Promise<CardIndex> {
  if (cached) return Promise.resolve(cached)
  inFlight ??= fetch('/api/cards')
    .then((r) => (r.ok ? (r.json() as Promise<CardSummary[]>) : []))
    .catch(() => [] as CardSummary[])
    .then((list) => {
      const index: CardIndex = {}
      for (const c of list) index[c.name] = c
      // An empty answer (server hiccup) isn't cached, so the next mount retries.
      if (list.length > 0) cached = index
      inFlight = null
      return index
    })
  return inFlight
}

/** The card index — empty until it loads, then stable for the rest of the page's life. */
export function useCardIndex(): CardIndex {
  const [index, setIndex] = useState<CardIndex>(cached ?? EMPTY)
  useEffect(() => {
    if (cached) return
    let cancelled = false
    void load().then((next) => { if (!cancelled) setIndex(next) })
    return () => { cancelled = true }
  }, [])
  return index
}
