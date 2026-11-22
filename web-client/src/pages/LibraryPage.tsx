/**
 * Decks & Cubes — everything the player has built, at `/library` (`?tab=cubes` for the cube shelf).
 *
 * Decks and cubes are built with one tool (the builder and its Deck builder | Cube builder switch), so
 * they live on one shelf too: two tabs over the same gallery, the same search, sort and colour filter.
 * The shelf has two views over the same entries — a grid of full-art tiles for browsing, and a compact
 * list for a long shelf or a phone. Either way, the whole tile or row opens the deck or cube in the
 * builder, and the actions beside it are always visible (an overflow menu where there's no room)
 * rather than hover-revealed, so they work on touch. A cube is played from here; a deck is played
 * from a lobby.
 *
 * The first entry of each shelf is the way to start a new one, so an empty shelf and a full one teach
 * the same thing. `/cubes` redirects here, and the builders' title links back.
 */
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CubePlayButton } from '@/components/cube/CubePlayButton'
import { boosterStyleInfo, cubeToText, isBoosterStyle, uniqueCubeName } from '@/components/cube/cubeModel'
import { ShareCubeDialog } from '@/components/cube/ShareCubeDialog'
import { DeckDetailsDialog } from '@/components/deck/DeckDetailsDialog'
import {
  COLOR_PIP_SYMBOL,
  DeckTile,
  deckBannerGradient,
  deckTileModel,
  formatAccent,
  heroArtUrl,
  type DeckTileCard,
} from '@/components/deck/DeckTile'
import { clearWorkingCopy } from '@/components/deckbuilder/BuilderModeSwitch'
import { COLOR_TOKENS, useCardCatalog } from '@/components/deckbuilder/browser'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { cubeCardCount } from '@/store/cubeLibrary'
import { mergeCommanderIntoCards } from '@/store/deckLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { type UnifiedDeck, useUnifiedDecks } from '@/store/useUnifiedDecks'
import { useSaveDeck } from '@/store/useSaveDeck'
import { labelForFormat, useDeckLegalFormats } from '@/utils/deckLegality'
import styles from '@/components/library/Library.module.css'

export type LibraryTab = 'decks' | 'cubes'
export type LibraryView = 'grid' | 'list'
type Sort = 'updated' | 'name' | 'size' | 'colors'

const LAST_TAB_KEY = 'argentum.library.tab'
const VIEW_KEY = 'argentum.library.view'

function readLastTab(): LibraryTab {
  try {
    return window.localStorage.getItem(LAST_TAB_KEY) === 'cubes' ? 'cubes' : 'decks'
  } catch {
    return 'decks'
  }
}

/** The remembered view, else the one that suits the screen: art tiles when there's room, a list on a phone. */
function readView(): LibraryView {
  try {
    const stored = window.localStorage.getItem(VIEW_KEY)
    if (stored === 'grid' || stored === 'list') return stored
  } catch {
    // Storage blocked: fall through to the screen-size default.
  }
  return typeof window !== 'undefined' && window.matchMedia?.('(max-width: 640px)').matches ? 'list' : 'grid'
}

/** One shelf row, whichever kind it is — what search, sort and the colour filter read. */
interface Shelved<T> {
  item: T
  name: string
  /** Searchable text besides the name: a deck's note, a cube's description. */
  blurb: string
  total: number
  colors: string[]
  hero: CardSummary | null
  updatedAt: number
}

