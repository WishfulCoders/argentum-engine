/**
 * A cube laid out like the deckbuilder's Deck view: the list in flowing columns, grouped by type,
 * colour or rarity, with a name filter above it — and {@link CubeRailPreview}, the hovered card large
 * in the left rail. The shared-cube page reads a cube this way; the builder's Cube view edits one
 * this way, which is the same list with copy counts and rarity chips made editable.
 *
 * Hover goes through a {@link HoverStore}, not the page's state: a hover then re-renders the rail
 * preview and two rows, not the page around them.
 */
import { memo, useMemo, useState } from 'react'
import type { CubeRarity, SharedCubeEntry } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { DeckHoverPreview } from '@/components/deckbuilder/DeckHoverPreview'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { type HoverStore, useHovered, useHoveredSelect } from '@/components/ui/hoverStore'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import { cubeCardCount } from '@/store/cubeLibrary'
import { GROUP_BY_LABEL, groupCubeEntries, type CubeGroup, type CubeGroupBy } from './cubeGrouping'
import { resolveRarity } from './cubeModel'
import { RarityChip } from './RarityChip'
import db from '@/components/deckbuilder/deckbuilder.module.css'
import styles from './SharedCube.module.css'

const GROUP_BYS: readonly CubeGroupBy[] = ['TYPE', 'COLOR', 'RARITY']

/** The filter and grouping state a {@link CubeColumns} list is drawn with. */
export function useCubeColumns(
  entries: readonly SharedCubeEntry[],
  lookup: (name: string) => CardSummary | undefined,
  catalogReady: boolean,
) {
  const [groupBy, setGroupBy] = useState<CubeGroupBy>('TYPE')
  const [filter, setFilter] = useState('')
  const groups = useMemo(() => {
    const needle = filter.trim().toLowerCase()
    const visible = needle ? entries.filter((e) => e.name.toLowerCase().includes(needle)) : entries
    return groupCubeEntries(visible, catalogReady ? lookup : () => undefined, groupBy)
  }, [entries, filter, lookup, groupBy, catalogReady])
  return { groupBy, setGroupBy, filter, setFilter, groups }
}

export type CubeColumnsState = ReturnType<typeof useCubeColumns>

/** Filter box and group-by switch; `children` sit between them (a card count, "Clear all"). */
export function CubeColumnsToolbar({
  state,
  total,
  children,
}: {
  state: CubeColumnsState
  total: number
  children?: React.ReactNode
}) {
  return (
    <div className={styles.listToolbar}>
      <input
        className={styles.filter}
        value={state.filter}
        onChange={(e) => state.setFilter(e.target.value)}
        placeholder={`Filter ${total} cards`}
        aria-label="Filter the cube"
        disabled={total === 0}
      />
      {children}
      <div className={styles.segmented} role="radiogroup" aria-label="Group cards by">
        {GROUP_BYS.map((by) => (
          <button
            key={by}
            type="button"
            role="radio"
            aria-checked={state.groupBy === by}
            className={styles.segment}
            onClick={() => state.setGroupBy(by)}
          >
            {GROUP_BY_LABEL[by]}
          </button>
        ))}
      </div>
    </div>
  )
}

export function CubeColumns({
  groups,
  filter,
  lookup,
  catalogReady,
  rarityMatters,
  hover,
  fallbackName,
  onCount,
  onRarity,
  emptyHint,
}: {
  groups: readonly CubeGroup[]
  filter: string
  lookup: (name: string) => CardSummary | undefined
  catalogReady: boolean
  rarityMatters: boolean
  /** The hovered card — shown in the rail, highlighted in the list. */
  hover: HoverStore<CardSummary>
  /** The card the rail shows while nothing is hovered, highlighted then instead. */
  fallbackName: string | null
  /** Omit both for a read-only list. */
  onCount?: (name: string, count: number) => void
  onRarity?: (name: string, rarity: CubeRarity | null) => void
  /** Shown instead of the columns when the cube itself is empty. */
  emptyHint?: React.ReactNode
}) {
  if (groups.length === 0) {
    return (
      <div className={db.deckCentric}>
        {filter.trim() ? <p className={styles.empty}>No card in the cube matches “{filter}”.</p> : emptyHint}
      </div>
    )
  }
  return (
    <div className={db.deckCentric}>
      <div className={db.deckCentricColumns}>
        {groups.map((group) => (
          <section key={group.label} className={`${db.deckCentricGroup} ${styles.group}`} aria-label={group.label}>
            <h3 className={db.deckCentricGroupLabel}>
              {group.label} ({cubeCardCount(group.entries)})
            </h3>
            {group.entries.map((entry) => (
              <CubeRow
                key={entry.name}
                entry={entry}
                card={lookup(entry.name)}
                catalogReady={catalogReady}
                rarityMatters={rarityMatters}
                hover={hover}
                fallbackName={fallbackName}
                onCount={onCount}
                onRarity={onRarity}
              />
            ))}
          </section>
        ))}
      </div>
    </div>
  )
}

