/**
 * The state behind browsing the catalogue: the query (and what it parses to), the sort, the paged
 * slice on screen, and the reprint art an `s:` filter calls for.
 *
 * {@link CardBrowser} lays it out as one pane; the cube builder spreads the same state across the
 * deckbuilder's three columns (filters left, grid centre), and the card browser page keeps the query
 * and sort in its URL by passing them in as `controlled`. Keeping it here means all of them browse
 * cards identically — same query language, same sort, same set-filter art.
 */
import { useDeferredValue, useEffect, useMemo, useState } from 'react'
import { parseQuery, type CardSummary } from '../cardFilter'
import { sortCards, type SortMode } from './cardSort'
import { useArtOverrides, useCardsWithSetArt, useSetPrintingOverride } from './useSetPrintingOverride'

const PAGE_SIZE = 120

/** Query and sort owned by the caller (e.g. kept in the URL) instead of by the hook. */
export interface ControlledBrowse {
  query: string
  setQuery: (next: string) => void
  sortMode: SortMode
  setSortMode: (next: SortMode) => void
}

export function useCardBrowse(catalog: readonly CardSummary[], initialQuery = '', controlled?: ControlledBrowse) {
  const [ownQuery, setOwnQuery] = useState(initialQuery)
  const [ownSortMode, setOwnSortMode] = useState<SortMode>('name')
  const query = controlled ? controlled.query : ownQuery
  const setQuery = controlled ? controlled.setQuery : setOwnQuery
  const sortMode = controlled ? controlled.sortMode : ownSortMode
  const setSortMode = controlled ? controlled.setSortMode : setOwnSortMode
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)

  // Errors and the plain-words reading follow every keystroke; the filtering itself trails at low
  // priority (`useDeferredValue`), so typing never waits on a 15k-card scan.
  const parseResult = useMemo(() => parseQuery(query), [query])
  const results = useDeferredValue(parseResult)
  const activeSetFilter = results.setFilter

  const predicate = results.predicate
  const filtered = useMemo(
    () => sortCards(catalog.filter(predicate), sortMode),
    [catalog, predicate, sortMode],
  )

  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode])

  // Only the visible page needs reprint art — asking for every match of a broad `s:` filter
  // would be a needlessly large request.
  const visibleNames = useMemo(
    () => filtered.slice(0, visibleCount).map((c) => c.name),
    [filtered, visibleCount],
  )
  const setOverrides = useSetPrintingOverride(activeSetFilter, visibleNames)
  const overrides = useArtOverrides(results.printedNameMatch, filtered, setOverrides)
  const withArt = useCardsWithSetArt(filtered, overrides)
  const displayed = useMemo(() => withArt.slice(0, visibleCount), [withArt, visibleCount])

  return {
    query,
    setQuery,
    sortMode,
    setSortMode,
    errors: parseResult.errors,
    explanation: parseResult.explanation,
    advanced: parseResult.advanced,
    filtered,
    displayed,
    /** Reprint art the active `s:` filter picked, by card name. */
    overrides,
    hasMore: displayed.length < filtered.length,
    showMore: () => setVisibleCount((c) => c + PAGE_SIZE),
  }
}