export function LibraryPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const tabParam = searchParams.get('tab')
  const tab: LibraryTab = tabParam === 'cubes' || tabParam === 'decks' ? tabParam : readLastTab()

  const decks = useUnifiedDecks()
  const cubes = useUnifiedCubes()
  const { index, loading: catalogLoading } = useCardCatalog()

  const [filter, setFilter] = useState('')
  const [sort, setSort] = useState<Sort>('updated')
  const [colorFilter, setColorFilter] = useState<ReadonlySet<string>>(new Set())
  const [view, setView] = useState<LibraryView>(readView)

  useEffect(() => {
    try {
      window.localStorage.setItem(LAST_TAB_KEY, tab)
    } catch {
      // Storage blocked: the shelf just opens on Decks next time.
    }
  }, [tab])

  const chooseView = (next: LibraryView) => {
    setView(next)
    try {
      window.localStorage.setItem(VIEW_KEY, next)
    } catch {
      // Storage blocked: the choice holds for this visit only.
    }
  }

  const selectTab = (next: LibraryTab) => {
    if (next === tab) return
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        params.set('tab', next)
        return params
      },
      { replace: true },
    )
  }

  const deckRows = useMemo<Shelved<UnifiedDeck>[]>(
    () =>
      decks.decks.map((deck) => {
        const { total, colors, hero } = deckTileModel(deck, index)
        return { item: deck, name: deck.name, blurb: deck.note ?? '', total, colors, hero, updatedAt: deck.updatedAt }
      }),
    [decks.decks, index],
  )

  const cubeRows = useMemo<Shelved<UnifiedCube>[]>(
    () =>
      cubes.cubes.map((cube) => {
        const cards: Record<string, number> = {}
        for (const entry of cube.cards) cards[entry.name] = (cards[entry.name] ?? 0) + entry.count
        const { colors, hero } = deckTileModel({ cards }, index)
        return {
          item: cube,
          name: cube.name,
          blurb: cube.description ?? '',
          total: cubeCardCount(cube.cards),
          colors,
          hero,
          updatedAt: cube.updatedAt,
        }
      }),
    [cubes.cubes, index],
  )

  // Only the visible tab's rows are filtered and sorted, and only when an input changes.
  const shownRows = useMemo(
    () => shelve<UnifiedDeck | UnifiedCube>(tab === 'decks' ? deckRows : cubeRows, filter, colorFilter, sort),
    [tab, deckRows, cubeRows, filter, colorFilter, sort],
  )

  const toggleColor = (key: string) =>
    setColorFilter((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })

  const clearFilters = () => {
    setFilter('')
    setColorFilter(new Set())
  }

  const newDeck = () => {
    clearWorkingCopy('deck')
    navigate('/deckbuilder')
  }
  const newCube = (from?: 'set' | 'paste') => {
    clearWorkingCopy('cube')
    navigate(from ? `/cubes/new?from=${from}` : '/cubes/new')
  }

  const counts = { decks: decks.decks.length, cubes: cubes.cubes.length }
  const isLoggedIn = decks.isLoggedIn
  const filtering = filter.trim() !== '' || colorFilter.size > 0

  return (
    <PageShell title="Decks & Cubes" width="wide">
      <header className={styles.hero}>
        <div>
          <h1 className={p.h1}>Decks &amp; cubes</h1>
          <p className={`${p.lede} ${styles.lede}`}>
            Everything you build, on one shelf. Decks you bring to a game; cubes you draft or open as sealed.
          </p>
        </div>
        {/* Looking comes before building: the card browser is one click from the shelf. Starting a
            new deck or cube is the first entry of the shelf itself. */}
        <button type="button" className={`${p.button} ${styles.heroAction}`} onClick={() => navigate('/cards')}>
          <SearchIcon /> Browse cards
        </button>
      </header>

      <div className={styles.bar}>
        <div className={styles.tabs} role="tablist" aria-label="Library">
          {(['decks', 'cubes'] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="tab"
              id={`library-tab-${t}`}
              aria-controls="library-panel"
              aria-selected={tab === t}
              className={styles.tab}
              onClick={() => selectTab(t)}
            >
              {t === 'decks' ? <DeckIcon /> : <CubeIcon />}
              {t === 'decks' ? 'Decks' : 'Cubes'}
              <span className={styles.tabCount}>{counts[t]}</span>
            </button>
          ))}
        </div>

        <label className={styles.search}>
          <SearchIcon />
          <input
            type="search"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Escape' && filter) {
                e.preventDefault()
                setFilter('')
              }
            }}
            placeholder={tab === 'decks' ? 'Search decks…' : 'Search cubes…'}
            aria-label={tab === 'decks' ? 'Search decks by name or note' : 'Search cubes by name or description'}
          />
        </label>

        <div className={styles.colors} role="group" aria-label="Filter by colour">
          {COLOR_TOKENS.map(({ label, key }) => (
            <button
              key={key}
              type="button"
              className={styles.colorChip}
              aria-pressed={colorFilter.has(key)}
              aria-label={key.toLowerCase()}
              title={key.charAt(0) + key.slice(1).toLowerCase()}
              onClick={() => toggleColor(key)}
            >
              <ManaSymbol symbol={label} size={18} />
            </button>
          ))}
          <button
            type="button"
            className={styles.colorChip}
            aria-pressed={colorFilter.has('COLORLESS')}
            aria-label="colourless"
            title="Colourless"
            onClick={() => toggleColor('COLORLESS')}
          >
            <ManaSymbol symbol="C" size={18} />
          </button>
        </div>

        <div className={styles.barEnd}>
          <select className={styles.sort} value={sort} onChange={(e) => setSort(e.target.value as Sort)} aria-label="Sort">
            <option value="updated">Recently edited</option>
            <option value="name">Name (A→Z)</option>
            <option value="size">Card count</option>
            <option value="colors">Colour</option>
          </select>
          <div className={styles.viewToggle} role="group" aria-label="View">
            <button
              type="button"
              className={styles.viewButton}
              aria-pressed={view === 'grid'}
              aria-label="Grid view"
              title="Grid — art tiles"
              onClick={() => chooseView('grid')}
            >
              <GridIcon />
            </button>
            <button
              type="button"
              className={styles.viewButton}
              aria-pressed={view === 'list'}
              aria-label="List view"
              title="List — compact rows"
              onClick={() => chooseView('list')}
            >
              <ListIcon />
            </button>
          </div>
        </div>
      </div>

      <section id="library-panel" role="tabpanel" aria-labelledby={`library-tab-${tab}`} className={styles.panel}>
        {tab === 'decks' ? (
          <DeckShelf
            rows={shownRows as Shelved<UnifiedDeck>[]}
            library={decks}
            view={view}
            showStorage={isLoggedIn}
            filtering={filtering}
            onNew={newDeck}
            onClearFilters={clearFilters}
          />
        ) : (
          <CubeShelf
            rows={shownRows as Shelved<UnifiedCube>[]}
            library={cubes}
            view={view}
            showStorage={isLoggedIn}
            index={index}
            catalogReady={!catalogLoading}
            filtering={filtering}
            onNew={newCube}
            onClearFilters={clearFilters}
          />
        )}
      </section>

      {!isLoggedIn && counts[tab] > 0 && (
        <p className={styles.footnote}>
          <MonitorIcon /> Saved in this browser. Sign in to keep your {tab} in your account
          {tab === 'cubes' ? ' and share them by link' : ' on every device'}.
        </p>
      )}
    </PageShell>
  )
}