/**
 * One cube card. Memoized and subscribed to the hover store for its own highlight only, so moving
 * the pointer re-renders the row it left and the row it entered — not the cube.
 */
const CubeRow = memo(function CubeRow({
  entry,
  card,
  catalogReady,
  rarityMatters,
  hover,
  fallbackName,
  onCount,
  onRarity,
}: {
  entry: SharedCubeEntry
  card: CardSummary | undefined
  catalogReady: boolean
  rarityMatters: boolean
  hover: HoverStore<CardSummary>
  fallbackName: string | null
  onCount: ((name: string, count: number) => void) | undefined
  onRarity: ((name: string, rarity: CubeRarity | null) => void) | undefined
}) {
  const active = useHoveredSelect(hover, (hovered) => (hovered?.name ?? fallbackName) === entry.name)
  const show = () => card && hover.set(card)
  return (
    <div
      className={styles.row}
      data-editable={onCount ? true : undefined}
      data-active={active}
      data-missing={catalogReady && !card}
      title={catalogReady && !card ? 'Not implemented yet' : undefined}
      onMouseEnter={show}
      onClick={show}
    >
      <span className={styles.count}>{entry.count}</span>
      <RarityChip
        cardName={entry.name}
        resolved={resolveRarity(entry, card?.rarity)}
        muted={!rarityMatters}
        {...(onRarity ? { onChange: (r: CubeRarity | null) => onRarity(entry.name, r) } : {})}
      />
      <span className={styles.name}>{entry.name}</span>
      <span className={styles.cost}>
        {card?.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : null}
      </span>
      {onCount && (
        <span className={styles.steps}>
          <button
            type="button"
            className={styles.step}
            aria-label={entry.count === 1 ? `Remove ${entry.name}` : `One fewer ${entry.name}`}
            title={entry.count === 1 ? 'Remove' : 'One fewer'}
            onClick={(e) => {
              e.stopPropagation()
              onCount(entry.name, entry.count - 1)
            }}
          >
            {entry.count === 1 ? '×' : '−'}
          </button>
          <button
            type="button"
            className={styles.step}
            aria-label={`One more ${entry.name}`}
            title="One more"
            onClick={(e) => {
              e.stopPropagation()
              onCount(entry.name, entry.count + 1)
            }}
          >
            +
          </button>
        </span>
      )}
    </div>
  )
})

/**
 * The rail's large preview of the hovered card (or `fallback` until one is), with the F-to-flip a
 * double-faced card gets everywhere else. It subscribes to the hover store itself.
 */
export function CubeRailPreview({ hover, fallback }: { hover: HoverStore<CardSummary>; fallback: CardSummary | null }) {
  const card = useHovered(hover) ?? fallback
  // Keyed by name: a new card starts on its front face.
  return <RailCard key={card?.name ?? ''} card={card} />
}

function RailCard({ card }: { card: CardSummary | null }) {
  const dfc = useDfcHoverFlip(
    card
      ? {
          name: card.name,
          imageUri: card.imageUri ?? null,
          isDoubleFaced: card.isDoubleFaced ?? false,
          backFaceName: card.backFaceName ?? null,
          backFaceImageUri: card.backFaceImageUri ?? null,
        }
      : null,
  )
  return (
    <DeckHoverPreview
      name={card ? (dfc.displayName ?? card.name) : null}
      imageUri={card ? (dfc.displayImageUri ?? card.imageUri ?? null) : null}
      hint={dfc.hint}
      imageRotateDeg={dfc.imageRotateDeg(card)}
    />
  )
}
