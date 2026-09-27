import { useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, SelectCardsDecision } from '@/types'
import { calculateFittingCardWidth, decisionCardMaxWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { useDecisionHotkeys } from '@/hooks/useDecisionHotkeys.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DecisionCardPreview } from './DecisionComponents'
import {
  clearPlannedOrder,
  initialLaneState,
  lanesFor,
  moveCard,
  planLibraryOrder,
  type Lane,
  type LaneKey,
  type LaneState,
} from './libraryLanes'
import styles from './DecisionUI.module.css'

const GENERIC_PROMPT = /^(choose|select) (up to )?\d+ cards?\.?$/i

/** Where a dragged card would land: before `index` in `lane`. */
interface DropSpot {
  lane: LaneKey
  index: number
}

/**
 * A two-way card split drawn as its two destinations — scry's "top of library" and "bottom",
 * surveil's "top" and "graveyard", "hand" and "bottom" — with the cards dragged into the one they
 * go to. The library-top lane is ordered left to right, top card first, and that order also
 * answers the reorder prompt the engine raises next (see `libraryLanes.ts`).
 *
 * Click a card to send it to the other lane. With a card under the cursor, ↑/↓ moves it between
 * lanes and ←/→ along its lane; Enter confirms.
 */
export function LibraryLanesUI({
  decision,
  responsive,
}: {
  decision: SelectCardsDecision
  responsive: ResponsiveSizes
}) {
  const submitDecision = useGameStore((s) => s.submitDecision)
  const gameState = useGameStore((s) => s.gameState)
  const [state, setState] = useState<LaneState>(() => initialLaneState(decision))
  const [dragId, setDragId] = useState<EntityId | null>(null)
  const [dropSpot, setDropSpot] = useState<DropSpot | null>(null)
  const [hoveredId, setHoveredId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)

  const lanes = useMemo(() => lanesFor(decision), [decision])
  const selectable = useMemo(() => new Set(decision.options), [decision.options])
  const total = state.selected.length + state.remainder.length

  const infoFor = (cardId: EntityId) => {
    const fromDecision = decision.cardInfo?.[cardId]
    const fromState = gameState?.cards[cardId]
    return {
      name: fromDecision?.name || fromState?.name || 'Unknown Card',
      imageUri: fromDecision?.imageUri || fromState?.imageUri,
    }
  }

  const move = (cardId: EntityId, toLane: LaneKey, index?: number) =>
    setState((prev) => moveCard(prev, cardId, toLane, index, selectable, decision.maxSelections))

  const laneOf = (cardId: EntityId): LaneKey => (state.selected.includes(cardId) ? 'selected' : 'remainder')
  const otherLane = (key: LaneKey): LaneKey => (key === 'selected' ? 'remainder' : 'selected')

  const count = state.selected.length
  const canConfirm = count >= decision.minSelections && count <= decision.maxSelections

  const handleConfirm = () => {
    if (!canConfirm) return
    const topLane = lanes.find((l) => l.isLibraryTop)
    if (topLane) planLibraryOrder(decision.id, state[topLane.key])
    else clearPlannedOrder()
    submitDecision(decision.id, state.selected)
  }

  useDecisionHotkeys(
    {
      Enter: () => {
        if (!canConfirm) return false
        handleConfirm()
      },
      ArrowUp: () => {
        if (!hoveredId) return false
        const at = lanes.findIndex((l) => l.key === laneOf(hoveredId))
        if (at <= 0) return false
        move(hoveredId, lanes[at - 1]!.key)
      },
      ArrowDown: () => {
        if (!hoveredId) return false
        const at = lanes.findIndex((l) => l.key === laneOf(hoveredId))
        if (at < 0 || at >= lanes.length - 1) return false
        move(hoveredId, lanes[at + 1]!.key)
      },
      ArrowLeft: () => {
        if (!hoveredId) return false
        const lane = laneOf(hoveredId)
        const at = state[lane].indexOf(hoveredId)
        if (at <= 0) return false
        move(hoveredId, lane, at - 1)
      },
      ArrowRight: () => {
        if (!hoveredId) return false
        const lane = laneOf(hoveredId)
        const at = state[lane].indexOf(hoveredId)
        if (at < 0 || at >= state[lane].length - 1) return false
        // moveCard counts the gap the card leaves, so "after the next card" is index + 2.
        move(hoveredId, lane, at + 2)
      },
    },
    !minimized,
  )

  if (minimized) {
    return (
      <button className={styles.floatingReturnButton} onClick={() => setMinimized(false)}>
        Return to {decision.context.sourceName ?? 'decision'}
      </button>
    )
  }

  // Size: every card in one lane must fit the width, and both lanes the height (title, lane
  // headers and the button row take roughly 300 px).
  const gap = responsive.isMobile ? 6 : 12
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const heightCap = Math.floor((responsive.viewportHeight - 300) / lanes.length / 1.4)
  const cardWidth = calculateFittingCardWidth(
    total,
    availableWidth,
    gap,
    Math.max(70, Math.min(decisionCardMaxWidth(responsive), heightCap)),
    60,
  )
  const cardHeight = Math.round(cardWidth * 1.4)

  /**
   * The card a drop carries, read from the drag's own data rather than `dragId`: a quick flick can
   * drop before the render that sets `dragId` has happened.
   */
  const draggedFrom = (e: React.DragEvent): EntityId | null => {
    const raw = e.dataTransfer.getData('text/plain')
    return [...state.selected, ...state.remainder].find((id) => String(id) === raw) ?? dragId
  }

  /** The selected lane turns away a card that can't be chosen, and a new card once it is full. */
  const refusesCard = (lane: LaneKey, cardId: EntityId) =>
    lane === 'selected' &&
    (!selectable.has(cardId) || (count >= decision.maxSelections && laneOf(cardId) !== 'selected'))

  /** Insertion index for a drag over the card at [index]: before it on its left half, after on its right. */
  const indexAt = (e: React.DragEvent, index: number) => {
    const rect = e.currentTarget.getBoundingClientRect()
    return index + (e.clientX > rect.left + rect.width / 2 ? 1 : 0)
  }

  const endDrag = () => {
    setDragId(null)
    setDropSpot(null)
  }

  const renderLane = (lane: Lane) => {
    const cards = state[lane.key]
    const marker = dragId && dropSpot?.lane === lane.key ? dropSpot.index : null
    const refuses = dragId !== null && refusesCard(lane.key, dragId)

    const items: React.ReactNode[] = []
    cards.forEach((cardId, index) => {
      if (marker === index) items.push(<div key="marker" className={styles.laneDropMarker} style={{ height: cardHeight }} />)
      const { name, imageUri } = infoFor(cardId)
      const locked = !selectable.has(cardId)
      items.push(
        <div
          key={cardId}
          className={styles.laneCardSlot}
          style={{ width: cardWidth }}
        >
          <div
            draggable
            onDragStart={(e) => {
              e.dataTransfer.effectAllowed = 'move'
              e.dataTransfer.setData('text/plain', String(cardId))
              setDragId(cardId)
            }}
            onDragEnd={endDrag}
            onDragOver={(e) => {
              if (refuses) return
              e.preventDefault()
              e.stopPropagation()
              const spot = { lane: lane.key, index: indexAt(e, index) }
              if (spot.index !== dropSpot?.index || dropSpot.lane !== lane.key) setDropSpot(spot)
            }}
            onDrop={(e) => {
              e.preventDefault()
              e.stopPropagation()
              const dropped = draggedFrom(e)
              if (dropped && !refusesCard(lane.key, dropped)) move(dropped, lane.key, indexAt(e, index))
              endDrag()
            }}
            onClick={() => move(cardId, otherLane(lane.key))}
            onMouseEnter={() => setHoveredId(cardId)}
            onMouseLeave={() => setHoveredId((h) => (h === cardId ? null : h))}
            className={[
              styles.decisionCard,
              styles.laneCard,
              locked ? styles.laneCardLocked : styles.decisionCardDefault,
              dragId === cardId ? styles.laneCardDragging : '',
            ].join(' ')}
            style={{ width: cardWidth, height: cardHeight }}
            title={locked ? `${name} — can't be chosen for "${decision.selectedLabel}"` : name}
            data-testid={`lane-card-${lane.key}`}
          >
            <img src={getCardImageUrl(name, imageUri)} alt={name} className={styles.cardImage} draggable={false} />
          </div>
          {lane.isLibraryTop && (
            <div className={index === 0 ? styles.laneOrdinalTop : styles.laneOrdinal}>
              {index === 0 ? '1 · top' : index + 1}
            </div>
          )}
        </div>,
      )
    })
    if (marker !== null && marker >= cards.length) {
      items.push(<div key="marker" className={styles.laneDropMarker} style={{ height: cardHeight }} />)
    }

    return (
      <div
        key={lane.key}
        className={[styles.lane, lane.isLibraryTop ? styles.laneLibraryTop : '', refuses ? styles.laneRefuses : ''].join(' ')}
        onDragOver={(e) => {
          if (refuses) return
          e.preventDefault()
          // Over the lane but not a card: the end of the lane.
          if (dropSpot?.lane !== lane.key) setDropSpot({ lane: lane.key, index: cards.length })
        }}
        onDragLeave={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
            setDropSpot((s) => (s?.lane === lane.key ? null : s))
          }
        }}
        onDrop={(e) => {
          e.preventDefault()
          // Dropped on the lane itself, not on a card: the end of the lane. (Read from the event,
          // not from dropSpot — a quick drop can land before the dragover's state renders.)
          const dropped = draggedFrom(e)
          if (dropped && !refusesCard(lane.key, dropped)) move(dropped, lane.key)
          endDrag()
        }}
        data-testid={`lane-${lane.key}`}
        data-lane-label={lane.label}
      >
        <div className={styles.laneHeader}>
          <span className={styles.laneLabel}>{lane.label}</span>
          {lane.isLibraryTop && cards.length > 1 && <span className={styles.laneHint}>left card is drawn first</span>}
          {lane.key === 'selected' && decision.maxSelections < total && (
            <span className={styles.laneHint}>
              {count} / {decision.maxSelections}
              {decision.minSelections > 0 && count < decision.minSelections ? ` (at least ${decision.minSelections})` : ''}
            </span>
          )}
        </div>
        <div className={styles.laneCards} style={{ gap, minHeight: cardHeight + 24 }}>
          {items.length > 0 ? items : <div className={styles.laneEmpty} style={{ height: cardHeight }}>Drop cards here</div>}
        </div>
      </div>
    )
  }

  const hovered = hoveredId && !dragId ? infoFor(hoveredId) : null

  return (
    <div className={styles.overlay} style={{ gap: 'var(--space-3)' }}>
      <h2 className={styles.title}>{decision.context.sourceName ?? decision.prompt}</h2>
      {/* The engine's generic "Choose up to N cards" is the grid picker's wording; the lanes say it. */}
      {decision.context.sourceName && !GENERIC_PROMPT.test(decision.prompt) && (
        <p className={styles.hint}>{decision.prompt}</p>
      )}

      <div className={styles.lanesBoard}>{lanes.map(renderLane)}</div>

      <p className={styles.hintSmall}>
        Drag cards between rows, or click one to send it to the other row. Hover a card: ↑ ↓ changes row,
        ← → reorders. Enter confirms · hold Tab to see the board.
      </p>

      <div className={styles.optionButtonRow}>
        <button onClick={() => setMinimized(true)} className={styles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button onClick={handleConfirm} disabled={!canConfirm} className={styles.confirmButton}>
          Confirm
        </button>
      </div>

      {hovered && !responsive.isMobile && <DecisionCardPreview cardName={hovered.name} imageUri={hovered.imageUri} />}
    </div>
  )
}
