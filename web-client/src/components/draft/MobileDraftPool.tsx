import { useMemo } from 'react'
import type { SealedCardInfo } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { groupPool, getCmc, type PoolGrouping } from './draftPool'

/** Which half of the phone draft screen is showing. */
export type MobileDraftTab = 'pack' | 'pool'

const GROUP_DOT: Record<string, string> = {
  W: '#f9faf4', U: '#0e68ab', B: '#6a6a6a', R: '#d32f2f', G: '#388e3c', M: '#ffd700', C: '#888',
}

const GROUPINGS: ReadonlyArray<readonly [PoolGrouping, string]> = [
  ['color', 'Color'], ['type', 'Type'], ['curve', 'Curve'],
]

/**
 * Phone Pool tab: the drafted cards as stacked image columns. Each card shows its title bar and
 * the last card of a column shows in full; tapping a card opens [MobileCardPreview].
 */
export function MobilePoolView({
  cards,
  picksPerRound,
  grouping,
  onGroupingChange,
  onPreview,
}: {
  cards: readonly SealedCardInfo[]
  /** Cards taken per pick; that many of the latest cards get the NEW tag. */
  picksPerRound: number
  grouping: PoolGrouping
  onGroupingChange: (grouping: PoolGrouping) => void
  onPreview: (card: SealedCardInfo) => void
}) {
  const groups = useMemo(() => groupPool(cards, grouping), [cards, grouping])
  const newestPicks = useMemo(
    () => new Set(cards.slice(-Math.max(1, picksPerRound)).map((c) => c.name)),
    [cards, picksPerRound],
  )

  const stats = useMemo(() => {
    let creatures = 0
    const curve = [0, 0, 0, 0, 0, 0, 0, 0]
    for (const card of cards) {
      if (card.typeLine.toLowerCase().includes('creature')) creatures++
      curve[Math.min(getCmc(card), 7)]!++
    }
    return { creatures, spells: cards.length - creatures, curve, maxCurve: Math.max(1, ...curve) }
  }, [cards])

  if (cards.length === 0) {
    return (
      <div style={{ padding: '48px 16px', textAlign: 'center', color: '#666', fontSize: 14 }}>
        Cards you pick will show up here.
      </div>
    )
  }

  return (
    <div style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
      {/* Stats strip */}
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 14 }}>
        <PoolStat value={stats.creatures} label="Creatures" color="#8bc34a" />
        <PoolStat value={stats.spells} label="Spells" color="#4fc3f7" />
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 34, marginLeft: 'auto' }} aria-label="Mana curve">
          {stats.curve.map((count, cmc) => (
            <div key={cmc} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 13 }}>
              <span style={{ color: '#888', fontSize: 8, lineHeight: '10px' }}>{count || ''}</span>
              <div
                style={{
                  width: '100%',
                  height: count > 0 ? Math.max(2, (count / stats.maxCurve) * 16) : 0,
                  backgroundColor: '#f2b45c',
                  borderRadius: '1px 1px 0 0',
                }}
              />
              <span style={{ color: '#555', fontSize: 8, lineHeight: '10px' }}>{cmc >= 7 ? '7+' : cmc}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Grouping chips */}
      <div style={{ display: 'flex', gap: 6 }} role="group" aria-label="Group pool by">
        {GROUPINGS.map(([value, label]) => {
          const active = value === grouping
          return (
            <button
              key={value}
              onClick={() => onGroupingChange(value)}
              aria-pressed={active}
              style={{
                padding: '5px 12px',
                fontSize: 12,
                borderRadius: 14,
                border: active ? '1px solid rgba(79, 195, 247, 0.5)' : '1px solid #444',
                backgroundColor: active ? 'rgba(79, 195, 247, 0.15)' : 'transparent',
                color: active ? '#4fc3f7' : '#aaa',
                cursor: 'pointer',
              }}
            >
              {label}
            </button>
          )
        })}
      </div>

      {/* Stacked columns */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 8, alignItems: 'start' }}>
        {groups.map((group) => (
          <div key={group.key} style={{ minWidth: 0 }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 5,
                marginBottom: 4,
                color: '#999',
                fontSize: 11,
                fontWeight: 600,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {grouping === 'color' && (
                <span
                  style={{ width: 8, height: 8, borderRadius: '50%', flexShrink: 0, backgroundColor: GROUP_DOT[group.key] }}
                />
              )}
              {group.label} {group.total}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {group.entries.map(({ card, count }, i) => (
                <StackedPoolCard
                  key={card.name}
                  card={card}
                  count={count}
                  isLast={i === group.entries.length - 1}
                  isNewest={newestPicks.has(card.name)}
                  onClick={() => onPreview(card)}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function PoolStat({ value, label, color }: { value: number; label: string; color: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <span style={{ color, fontSize: 16, fontWeight: 700, lineHeight: 1.1 }}>{value}</span>
      <span style={{ color: '#777', fontSize: 10 }}>{label}</span>
    </div>
  )
}

function StackedPoolCard({
  card,
  count,
  isLast,
  isNewest,
  onClick,
}: {
  card: SealedCardInfo
  count: number
  isLast: boolean
  isNewest: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      aria-label={count > 1 ? `${card.name} ×${count}` : card.name}
      style={{
        position: 'relative',
        display: 'block',
        width: '100%',
        padding: 0,
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        aspectRatio: '488 / 680',
        // Percent margins resolve against the column width, so this leaves the top ~20% of the
        // column width (the card's title bar) showing under the next card.
        marginBottom: isLast ? 0 : '-119%',
      }}
    >
      <img
        src={getCardImageUrl(card.name, card.imageUri, 'normal')}
        alt={card.name}
        loading="lazy"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          borderRadius: 6,
          boxShadow: '0 -2px 6px rgba(0, 0, 0, 0.55)',
          display: 'block',
        }}
      />
      {(count > 1 || isNewest) && (
        <span style={{ position: 'absolute', top: 3, right: 3, display: 'flex', gap: 2 }}>
          {isNewest && <PoolCardBadge background="#4fc3f7">NEW</PoolCardBadge>}
          {count > 1 && <PoolCardBadge background="#ffd54f">×{count}</PoolCardBadge>}
        </span>
      )}
    </button>
  )
}

function PoolCardBadge({ background, children }: { background: string; children: React.ReactNode }) {
  return (
    <span
      style={{
        backgroundColor: background,
        color: '#000',
        fontSize: 9,
        fontWeight: 700,
        padding: '1px 4px',
        borderRadius: 3,
        lineHeight: 1.3,
      }}
    >
      {children}
    </span>
  )
}

/** Full-screen card view for a tapped pool card. Tapping anywhere closes it. */
export function MobileCardPreview({ card, onClose }: { card: SealedCardInfo; onClose: () => void }) {
  return (
    <div
      onClick={onClose}
      role="dialog"
      aria-label={card.name}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 1100,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 14,
        padding: 24,
      }}
    >
      <img
        src={getCardImageUrl(card.name, card.imageUri, 'large')}
        alt={card.name}
        style={{ width: 'min(86vw, 380px)', aspectRatio: '488 / 680', borderRadius: 14, objectFit: 'cover' }}
      />
      <span style={{ color: '#888', fontSize: 13 }}>Tap anywhere to close</span>
    </div>
  )
}

const TAB_ICONS: Record<MobileDraftTab, React.ReactNode> = {
  pack: (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <rect x="5" y="3" width="11" height="15" rx="2" />
      <path d="M9 21h9a2 2 0 0 0 2-2V7" />
    </svg>
  ),
  pool: (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <rect x="3" y="4" width="5" height="16" rx="1" />
      <rect x="10" y="4" width="5" height="16" rx="1" />
      <rect x="17" y="4" width="4" height="16" rx="1" />
    </svg>
  ),
}

/** Bottom tab bar switching the phone draft screen between the current pack and the pool. */
export function MobileDraftTabBar({
  tab,
  onChange,
  packsWaiting,
  pickedLabel,
}: {
  tab: MobileDraftTab
  onChange: (tab: MobileDraftTab) => void
  /** Packs waiting for this player (current + queued); badged on the Pack tab. */
  packsWaiting: number
  /** Picked-count text for the Pool tab, e.g. "17 / 45". */
  pickedLabel: string
}) {
  const tabs: ReadonlyArray<readonly [MobileDraftTab, string, string | null]> = [
    ['pack', 'Pack', null],
    ['pool', 'Pool', pickedLabel],
  ]
  return (
    <div
      role="tablist"
      style={{
        display: 'flex',
        flexShrink: 0,
        backgroundColor: 'rgba(10, 12, 20, 0.86)',
        borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        backdropFilter: 'blur(18px)',
        WebkitBackdropFilter: 'blur(18px)',
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
      }}
    >
      {tabs.map(([value, label, detail]) => {
        const active = value === tab
        return (
          <button
            key={value}
            role="tab"
            aria-selected={active}
            onClick={() => onChange(value)}
            style={{
              flex: 1,
              position: 'relative',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 2,
              padding: '7px 0 8px',
              border: 'none',
              background: 'none',
              color: active ? '#f2c97a' : '#9aa3b8',
              fontSize: 11,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {TAB_ICONS[value]}
            <span>
              {label}
              {detail && <span style={{ fontWeight: 500, marginLeft: 4, opacity: 0.85 }}>{detail}</span>}
            </span>
            {value === 'pack' && packsWaiting > 0 && (
              <span
                aria-label={`${packsWaiting} ${packsWaiting === 1 ? 'pack' : 'packs'} waiting`}
                style={{
                  position: 'absolute',
                  top: 3,
                  left: 'calc(50% + 8px)',
                  minWidth: 16,
                  padding: '0 4px',
                  borderRadius: 8,
                  backgroundColor: '#ff9800',
                  color: '#000',
                  fontSize: 10,
                  fontWeight: 700,
                  lineHeight: '16px',
                  textAlign: 'center',
                }}
              >
                {packsWaiting}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
