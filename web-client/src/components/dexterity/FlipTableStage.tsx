import { useLayoutEffect, useRef, useState, type ReactNode, type PointerEvent as ReactPointerEvent } from 'react'
import type { EntityId, FlipTable, FlipTableCard } from '@/types'
import { CARD_BACK_IMAGE_URL, getCardImageUrl } from '@/utils/cardImages.ts'
import { CARD_H, CARD_W, type FlipView, type Point } from './flipGeometry'
import styles from './Flip.module.css'

/** How a card on the table is marked right now. */
export type CardMark = 'target' | 'own' | 'immune' | 'hit' | 'destroyed' | 'spared'

const MARK_CLASS: Record<CardMark, string | undefined> = {
  target: styles.cardTarget,
  own: styles.cardOwn,
  immune: styles.cardImmune,
  hit: styles.cardHit,
  destroyed: styles.cardDestroyed,
  spared: styles.cardSpared,
}

export type StagePointerKind = 'move' | 'down' | 'up' | 'cancel' | 'leave'

export function FlipTableStage({
  table,
  view,
  viewerId,
  playerName,
  marks,
  chips,
  className,
  onPointer,
  children,
}: {
  table: FlipTable
  view: FlipView
  viewerId: EntityId | null
  playerName: (playerId: EntityId) => string
  marks: ReadonlyMap<EntityId, CardMark>
  /** A small label under a card ("Destroyed", "Token"). */
  chips?: ReadonlyMap<EntityId, { text: string; tone: 'danger' | 'warn' | 'calm' }>
  className?: string | undefined
  /** Pointer activity, already converted to table millimetres. */
  onPointer?: (kind: StagePointerKind, p: Point, e: ReactPointerEvent<HTMLDivElement>) => void
  /** Overlay elements, positioned in stage coordinates. */
  children?: ReactNode
}) {
  const frameRef = useRef<HTMLDivElement>(null)
  const stageRef = useRef<HTMLDivElement>(null)
  const [fit, setFit] = useState({ k: 1, left: 0, top: 0 })

  useLayoutEffect(() => {
    const frame = frameRef.current
    if (!frame) return
    const measure = () => {
      const { width, height } = frame.getBoundingClientRect()
      const k = Math.max(0.2, Math.min(width / view.stageWidth, height / view.stageHeight))
      setFit({ k, left: (width - view.stageWidth * k) / 2, top: (height - view.stageHeight * k) / 2 })
    }
    measure()
    const observer = new ResizeObserver(measure)
    observer.observe(frame)
    return () => observer.disconnect()
  }, [view.stageWidth, view.stageHeight])

  const toTable = (e: ReactPointerEvent<HTMLDivElement>): Point => {
    const rect = stageRef.current!.getBoundingClientRect()
    return view.fromStage({ x: (e.clientX - rect.left) / fit.k, y: (e.clientY - rect.top) / fit.k })
  }
  const handler = (kind: StagePointerKind) =>
    onPointer ? (e: ReactPointerEvent<HTMLDivElement>) => onPointer(kind, toTable(e), e) : undefined

  const tableTopLeft = view.toStage({ x: view.fromFarSide ? table.width : 0, y: view.fromFarSide ? table.height : 0 })
  const hand = view.toStage({ x: table.handX, y: table.handY })

  return (
    <div ref={frameRef} className={styles.stageFrame}>
      <div
        ref={stageRef}
        className={`${styles.stage} ${className ?? ''}`}
        style={{
          width: view.stageWidth,
          height: view.stageHeight,
          transform: `translate(${fit.left}px, ${fit.top}px) scale(${fit.k})`,
        }}
        onPointerMove={handler('move')}
        onPointerDown={handler('down')}
        onPointerUp={handler('up')}
        onPointerCancel={handler('cancel')}
        onPointerLeave={handler('leave')}
        onContextMenu={(e) => e.preventDefault()}
      >
        <div
          className={styles.felt}
          style={{ left: tableTopLeft.x, top: tableTopLeft.y, width: table.width, height: table.height }}
        />
        <div
          className={styles.handLip}
          style={{ left: hand.x - 120, width: 240, top: view.fromFarSide ? tableTopLeft.y - 36 : tableTopLeft.y + table.height }}
        />

        {table.bands.map((band, index) => {
          const a = view.toStage({ x: 0, y: band.top })
          const b = view.toStage({ x: table.width, y: band.top + band.height })
          const yours = band.playerId === viewerId
          const top = Math.min(a.y, b.y)
          const height = Math.abs(b.y - a.y)
          // Labels sit at each band's outer edge: the edge further from the table's middle.
          const outerTop = top + height / 2 < view.stageHeight / 2
          return (
            <div key={band.playerId}>
              <div
                className={`${styles.band} ${yours ? styles.bandYours : ''}`}
                style={{ left: Math.min(a.x, b.x) + 10, top: top + 10, width: table.width - 20, height: height - 20 }}
              />
              <div
                className={styles.bandLabel}
                style={{ left: Math.min(a.x, b.x) + 22, top: outerTop ? top + 16 : top + height - 30 }}
              >
                <span className={styles.bandDot} style={{ background: yours ? '#7fd6a6' : '#f08a7a' }} />
                {yours ? 'You' : playerName(band.playerId)}
              </div>
              {index > 0 && (
                <div
                  className={styles.centreLine}
                  // Midway across the gap to the band before this one.
                  style={{ left: Math.min(a.x, b.x) + 30, width: table.width - 60, top: view.toStage({ x: 0, y: band.top - 18 }).y }}
                />
              )}
            </div>
          )
        })}

        {table.cards.map((card) => (
          <TableCard
            key={card.entityId}
            card={card}
            view={view}
            mark={marks.get(card.entityId)}
            chip={chips?.get(card.entityId)}
          />
        ))}

        {children}
      </div>
    </div>
  )
}

function TableCard({
  card,
  view,
  mark,
  chip,
}: {
  card: FlipTableCard
  view: FlipView
  mark: CardMark | undefined
  chip: { text: string; tone: 'danger' | 'warn' | 'calm' } | undefined
}) {
  const centre = view.toStage({ x: card.x, y: card.y })
  const image = card.faceDown || !card.name ? CARD_BACK_IMAGE_URL : getCardImageUrl(card.name, card.imageUri)
  const chipClass = chip?.tone === 'danger' ? styles.chipDanger : chip?.tone === 'warn' ? styles.chipWarn : styles.chipCalm
  return (
    <div
      className={`${styles.card} ${mark ? MARK_CLASS[mark] ?? '' : ''}`}
      style={{ left: centre.x - card.width / 2, top: centre.y - card.height / 2, width: card.width, height: card.height }}
      title={card.name ?? 'Face-down permanent'}
    >
      <div
        className={styles.cardFace}
        style={{
          backgroundImage: `url("${image}")`,
          transform: `translate(-50%, -50%) rotate(${card.tapped ? 90 : 0}deg)`,
          width: CARD_W,
          height: CARD_H,
        }}
      />
      {chip && (
        <span className={`${styles.chip} ${chipClass}`} style={{ top: card.height - 7 }}>
          {chip.text}
        </span>
      )}
    </div>
  )
}
