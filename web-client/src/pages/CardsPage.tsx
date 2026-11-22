/**
 * The card browser, at `/cards` — every card Argentum can play, to look through without building
 * anything.
 *
 * The same search, filter menu and grid as the deckbuilder (one query language everywhere), laid out
 * for reading instead of adding: bigger tiles, filters open beside them, and a click opens the card
 * up close ({@link CardDetailDialog}) rather than putting a copy in a deck. From there a card leads
 * on — to its set, its types, or a new deck built around it.
 *
 * Everything lives in the URL — `q`, `sort` and the open `card` — so a search or a single card is a
 * link worth sending. Opening a card pushes a history entry, so Back closes it.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { CardDetailDialog } from '@/components/cards/CardDetailDialog'
import { clearWorkingCopy } from '@/components/deckbuilder/BuilderModeSwitch'
import {
  CardGrid,
  FilterSection,
  SearchBar,
  useCardBrowse,
  useCardCatalog,
  type SortMode,
} from '@/components/deckbuilder/browser'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import type { PrintingDTO } from '@/components/deckbuilder/PrintingPicker'
import { PageShell } from '@/components/ui/PageShell'
import styles from '@/components/cards/Cards.module.css'

type TileSize = 'regular' | 'large'
const TILE_SIZE_KEY = 'argentum.cards.tileSize'

function readTileSize(): TileSize {
  try {
    return window.localStorage.getItem(TILE_SIZE_KEY) === 'regular' ? 'regular' : 'large'
  } catch {
    return 'large'
  }
}

/** History state on the entry that opening a card pushed — closing then goes Back instead of piling up entries. */
interface OpenedHere {
  cardOpenedHere?: boolean
}