function shelve<T>(rows: Shelved<T>[], filter: string, colorFilter: ReadonlySet<string>, sort: Sort): Shelved<T>[] {
  const needle = filter.trim().toLowerCase()
  const kept = rows.filter((row) => {
    if (needle && !row.name.toLowerCase().includes(needle) && !row.blurb.toLowerCase().includes(needle)) return false
    if (colorFilter.size === 0) return true
    for (const key of colorFilter) {
      if (key === 'COLORLESS' ? row.colors.length === 0 : row.colors.includes(key)) return true
    }
    return false
  })
  return kept.sort((a, b) => {
    switch (sort) {
      case 'name':
        return a.name.localeCompare(b.name)
      case 'size':
        return b.total - a.total || a.name.localeCompare(b.name)
      case 'colors':
        return colorBucket(a.colors) - colorBucket(b.colors) || a.name.localeCompare(b.name)
      default:
        return b.updatedAt - a.updatedAt
    }
  })
}

// ---------------------------------------------------------------------------
// The view-independent shelf entry
// ---------------------------------------------------------------------------

interface ShelfAction {
  label: string
  icon: ReactNode
  onClick: () => void
}

/** Everything a grid tile or a list row shows for one deck or cube. */
interface ShelfEntry {
  key: string
  name: string
  /** A deck's note (italic) or a cube's description. */
  note?: string | undefined
  description?: string | undefined
  total: number
  colors: string[]
  hero: DeckTileCard | null
  format?: string | null
  formatTitle?: string | undefined
  /** Extra chips after the format: a cube's pack style, "Shared", a missing-cards warning. */
  chips?: ReactNode
  storage?: 'cloud' | 'local' | undefined
  updatedAt: number
  openTitle: string
  onOpen: () => void
  /** Secondary actions; Delete is added by the shelf, behind its confirmation. */
  actions: ShelfAction[]
  /** The one primary action besides opening it — a cube's Play button. */
  primary?: ReactNode
  confirmText: string
  onDelete: () => void
}

