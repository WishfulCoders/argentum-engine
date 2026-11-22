import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState, useStackCards, useZoneCards } from '@/store/selectors.ts'
import { ante } from '@/types'
import type { ClientCard, ClientPlayer } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { handleImageError, useResponsiveContext } from './shared'
import { styles } from './styles'

/** Bronze stake palette — distinct from the gold Plotted pile and the commander's gold label. */
export const ANTE_HUE = '#e0a15e'
const ANTE_DEEP = '#a8692c'
const CARD_RATIO = 1.4
/** How many cards peek out behind the top one; more than this just raises the count badge. */
const MAX_PEEK = 2

/** A small CSS-drawn coin — reads as "stake" without leaning on an emoji's platform rendering. */
export function AnteCoin({ size = 10 }: { size?: number }) {
  return (
    <span
      aria-hidden
      style={{
        display: 'inline-block',
        width: size,
        height: size,
        borderRadius: '50%',
        flexShrink: 0,
        background: `radial-gradient(circle at 35% 30%, #ffe2b0 0%, ${ANTE_HUE} 45%, ${ANTE_DEEP} 100%)`,
        boxShadow: `inset 0 0 0 1px rgba(80, 40, 8, 0.55), 0 0 4px ${ANTE_HUE}88`,
      }}
    />
  )
}

/**
 * A player's **ante** (CR 407) — the cards they've wagered in a game played for ante.
 *
 * Sits beside the command zone in every board layout (your row, an opponent's row, multiplayer
 * cells). It is a single fixed-size pile slot no matter how many cards are staked — extra cards
 * peek out *inside* the slot and raise the count — so a card entering or leaving the ante never
 * reflows the battlefield next to it. Because `playingForAnte` is fixed for the whole game, the
 * slot is there from the first frame (an empty, dashed "Ante" slot until the ante is made).
 *
 * The ante is public: the top card shows face up and hovering it opens the usual card preview;
 * clicking opens a browser of every staked card. Cards in the ante are picked as targets through
 * the shared zone-card picker (see `utils/targeting.ts`), so this pile only *signals* targeting —
 * it glows while one of its cards is a legal target or is targeted by a spell on the stack.
 *
 * Renders nothing in a game that isn't played for ante.
 */