export function CardsPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { catalog, setInfos, index, loading, error } = useCardCatalog()

  const query = searchParams.get('q') ?? ''
  const sortParam = searchParams.get('sort')
  const sortMode: SortMode = sortParam === 'cmc' || sortParam === 'color' || sortParam === 'rarity' ? sortParam : 'name'
  const openName = searchParams.get('card')

  const updateParams = useCallback(
    (edit: (p: URLSearchParams) => void, opts: { push?: boolean; state?: OpenedHere } = {}) =>
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          edit(next)
          return next
        },
        { replace: !opts.push, ...(opts.state ? { state: opts.state } : {}) },
      ),
    [setSearchParams],
  )

  const setQuery = useCallback(
    (next: string) => updateParams((p) => (next ? p.set('q', next) : p.delete('q'))),
    [updateParams],
  )
  const setSortMode = useCallback(
    (next: SortMode) => updateParams((p) => (next === 'name' ? p.delete('sort') : p.set('sort', next))),
    [updateParams],
  )

  const browse = useCardBrowse(catalog, '', { query, setQuery, sortMode, setSortMode })
  const { filtered, displayed, overrides } = browse

  const [tileSize, setTileSize] = useState<TileSize>(readTileSize)
  useEffect(() => {
    try {
      window.localStorage.setItem(TILE_SIZE_KEY, tileSize)
    } catch {
      // Storage blocked: the browser just opens on large tiles next time.
    }
  }, [tileSize])

  // Phone layout: one pane at a time.
  const [pane, setPane] = useState<'cards' | 'filters'>('cards')

  // ── The open card ──────────────────────────────────────────────────────
  const openCard = useCallback(
    (card: CardSummary) => updateParams((p) => p.set('card', card.name), { push: true, state: { cardOpenedHere: true } }),
    [updateParams],
  )
  const closeCard = useCallback(() => {
    if ((location.state as OpenedHere | null)?.cardOpenedHere) navigate(-1)
    else updateParams((p) => p.delete('card'))
  }, [location.state, navigate, updateParams])

  const openCardSummary = openName ? index[openName] ?? null : null
  const position = useMemo(() => {
    if (!openName) return null
    const i = filtered.findIndex((c) => c.name === openName)
    return i < 0 ? null : i
  }, [filtered, openName])

  // Stepping swaps the card in place (replace), so Back still closes the dialog in one go. A step past
  // the loaded page loads the next one, keeping the grid behind the dialog in step with it.
  const stepTo = useCallback(
    (i: number) => {
      const card = filtered[i]
      if (!card) return
      if (i >= displayed.length) browse.showMore()
      updateParams((p) => p.set('card', card.name), { state: { cardOpenedHere: !!(location.state as OpenedHere | null)?.cardOpenedHere } })
    },
    [filtered, displayed.length, browse, updateParams, location.state],
  )

  // A chip in the dialog (a type, a set, a rarity) is a new search: close the card and show its results.
  const searchFromDetail = useCallback(
    (next: string) => {
      updateParams((p) => {
        p.delete('card')
        p.set('q', next)
      })
      setPane('cards')
    },
    [updateParams],
  )

  const buildDeck = useCallback(
    (card: CardSummary, printing: PrintingDTO | null, asCommander: boolean) => {
      const params = new URLSearchParams({ seed: card.name })
      if (asCommander) {
        params.set('as', 'commander')
        params.set('fmt', 'COMMANDER')
      }
      if (printing) params.set('print', `${printing.setCode}:${printing.collectorNumber}`)
      // A fresh deck, not whatever the builder was last left holding.
      clearWorkingCopy('deck')
      navigate(`/deckbuilder?${params.toString()}`)
    },
    [navigate],
  )

  const resultLabel = loading
    ? 'Loading…'
    : displayed.length < filtered.length
      ? `Showing ${displayed.length.toLocaleString()} of ${filtered.length.toLocaleString()}`
      : `${filtered.length.toLocaleString()} card${filtered.length === 1 ? '' : 's'}`

  return (
    <PageShell title="Cards" fit width="full" plain>
      <div className={styles.toolbar}>
        <div className={styles.intro}>
          <h1 className={styles.title}>Card browser</h1>
          <p className={styles.lede}>
            {catalog.length > 0 ? `${catalog.length.toLocaleString()} cards` : 'Every card'} you can play in Argentum.
            Click one for a closer look.
          </p>
        </div>
        <div className={styles.paneTabs} role="tablist" aria-label="Card browser panes">
          <button type="button" role="tab" className={styles.paneTab} aria-selected={pane === 'cards'} onClick={() => setPane('cards')}>
            Cards
          </button>
          <button type="button" role="tab" className={styles.paneTab} aria-selected={pane === 'filters'} onClick={() => setPane('filters')}>
            Filters{query.trim() && <span className={styles.paneDot} aria-label="(active)" />}
          </button>
        </div>
      </div>

      <div className={styles.workspace} data-pane={pane}>
        <aside className={styles.filtersPane} aria-label="Filters">
          <FilterSection query={query} onQueryChange={setQuery} catalog={catalog} setInfos={setInfos} advanced={browse.advanced} />
        </aside>

        <main className={styles.resultsPane}>
          <SearchBar
            query={query}
            onQueryChange={setQuery}
            sortMode={sortMode}
            onSortChange={setSortMode}
            errors={browse.errors}
            explanation={browse.explanation}
            resultLabel={resultLabel}
            actionHint={
              <>
                <kbd>Click</kbd> a card to see it up close
              </>
            }
            filterChip={<TileSizeToggle value={tileSize} onChange={setTileSize} />}
          />
          {error ? (
            <div className={styles.notice} role="alert">Couldn’t load the card catalogue: {error}</div>
          ) : loading ? (
            <div className={styles.notice}>Loading cards…</div>
          ) : (
            <CardGrid
              cards={displayed}
              deckCards={NO_COUNTS}
              onAdd={openCard}
              hasMore={browse.hasMore}
              onShowMore={browse.showMore}
              large={tileSize === 'large'}
              tileLabel={(c) => `Show ${c.name}`}
            />
          )}
        </main>
      </div>

      {openCardSummary && (
        <CardDetailDialog
          card={openCardSummary}
          setInfos={setInfos}
          preferredPrinting={overrides[openCardSummary.name] ?? null}
          position={position != null ? { index: position + 1, total: filtered.length } : null}
          onPrev={position != null && position > 0 ? () => stepTo(position - 1) : null}
          onNext={position != null && position < filtered.length - 1 ? () => stepTo(position + 1) : null}
          onClose={closeCard}
          onSearch={searchFromDetail}
          onBuildDeck={(printing, asCommander) => buildDeck(openCardSummary, printing, asCommander)}
        />
      )}
    </PageShell>
  )
}

const NO_COUNTS: Record<string, number> = {}

function TileSizeToggle({ value, onChange }: { value: TileSize; onChange: (v: TileSize) => void }) {
  return (
    <div className={styles.sizeToggle} role="group" aria-label="Card size">
      <button type="button" aria-pressed={value === 'regular'} onClick={() => onChange('regular')} title="Smaller cards — more per row">
        <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
          {[1, 5.5, 10].flatMap((x) => [1, 5.5, 10].map((y) => <rect key={`${x}-${y}`} x={x} y={y} width="3" height="3" rx="0.6" fill="currentColor" />))}
        </svg>
      </button>
      <button type="button" aria-pressed={value === 'large'} onClick={() => onChange('large')} title="Bigger cards">
        <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
          {[1, 7.5].flatMap((x) => [1, 7.5].map((y) => <rect key={`${x}-${y}`} x={x} y={y} width="5.5" height="5.5" rx="1" fill="currentColor" />))}
        </svg>
      </button>
    </div>
  )
}
