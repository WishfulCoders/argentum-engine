/**
 * The whole implemented-card catalog (`/api/cards`), fetched once per page load and shared by every
 * cube view — the editor, the library's colour pips and the shared-cube page all read it, and none of
 * them should refetch a few thousand cards on mount.
 */
import { useEffect, useMemo, useState } from 'react'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'

let cache: CardSummary[] | null = null
let inflight: Promise<CardSummary[]> | null = null

function loadCatalog(): Promise<CardSummary[]> {
  inflight ??= fetch('/api/cards')
    .then((r) => (r.ok ? (r.json() as Promise<CardSummary[]>) : []))
    .then((list) => {
      cache = list
      return list
    })
    .catch(() => {
      inflight = null
      return []
    })
  return inflight
}

export interface CardCatalog {
  readonly cards: readonly CardSummary[]
  /** Lower-cased name → card. */
  readonly byName: ReadonlyMap<string, CardSummary>
  readonly ready: boolean
}

export function useCardCatalog(): CardCatalog {
  const [cards, setCards] = useState<CardSummary[]>(cache ?? [])

  useEffect(() => {
    if (cache) return
    let cancelled = false
    void loadCatalog().then((list) => {
      if (!cancelled) setCards(list)
    })
    return () => {
      cancelled = true
    }
  }, [])

  const byName = useMemo(() => {
    const map = new Map<string, CardSummary>()
    for (const card of cards) map.set(card.name.toLowerCase(), card)
    return map
  }, [cards])

  return { cards, byName, ready: cards.length > 0 }
}