export function AnteZone({
  player,
  isOpponent = false,
  compact = false,
}: {
  player: ClientPlayer
  isOpponent?: boolean
  /** A multiplayer board cell — matches CommandZone's compact sizing. */
  compact?: boolean
}) {
  const playingForAnte = useGameStore((state) => selectGameState(state)?.playingForAnte ?? false)
  const cards = useZoneCards(ante(player.playerId))
  const hoverCard = useGameStore((state) => state.hoverCard)
  const validTargets = useGameStore((state) => state.targetingState?.validTargets)
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const [browsing, setBrowsing] = useState(false)

  if (!playingForAnte) return null

  const width = responsive.pileWidth
  const height = Math.round(width * CARD_RATIO)
  const radius = responsive.isMobile ? 4 : 6
  const narrow = responsive.isMobile || width < 56
  const topCard = cards[cards.length - 1]
  const peeking = cards.slice(Math.max(0, cards.length - 1 - MAX_PEEK), cards.length - 1)

  const isTargetable = !!validTargets && cards.some((c) => validTargets.includes(c.id))
  const isTargetedOnStack = stackCards.some((s) =>
    s.targets?.some((t) => t.type === 'Card' && cards.some((c) => c.id === t.cardId)),
  )
  const glow = isTargetable
    ? '0 0 0 2px #fbbf24, 0 0 14px rgba(251, 191, 36, 0.75)'
    : isTargetedOnStack
      ? '0 0 12px 4px rgba(255, 136, 0, 0.8)'
      : `0 2px 10px ${ANTE_HUE}33`

  const verticalOffset = isOpponent
    ? { alignSelf: 'flex-start' as const }
    : { alignSelf: 'flex-end' as const, marginBottom: responsive.sectionGap * 2 }

  const ownerName = isOpponent ? `${player.name}'s` : 'Your'
  const title = cards.length === 0
    ? `${ownerName} ante is empty`
    : `${ownerName} ante: ${cards.map((c) => c.name).join(', ')} — the winner takes it`

  return (
    <div
      data-zone={isOpponent ? 'opponent-ante' : 'player-ante'}
      data-zone-owner={player.playerId}
      style={{
        ...styles.zoneStack,
        gap: compact ? 2 : 3,
        padding: compact ? '0 4px' : '0 6px',
        minWidth: width + (compact ? 8 : 12),
        flexShrink: 0,
        ...verticalOffset,
      }}
    >
      <div
        title={title}
        style={{
          position: 'relative',
          width,
          height,
          cursor: cards.length > 0 ? 'pointer' : 'default',
        }}
        onClick={() => { if (cards.length > 0) setBrowsing(true) }}
        onMouseEnter={(e) => { if (topCard) hoverCard(topCard.id, { x: e.clientX, y: e.clientY }) }}
        onMouseLeave={() => hoverCard(null)}
      >
        {topCard ? (
          <>
            {peeking.map((card, i) => {
              const depth = peeking.length - i
              return (
                <img
                  key={card.id}
                  src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                  alt=""
                  aria-hidden
                  style={{
                    position: 'absolute',
                    inset: 0,
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                    borderRadius: radius,
                    transform: `translate(${-3 * depth}px, ${-3 * depth}px)`,
                    filter: 'brightness(0.55)',
                    border: `1px solid ${ANTE_DEEP}`,
                    boxSizing: 'border-box',
                  }}
                  onError={(e) => handleImageError(e, card.name, 'normal')}
                />
              )
            })}
            <div
              style={{
                position: 'absolute',
                inset: 0,
                borderRadius: radius,
                overflow: 'hidden',
                border: `1px solid ${ANTE_HUE}`,
                boxShadow: glow,
                transition: 'box-shadow 160ms ease',
              }}
            >
              <img
                src={getCardImageUrl(topCard.name, topCard.imageUri, 'normal')}
                alt={topCard.name}
                style={styles.pileImage}
                onError={(e) => handleImageError(e, topCard.name, 'normal')}
              />
            </div>
            {cards.length > 1 && (
              <div
                style={{
                  ...styles.pileCount,
                  fontSize: responsive.fontSize.small,
                  background: `linear-gradient(135deg, ${ANTE_DEEP}, ${ANTE_HUE})`,
                  color: '#1f1205',
                  border: 'none',
                }}
              >
                {cards.length}
              </div>
            )}
          </>
        ) : (
          <div
            style={{
              ...styles.emptyPile,
              borderRadius: radius,
              border: `1px dashed ${ANTE_HUE}55`,
              backgroundColor: `${ANTE_HUE}0a`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              opacity: 0.8,
            }}
          >
            <AnteCoin size={narrow ? 10 : 14} />
          </div>
        )}
      </div>
      <span
        style={{
          ...styles.zoneLabel,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 3,
          color: ANTE_HUE,
          fontWeight: 700,
          fontSize: narrow ? 8 : compact ? 9 : 10,
          letterSpacing: narrow ? 0.4 : 1,
          whiteSpace: 'nowrap',
        }}
      >
        <AnteCoin size={narrow ? 7 : 8} />
        Ante
      </span>
      {browsing && createPortal(
        <AnteBrowser
          cards={cards}
          ownerLabel={`${ownerName} Ante`}
          onClose={() => setBrowsing(false)}
        />,
        document.body,
      )}
    </div>
  )
}

/** Full-screen browser over one player's ante — hover any card for the usual preview. */
function AnteBrowser({ cards, ownerLabel, onClose }: { cards: readonly ClientCard[]; ownerLabel: string; onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * CARD_RATIO)

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div style={{ ...styles.exileOverlay, backgroundColor: 'rgba(28, 16, 4, 0.9)' }} onClick={onClose}>
      <div style={styles.exileBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <h2 style={{ ...styles.exileBrowserTitle, color: ANTE_HUE, display: 'flex', alignItems: 'center', gap: 8 }}>
            <AnteCoin size={16} />
            {ownerLabel} ({cards.length})
          </h2>
          <button
            style={{ ...styles.exileCloseButton, borderColor: ANTE_DEEP, color: ANTE_HUE }}
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>
        <p style={{ margin: 0, color: '#b9a58d', fontSize: responsive.fontSize.small }}>
          Wagered for this game — the winner takes the ante.
        </p>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                boxShadow: `0 0 0 1px ${ANTE_HUE}88, 0 4px 14px rgba(0, 0, 0, 0.5)`,
              }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

/**
 * The game-level "Playing for ante" chip for the centre HUD — one per table, beside the
 * day/night badge. Renders nothing in a game that isn't for ante.
 */
export function AnteIndicator() {
  const playingForAnte = useGameStore((state) => selectGameState(state)?.playingForAnte ?? false)
  if (!playingForAnte) return null
  return (
    <div
      role="note"
      title="This game is played for ante — the winner takes every card in the ante."
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        padding: '2px 9px',
        borderRadius: 999,
        cursor: 'help',
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: '0.06em',
        textTransform: 'uppercase',
        color: ANTE_HUE,
        whiteSpace: 'nowrap',
        userSelect: 'none',
        background: 'linear-gradient(150deg, rgba(56, 34, 12, 0.92) 0%, rgba(26, 15, 5, 0.95) 100%)',
        border: `1px solid ${ANTE_HUE}66`,
        boxShadow: `0 0 9px ${ANTE_HUE}33, inset 0 0 8px ${ANTE_HUE}1c`,
      }}
    >
      <AnteCoin size={10} />
      <span>For ante</span>
    </div>
  )
}
