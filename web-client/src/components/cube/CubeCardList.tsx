/**
 * The cube's own contents — the editor's right column. A 400-card set cube is unusable as one flat
 * list, so it filters by name and sections by type, colour or rarity; each row carries its rarity
 * chip, cost, and copy count.
 */
import { useMemo, useState } from 'react'
import type { CubeRarity, SharedCubeEntry } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { cubeCardCount } from '@/store/cubeLibrary'
import { GROUP_BY_LABEL, groupCubeEntries, type CubeGroupBy } from './cubeGrouping'
import { resolveRarity } from './cubeModel'
import { RarityChip } from './RarityChip'
import type { CardHoverHandler } from './useCardHover'
import styles from './CubeEditor.module.css'

const GROUP_BYS: readonly CubeGroupBy[] = ['TYPE', 'COLOR', 'RARITY']

interface CubeCardListProps {
  readonly entries: readonly SharedCubeEntry[]
  readonly lookup: (name: string) => CardSummary | undefined
  readonly catalogReady: boolean
  /** Rarity decides packs (booster styles) — draw the chips at full strength. */
  readonly rarityMatters: boolean
  readonly onHover: CardHoverHandler
  /** Omit for a read-only list (the shared-cube page). */
  readonly onCount?: (name: string, count: number) => void
  readonly onRarity?: (name: string, rarity: CubeRarity | null) => void
  readonly emptyHint?: React.ReactNode
}

export function CubeCardList({
  entries,
  lookup,
  catalogReady,
  rarityMatters,
  onHover,
  onCount,
  onRarity,
  emptyHint,
}: CubeCardListProps) {
  const [filter, setFilter] = useState('')
  const [groupBy, setGroupBy] = useState<CubeGroupBy>('TYPE')

  const visible = useMemo(() => {
    const q = filter.trim().toLowerCase()
    return q ? entries.filter((e) => e.name.toLowerCase().includes(q)) : entries
  }, [entries, filter])

  const groups = useMemo(
    () => groupCubeEntries(visible, catalogReady ? lookup : () => undefined, groupBy),
    [visible, lookup, groupBy, catalogReady],
  )

  return (
    <>
      <div className={styles.listToolbar}>
        <input
          className={`${styles.input} ${styles.filterInput}`}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder={`Filter ${entries.length} cards`}
          aria-label="Filter the cube"
          disabled={entries.length === 0}
        />
        <div className={styles.segmented} role="radiogroup" aria-label="Group cards by">
          {GROUP_BYS.map((by) => (
            <button
              key={by}
              type="button"
              role="radio"
              aria-checked={groupBy === by}
              className={styles.segment}
              onClick={() => setGroupBy(by)}
            >
              {GROUP_BY_LABEL[by]}
            </button>
          ))}
        </div>
      </div>
      <div className={styles.scroll}>
        {entries.length === 0 ? (
          <div className={styles.emptyList}>{emptyHint}</div>
        ) : visible.length === 0 ? (
          <p className={styles.hint}>No card in the cube matches “{filter}”.</p>
        ) : (
          groups.map((group) => (
            <section key={group.label} aria-label={group.label}>
              <h4 className={styles.groupHeader}>
                <span>{group.label}</span>
                <span>{cubeCardCount(group.entries)}</span>
              </h4>
              <ul className={styles.rowList}>
                {group.entries.map((entry) => {
                  const card = lookup(entry.name)
                  const missing = catalogReady && !card
                  return (
                    <li key={entry.name} className={styles.row}>
                      <RarityChip
                        cardName={entry.name}
                        resolved={resolveRarity(entry, card?.rarity)}
                        muted={!rarityMatters}
                        {...(onRarity ? { onChange: (r: CubeRarity | null) => onRarity(entry.name, r) } : {})}
                      />
                      <span
                        className={`${styles.rowName} ${missing ? styles.rowMissing : ''}`}
                        title={missing ? 'Not implemented yet' : entry.name}
                        onMouseEnter={(e) => card && onHover(card, e)}
                        onMouseMove={(e) => card && onHover(card, e)}
                        onMouseLeave={() => onHover(null)}
                      >
                        {entry.name}
                      </span>
                      <span className={styles.rowMeta}>
                        {card?.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : null}
                      </span>
                      {onCount ? (
                        <div className={styles.countControls}>
                          <button
                            type="button"
                            className={styles.stepButton}
                            aria-label={entry.count === 1 ? `Remove ${entry.name}` : `One fewer ${entry.name}`}
                            title={entry.count === 1 ? 'Remove' : 'One fewer'}
                            onClick={() => onCount(entry.name, entry.count - 1)}
                          >
                            {entry.count === 1 ? '×' : '−'}
                          </button>
                          <span className={styles.count}>{entry.count}</span>
                          <button
                            type="button"
                            className={styles.stepButton}
                            aria-label={`One more ${entry.name}`}
                            title="One more"
                            onClick={() => onCount(entry.name, entry.count + 1)}
                          >
                            +
                          </button>
                        </div>
                      ) : (
                        entry.count > 1 && <span className={styles.count}>×{entry.count}</span>
                      )}
                    </li>
                  )
                })}
              </ul>
            </section>
          ))
        )}
      </div>
    </>
  )
}
