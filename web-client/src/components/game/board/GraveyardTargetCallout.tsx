import { useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { handleImageError, useResponsiveContext } from './shared'
import type { GraveyardTarget } from './graveyardTargets'

const TARGET_ORANGE = '#ff8800'
const GAP = 14
const EDGE = 8
const SHORT_VIEWPORT = 560

interface Placement {
  readonly top: number
  readonly left: number
  /** Which side of the pile the callout sits on — the notch points back at the pile. */
  readonly side: 'left' | 'right'
  readonly notchTop: number
}

/**
 * Pulls the graveyard cards a stack object targets out of their pile and shows them at a
 * readable size beside it, with the card's name and what targets it. A pile renders only its
 * top card at a few dozen pixels, so without this the targeting arrow ends on an unreadable
 * thumbnail — usually of a *different* card than the one targeted.
 *
 * Portalled to the body with fixed positioning so the pile column's overflow clip can't cut it
 * off; it tracks the pile while mounted (it's only mounted while something is targeted). The
 * cards carry `data-card-id`, so `TargetingArrows` ends its arrows here.
 */
export function GraveyardTargetCallout({
  targets,
  pileRef,
  onOpen,
}: {
  targets: readonly GraveyardTarget[]
  pileRef: React.RefObject<HTMLElement | null>
  onOpen: () => void
}) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const panelRef = useRef<HTMLDivElement>(null)
  const [placement, setPlacement] = useState<Placement | null>(null)

  // Short landscape screens (phones on their side) aren't `isMobile` but can't fit the full-size
  // callout beside a pile without it covering the action buttons.
  const [shortViewport, setShortViewport] = useState(() => window.innerHeight < SHORT_VIEWPORT)
  const compact = responsive.isMobile || shortViewport
  const cardWidth = compact ? 64 : 96
  const cardHeight = Math.round(cardWidth * 1.4)

  useLayoutEffect(() => {
    const place = () => {
      setShortViewport(window.innerHeight < SHORT_VIEWPORT)
      const pile = pileRef.current
      const panel = panelRef.current
      if (!pile || !panel) return
      const p = pile.getBoundingClientRect()
      // A pile in a collapsed / display:none board measures 0×0 at the origin — don't float there.
      if (p.width === 0 && p.height === 0) {
        setPlacement(null)
        return
      }
      const w = panel.offsetWidth
      const h = panel.offsetHeight
      // Open toward the table: piles live at the screen edge, so go whichever way has room.
      // Clear the whole pile column, not just the pile — its "Graveyard" label is wider.
      const column = pile.parentElement?.getBoundingClientRect() ?? p
      const side: Placement['side'] = p.left + p.width / 2 > window.innerWidth / 2 ? 'left' : 'right'
      const left = side === 'left' ? Math.min(p.left, column.left) - GAP - w : Math.max(p.right, column.right) + GAP
      const pileMid = p.top + p.height / 2
      const top = Math.max(EDGE, Math.min(pileMid - h / 2, window.innerHeight - h - EDGE))
      const notchTop = Math.max(12, Math.min(pileMid - top, h - 12))
      setPlacement((prev) =>
        prev && prev.top === top && prev.left === left && prev.side === side && prev.notchTop === notchTop
          ? prev
          : { top, left, side, notchTop },
      )
    }
    place()
    // The pile moves with board layout (resizes, piles appearing above it); a cheap poll keeps
    // the callout pinned without wiring observers into every ancestor. Mounted only while targeted.
    const interval = window.setInterval(place, 150)
    window.addEventListener('resize', place)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('resize', place)
    }
  }, [pileRef, targets.length, cardWidth])

  const notch = placement && (
    <div
      aria-hidden
      style={{
        position: 'absolute',
        top: placement.notchTop - 7,
        [placement.side === 'left' ? 'right' : 'left']: -7,
        width: 12,
        height: 12,
        background: 'rgba(28, 16, 6, 0.94)',
        borderTop: `1.5px solid ${TARGET_ORANGE}`,
        borderRight: `1.5px solid ${TARGET_ORANGE}`,
        transform: placement.side === 'left' ? 'rotate(45deg)' : 'rotate(-135deg)',
      }}
    />
  )

  // One source for every card (the usual case — a single spell or ability) is said once, under the
  // row; mixed sources are labelled per card.
  const sharedSources = targets.every((t) => sameNames(t.sourceNames, targets[0]?.sourceNames ?? []))
    ? (targets[0]?.sourceNames ?? [])
    : null
  const sourceLine = (names: readonly string[], maxWidth: number) => (
    <div
      style={{
        maxWidth,
        color: '#ffb366',
        fontSize: 9,
        lineHeight: 1.25,
        textAlign: 'center',
        overflow: 'hidden',
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical',
      }}
    >
      by {names.join(', ')}
    </div>
  )

  return createPortal(
    <div
      ref={panelRef}
      data-testid="graveyard-target-callout"
      style={{
        position: 'fixed',
        top: placement?.top ?? -9999,
        left: placement?.left ?? -9999,
        // Under the targeting arrows (999) so the arrowhead lands on the card, over the board.
        zIndex: 900,
        visibility: placement ? 'visible' : 'hidden',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: compact ? '6px 6px 5px' : '8px 10px 7px',
        background: 'rgba(28, 16, 6, 0.94)',
        border: `1.5px solid ${TARGET_ORANGE}`,
        borderRadius: 10,
        boxShadow: '0 6px 20px rgba(0, 0, 0, 0.55), 0 0 18px rgba(255, 136, 0, 0.25)',
        animation: 'graveyardTargetIn 180ms ease-out',
        // On cramped screens the panel can graze the action buttons; only the card takes clicks.
        pointerEvents: 'none',
      }}
    >
      {notch}
      <div
        style={{
          color: TARGET_ORANGE,
          fontSize: compact ? 8 : 9,
          fontWeight: 800,
          letterSpacing: 1.2,
          textTransform: 'uppercase',
        }}
      >
        {targets.length > 1 ? `${targets.length} targeted` : 'Targeted'}
      </div>
      <div style={{ display: 'flex', gap: compact ? 6 : 10, alignItems: 'flex-start' }}>
        {targets.map(({ card, sourceNames }) => (
          <div
            key={card.id}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, width: cardWidth }}
          >
            <div
              data-card-id={card.id}
              role="button"
              title={`${card.name} — targeted by ${sourceNames.join(', ')}. Click to view the graveyard.`}
              onClick={onOpen}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: compact ? 4 : 6,
                overflow: 'hidden',
                cursor: 'pointer',
                pointerEvents: 'auto',
                animation: 'graveyardTargetPulse 1.4s ease-in-out infinite alternate',
              }}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
            <div
              style={{
                maxWidth: cardWidth + 8,
                color: '#fff',
                fontSize: compact ? 9 : 11,
                fontWeight: 700,
                lineHeight: 1.2,
                textAlign: 'center',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {card.name}
            </div>
            {!compact && !sharedSources && sourceNames.length > 0 && sourceLine(sourceNames, cardWidth + 8)}
          </div>
        ))}
      </div>
      {!compact && sharedSources && sharedSources.length > 0 && sourceLine(sharedSources, Math.max(cardWidth + 8, 180))}
    </div>,
    document.body,
  )
}

function sameNames(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((n, i) => n === b[i])
}