function Shelf({
  entries,
  view,
  ready,
  filtering,
  noun,
  start,
  onClearFilters,
}: {
  entries: ShelfEntry[]
  view: LibraryView
  ready: boolean
  filtering: boolean
  noun: 'decks' | 'cubes'
  start: StartProps
  onClearFilters: () => void
}) {
  const loading = !ready && entries.length === 0
  return (
    <>
      <ul className={view === 'grid' ? styles.grid : styles.list} aria-busy={loading || undefined}>
        {!filtering && (
          <li className={view === 'grid' ? styles.startItem : styles.startRowItem}>
            {view === 'grid' ? <StartTile {...start} /> : <StartRow {...start} />}
          </li>
        )}
        {loading ? (
          <Skeletons view={view} />
        ) : (
          entries.map((entry) =>
            view === 'grid' ? <GridItem key={entry.key} entry={entry} /> : <ListItem key={entry.key} entry={entry} />,
          )
        )}
      </ul>
      {ready && entries.length === 0 && filtering && <NoMatches noun={noun} onClear={onClearFilters} />}
    </>
  )
}

/** A full-art tile over its footer bar; the delete confirmation replaces the bar. */
function GridItem({ entry }: { entry: ShelfEntry }) {
  const [confirming, setConfirming] = useState(false)
  return (
    <li className={styles.item} data-confirming={confirming}>
      <DeckTile
        name={entry.name}
        note={entry.note}
        description={entry.description}
        total={entry.total}
        colors={entry.colors}
        hero={entry.hero}
        format={entry.format ?? null}
        formatTitle={entry.formatTitle}
        storage={entry.storage}
        chips={entry.chips}
        title={entry.openTitle}
        onClick={entry.onOpen}
      />
      {confirming ? (
        <DeleteConfirm entry={entry} onKeep={() => setConfirming(false)} />
      ) : (
        <div className={styles.footer}>
          <span className={styles.updated}>{updatedLabel(entry.updatedAt)}</span>
          <EntryActions entry={entry} onDelete={() => setConfirming(true)} />
          {entry.primary}
        </div>
      )}
    </li>
  )
}

/**
 * A compact row: thumbnail, name and note, chips, colours, count, last edit, actions. The name is
 * the row's button, stretched over the whole row, so any empty part of the row opens it too.
 */
function ListItem({ entry }: { entry: ShelfEntry }) {
  const [confirming, setConfirming] = useState(false)
  const artUrl = useMemo(() => heroArtUrl(entry.hero), [entry.hero])
  const subtitle = entry.note ?? entry.description
  return (
    <li className={styles.row} data-confirming={confirming}>
      <span className={styles.thumb} aria-hidden style={artUrl ? undefined : { background: deckBannerGradient(entry.colors) }}>
        {artUrl && <img src={artUrl} alt="" loading="lazy" decoding="async" />}
      </span>
      <span className={styles.rowMain}>
        <button type="button" className={styles.rowOpen} onClick={entry.onOpen} title={entry.openTitle}>
          {entry.name}
        </button>
        {subtitle && (
          <span className={styles.rowSub} data-note={entry.note ? true : undefined}>
            {subtitle}
          </span>
        )}
        <span className={styles.rowMetaCompact}>
          <Pips colors={entry.colors} />
          <span>{entry.total} cards</span>
        </span>
      </span>
      <span className={styles.rowChips}>
        {entry.format && <FormatChip format={entry.format} title={entry.formatTitle} />}
        {entry.chips}
      </span>
      <span className={styles.rowPips}>
        <Pips colors={entry.colors} />
      </span>
      <span className={styles.rowCount}>
        {entry.total}
        <span className={styles.rowCountUnit}> cards</span>
      </span>
      <span className={styles.rowUpdated} title={new Date(entry.updatedAt).toLocaleString()}>
        {shortUpdated(entry.updatedAt)}
      </span>
      {entry.storage ? <StorageMark storage={entry.storage} /> : <span className={styles.storageMark} aria-hidden />}
      {confirming ? (
        <DeleteConfirm entry={entry} onKeep={() => setConfirming(false)} />
      ) : (
        <span className={styles.rowEnd}>
          <EntryActions entry={entry} onDelete={() => setConfirming(true)} />
          {entry.primary}
        </span>
      )}
    </li>
  )
}

function DeleteConfirm({ entry, onKeep }: { entry: ShelfEntry; onKeep: () => void }) {
  return (
    <div className={styles.confirm} role="alert" aria-label={`Delete ${entry.name}`}>
      <span>{entry.confirmText}</span>
      <button type="button" className={styles.footerButton} onClick={onKeep} autoFocus>
        Keep
      </button>
      <button type="button" className={styles.dangerButton} onClick={entry.onDelete}>
        Delete
      </button>
    </div>
  )
}

/**
 * An entry's secondary actions: icon buttons where there's room, one "More" menu where there isn't
 * (CSS picks which by the shelf's width). Never hover-only — a touch screen has no hover.
 */
function EntryActions({ entry, onDelete }: { entry: ShelfEntry; onDelete: () => void }) {
  const all: Array<ShelfAction & { danger?: boolean }> = [
    ...entry.actions,
    { label: 'Delete', icon: <TrashIcon />, onClick: onDelete, danger: true },
  ]
  return (
    <>
      <span className={styles.actions}>
        {all.map((action) => (
          <button
            key={action.label}
            type="button"
            className={styles.iconButton}
            data-danger={action.danger || undefined}
            onClick={action.onClick}
            title={action.label}
            aria-label={`${action.label} ${entry.name}`}
          >
            {action.icon}
          </button>
        ))}
      </span>
      <MoreMenu name={entry.name} actions={all} />
    </>
  )
}

function MoreMenu({ name, actions }: { name: string; actions: Array<ShelfAction & { danger?: boolean }> }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLSpanElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    menuRef.current?.querySelector<HTMLButtonElement>('button')?.focus()
    const close = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const key = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false)
        rootRef.current?.querySelector<HTMLButtonElement>('button')?.focus()
        return
      }
      if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
      const items = Array.from(menuRef.current?.querySelectorAll<HTMLButtonElement>('button') ?? [])
      const at = items.indexOf(document.activeElement as HTMLButtonElement)
      const next = items[(at + (e.key === 'ArrowDown' ? 1 : items.length - 1)) % items.length]
      next?.focus()
      e.preventDefault()
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', key)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', key)
    }
  }, [open])

  return (
    <span ref={rootRef} className={styles.more}>
      <button
        type="button"
        className={styles.iconButton}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`More actions for ${name}`}
        title="More"
        onClick={() => setOpen((o) => !o)}
      >
        <MoreIcon />
      </button>
      {open && (
        <div ref={menuRef} className={styles.menu} role="menu" aria-label={name}>
          {actions.map((action) => (
            <button
              key={action.label}
              type="button"
              role="menuitem"
              className={styles.menuItem}
              data-danger={action.danger || undefined}
              onClick={() => {
                setOpen(false)
                action.onClick()
              }}
            >
              {action.icon}
              {action.label}
            </button>
          ))}
        </div>
      )}
    </span>
  )
}

function FormatChip({ format, title }: { format: string; title?: string | undefined }) {
  const accent = formatAccent(format)
  return (
    <span className={styles.chip} style={{ color: accent }} title={title}>
      {labelForFormat(format)}
    </span>
  )
}

function Pips({ colors }: { colors: string[] }) {
  return (
    <span className={styles.pips}>
      {colors.length > 0 ? (
        colors.map((c) => <ManaSymbol key={c} symbol={COLOR_PIP_SYMBOL[c] ?? 'C'} size={14} />)
      ) : (
        <ManaSymbol symbol="C" size={14} />
      )}
    </span>
  )
}

function StorageMark({ storage }: { storage: 'cloud' | 'local' }) {
  return (
    <span
      className={styles.storageMark}
      data-storage={storage}
      title={
        storage === 'cloud'
          ? 'Saved to your account — available on any device'
          : 'Saved only in this browser — not backed up to your account'
      }
      aria-label={storage === 'cloud' ? 'Saved to your account' : 'Saved in this browser'}
    >
      {storage === 'cloud' ? <CloudIcon /> : <MonitorIcon />}
    </span>
  )
}

// ---------------------------------------------------------------------------
// Decks
// ---------------------------------------------------------------------------

function DeckShelf({
  rows,
  library,
  view,
  showStorage,
  filtering,
  onNew,
  onClearFilters,
}: {
  rows: Shelved<UnifiedDeck>[]
  library: ReturnType<typeof useUnifiedDecks>
  view: LibraryView
  showStorage: boolean
  filtering: boolean
  onNew: () => void
  onClearFilters: () => void
}) {
  const navigate = useNavigate()
  const { save } = useSaveDeck()
  const [editing, setEditing] = useState<UnifiedDeck | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Server-authoritative legality for every deck, batched into one request and keyed on the library
  // rather than the filtered rows, so typing in the search box never re-asks.
  const legalityInput = useMemo(() => {
    const out: Record<string, Record<string, number>> = {}
    for (const deck of library.decks) out[deck.id] = mergeCommanderIntoCards(deck.cards, deck.commander ?? null)
    return out
  }, [library.decks])
  const legality = useDeckLegalFormats(legalityInput)

  // A cloud deck's id (`cloud:<n>`) names nothing in the deckbuilder's local library, so it opens
  // through the account-deck deep link instead; local decks open by their stored id.
  const open = (deck: UnifiedDeck) =>
    navigate(
      deck.cloudId != null
        ? `/deckbuilder?accountDeck=${deck.cloudId}`
        : `/deckbuilder/${encodeURIComponent(deck.id)}`,
    )

  async function duplicate(deck: UnifiedDeck) {
    setError(null)
    try {
      const { id: _id, updatedAt: _updatedAt, online: _online, cloudId: _cloudId, ...rest } = deck
      const name = uniqueCubeName(`${deck.name} copy`, library.decks.map((d) => d.name))
      await save({ ...rest, name })
      library.reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not duplicate the deck')
    }
  }

  async function remove(deck: UnifiedDeck) {
    setError(null)
    try {
      await library.removeDeck(deck)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the deck')
    }
  }

  const entries: ShelfEntry[] = rows.map(({ item: deck, total, colors, hero }) => {
    const legal = legality[deck.id] ?? []
    const format = deck.format ?? legal[0] ?? null
    return {
      key: deck.id,
      name: deck.name,
      note: deck.note,
      total,
      colors,
      hero,
      format,
      formatTitle: format
        ? deck.format
          ? `Saved as ${labelForFormat(format)}`
          : `Legal in ${labelForFormat(format)}`
        : undefined,
      storage: showStorage ? (deck.online ? 'cloud' : 'local') : undefined,
      updatedAt: deck.updatedAt,
      openTitle: `Open ${deck.name} in the deck builder`,
      onOpen: () => open(deck),
      actions: [
        { label: 'Rename', icon: <PencilIcon />, onClick: () => setEditing(deck) },
        { label: 'Duplicate', icon: <CopyIcon />, onClick: () => void duplicate(deck) },
      ],
      confirmText: 'Delete this deck?',
      onDelete: () => void remove(deck),
    }
  })

  return (
    <>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <Shelf
        entries={entries}
        view={view}
        ready={library.settled}
        filtering={filtering}
        noun="decks"
        onClearFilters={onClearFilters}
        start={{
          title: 'New deck',
          body: 'Search every implemented card, or start from a list or a ready-made deck.',
          onClick: onNew,
          links: [
            { label: 'Import a list', onClick: () => navigate('/deckbuilder?open=import') },
            { label: 'Start from an example', onClick: () => navigate('/deckbuilder?open=examples') },
          ],
        }}
      />
      {editing && (
        <DeckDetailsDialog
          name={editing.name}
          note={editing.note ?? ''}
          onCancel={() => setEditing(null)}
          onSave={({ name, note }) => {
            const deck = editing
            setEditing(null)
            void library.updateDetails(deck, { name, note }).catch((err: unknown) =>
              setError(err instanceof Error ? err.message : 'Could not rename the deck'),
            )
          }}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Cubes
// ---------------------------------------------------------------------------

function CubeShelf({
  rows,
  library,
  view,
  showStorage,
  index,
  catalogReady,
  filtering,
  onNew,
  onClearFilters,
}: {
  rows: Shelved<UnifiedCube>[]
  library: ReturnType<typeof useUnifiedCubes>
  view: LibraryView
  showStorage: boolean
  index: Record<string, CardSummary>
  catalogReady: boolean
  filtering: boolean
  onNew: (from?: 'set' | 'paste') => void
  onClearFilters: () => void
}) {
  const navigate = useNavigate()
  const [sharing, setSharing] = useState<UnifiedCube | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Unimplemented cards per cube, counted once per catalogue/library change rather than per render.
  const unresolvedById = useMemo(() => {
    const out = new Map<string, number>()
    if (!catalogReady) return out
    const known = new Set(Object.keys(index).map((n) => n.toLowerCase()))
    for (const cube of library.cubes) out.set(cube.id, cube.cards.filter((e) => !known.has(e.name.toLowerCase())).length)
    return out
  }, [catalogReady, index, library.cubes])

  const edit = (cube: UnifiedCube) => navigate(`/cubes/edit/${encodeURIComponent(cube.id)}`)

  async function duplicate(cube: UnifiedCube) {
    setError(null)
    try {
      const name = uniqueCubeName(`${cube.name} copy`, library.cubes.map((c) => c.name))
      await library.saveCube({ ...cubeToShared(cube), name })
      library.reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not duplicate the cube')
    }
  }

  async function remove(cube: UnifiedCube) {
    setError(null)
    try {
      await library.removeCube(cube)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the cube')
    }
  }

  const entries: ShelfEntry[] = rows.map(({ item: cube, total, colors, hero }) => {
    const unresolved = unresolvedById.get(cube.id) ?? 0
    const blockedReason = total === 0
      ? 'This cube is empty'
      : unresolved > 0
        ? `${unresolved} card${unresolved === 1 ? '' : 's'} in this cube aren’t implemented yet — edit it to drop them`
        : null
    const booster = isBoosterStyle(cube.boosterStyle)
    return {
      key: cube.id,
      name: cube.name,
      description: cube.description,
      total,
      colors,
      hero,
      // A shuffled cube is the default, so only the exception earns a chip.
      chips: (booster || cube.shareCode || unresolved > 0) && (
        <>
          {booster && (
            <span className={styles.chip} data-kind="booster" title="Packs are printed by rarity, like a real set">
              {boosterStyleInfo(cube.boosterStyle).label}
            </span>
          )}
          {cube.shareCode && (
            <span className={styles.chip} data-kind="shared" title="Anyone with the link can view and copy it">
              Shared
            </span>
          )}
          {unresolved > 0 && (
            <span className={styles.chip} data-kind="warning" title={blockedReason ?? undefined}>
              {unresolved} missing
            </span>
          )}
        </>
      ),
      storage: showStorage ? (cube.online ? 'cloud' : 'local') : undefined,
      updatedAt: cube.updatedAt,
      openTitle: `Open ${cube.name} in the cube builder`,
      onOpen: () => edit(cube),
      actions: [
        { label: 'Share', icon: <ShareIcon />, onClick: () => setSharing(cube) },
        { label: 'Duplicate', icon: <CopyIcon />, onClick: () => void duplicate(cube) },
      ],
      primary: (
        <CubePlayButton cube={cubeToShared(cube)} blockedReason={blockedReason} className={styles.play} panelAlign="end" />
      ),
      confirmText: cube.shareCode ? 'Delete this cube? Its share link stops working.' : 'Delete this cube?',
      onDelete: () => void remove(cube),
    }
  })

  return (
    <>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <Shelf
        entries={entries}
        view={view}
        ready={library.ready}
        filtering={filtering}
        noun="cubes"
        onClearFilters={onClearFilters}
        start={{
          title: 'New cube',
          body: 'Your own pool to draft or open as sealed — a classic shuffled cube, or a whole set opened as real boosters.',
          onClick: () => onNew(),
          links: [
            { label: 'Start from a set', onClick: () => onNew('set') },
            { label: 'Paste a list', onClick: () => onNew('paste') },
          ],
        }}
      />
      {sharing && (
        <ShareCubeDialog
          saved={sharing}
          dirty={false}
          isLoggedIn={library.isLoggedIn}
          text={cubeToText(sharing)}
          // A device-only cube moves into the account on its first share; `saveCube` routes it there.
          onSave={() => library.saveCube(cubeToShared(sharing))}
          share={library.share}
          unshare={library.unshare}
          onClose={() => setSharing(null)}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

interface StartProps {
  title: string
  body: string
  onClick: () => void
  links: Array<{ label: string; onClick: () => void }>
}

/** The first tile of the grid: start a new one, with the shortcuts that kind of thing has. */
function StartTile({ title, body, onClick, links }: StartProps) {
  return (
    <div className={styles.start}>
      <button type="button" className={styles.startMain} onClick={onClick}>
        <span className={styles.startIcon} aria-hidden>
          <PlusIcon size={22} />
        </span>
        <span className={styles.startTitle}>{title}</span>
        <span className={styles.startBody}>{body}</span>
      </button>
      <StartLinks links={links} />
    </div>
  )
}

/** The list view's first row: the same start, one line tall. */
function StartRow({ title, onClick, links }: StartProps) {
  return (
    <div className={styles.startRow}>
      <button type="button" className={styles.startRowMain} onClick={onClick}>
        <span className={styles.startRowIcon} aria-hidden>
          <PlusIcon size={18} />
        </span>
        {title}
      </button>
      <StartLinks links={links} />
    </div>
  )
}

function StartLinks({ links }: { links: StartProps['links'] }) {
  return (
    <div className={styles.startLinks}>
      {links.map((link) => (
        <button key={link.label} type="button" className={styles.startLink} onClick={link.onClick}>
          {link.label} <span aria-hidden>→</span>
        </button>
      ))}
    </div>
  )
}

function Skeletons({ view }: { view: LibraryView }) {
  return (
    <>
      {[0, 1, 2].map((i) => (
        <li key={i} className={view === 'grid' ? styles.skeleton : styles.skeletonRow} aria-hidden />
      ))}
    </>
  )
}

function NoMatches({ noun, onClear }: { noun: string; onClear: () => void }) {
  return (
    <div className={styles.noMatches}>
      No {noun} match the search.{' '}
      <button type="button" className={styles.startLink} onClick={onClear}>
        Clear filters
      </button>
    </div>
  )
}

function colorBucket(colors: string[]): number {
  if (colors.length === 0) return 99
  if (colors.length > 1) return 90 + colors.length
  return ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'].indexOf(colors[0]!)
}

/** "Edited 3 days ago" — coarse on purpose; the tile is no place for a timestamp. */
export function updatedLabel(updatedAt: number, now: number = Date.now()): string {
  const short = shortUpdated(updatedAt, now)
  return short ? `Edited ${short}` : ''
}

/** The age half of {@link updatedLabel} ("3 days ago"), for the list's narrow column. */
export function shortUpdated(updatedAt: number, now: number = Date.now()): string {
  if (!updatedAt) return ''
  const minutes = Math.max(0, Math.round((now - updatedAt) / 60_000))
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  if (days === 1) return 'yesterday'
  if (days < 30) return `${days} days ago`
  return new Date(updatedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

// ---------------------------------------------------------------------------
// Icons
// ---------------------------------------------------------------------------

function Svg({ size = 15, children }: { size?: number; children: ReactNode }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false">
      {children}
    </svg>
  )
}
const PlusIcon = ({ size }: { size?: number }) => <Svg size={size ?? 16}><path d="M12 5v14M5 12h14" /></Svg>
const SearchIcon = () => <Svg><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></Svg>
const PencilIcon = () => <Svg size={14}><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></Svg>
const CopyIcon = () => <Svg size={14}><rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15V5a2 2 0 0 1 2-2h10" /></Svg>
const TrashIcon = () => <Svg size={14}><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" /></Svg>
const ShareIcon = () => <Svg size={14}><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" /></Svg>
const MoreIcon = () => <Svg size={16}><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></Svg>
const MonitorIcon = () => <Svg size={13}><rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4" /></Svg>
const CloudIcon = () => <Svg size={14}><path d="M17.5 19a4.5 4.5 0 1 0-1.4-8.8A6 6 0 0 0 4.3 12.5 3.5 3.5 0 0 0 6.5 19Z" /></Svg>
const GridIcon = () => (
  <Svg size={16}>
    <rect x="3" y="3" width="7.5" height="7.5" rx="1.5" />
    <rect x="13.5" y="3" width="7.5" height="7.5" rx="1.5" />
    <rect x="3" y="13.5" width="7.5" height="7.5" rx="1.5" />
    <rect x="13.5" y="13.5" width="7.5" height="7.5" rx="1.5" />
  </Svg>
)
const ListIcon = () => (
  <Svg size={16}>
    <path d="M9 6h12M9 12h12M9 18h12" />
    <rect x="3" y="4.5" width="3" height="3" rx="0.5" />
    <rect x="3" y="10.5" width="3" height="3" rx="0.5" />
    <rect x="3" y="16.5" width="3" height="3" rx="0.5" />
  </Svg>
)
const DeckIcon = () => (
  <Svg size={16}>
    <rect x="7" y="3" width="12" height="16" rx="2" />
    <path d="M4 7v12a2 2 0 0 0 2 2h9" />
  </Svg>
)
const CubeIcon = () => (
  <Svg size={16}>
    <path d="M12 2 3 7v10l9 5 9-5V7Z" />
    <path d="m3 7 9 5 9-5M12 12v10" />
  </Svg>
)
