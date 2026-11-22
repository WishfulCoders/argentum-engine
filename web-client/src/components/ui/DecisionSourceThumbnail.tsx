import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState } from '@/store/selectors.ts'
import type { EntityId } from '@/types'
import { faceDownImageUrl, getCardImageUrl } from '@/utils/cardImages.ts'

/**
 * Small art thumbnail of the card behind another player's pending decision, shown in the
 * "X is making a choice" banner. Hovering it opens the regular card preview.
 *
 * The card is resolved from the viewer's own game state, which the server has already masked:
 * a face-down permanent shows its face-down helper art rather than its real face.
 *
 * A spell still being cast from its controller's hand (choosing targets, modes, X) has no entry in
 * the viewer's state yet, though its name is already public. For that case the server sends the
 * source's art alongside the status ([fallbackImageUri]); it is null whenever the viewer may not
 * see the source, so nothing renders then. With no state entry to hand the shared preview, the
 * fallback enlarges in place on hover.
 */
export function DecisionSourceThumbnail({
  sourceId,
  fallbackName,
  fallbackImageUri,
}: {
  sourceId: EntityId | null | undefined
  fallbackName?: string | null | undefined
  fallbackImageUri?: string | null | undefined
}) {
  const card = useGameStore((s) => (sourceId ? selectGameState(s)?.cards[sourceId] ?? null : null))
  const [zoomed, setZoomed] = useState(false)

  if (!card) {
    if (!fallbackImageUri) return null
    return (
      <span
        style={styles.fallbackAnchor}
        onPointerEnter={(e) => {
          if (e.pointerType !== 'touch') setZoomed(true)
        }}
        onPointerLeave={() => setZoomed(false)}
        onClick={() => setZoomed((z) => !z)}
      >
        <img src={fallbackImageUri} alt={fallbackName ?? 'Card'} style={styles.thumbnail} />
        {zoomed && <img src={fallbackImageUri} alt="" aria-hidden style={styles.zoom} />}
      </span>
    )
  }

  const imageUrl = card.isFaceDown
    ? faceDownImageUrl(card.faceDownMode)
    : getCardImageUrl(card.name, card.imageUri, 'small')

  return (
    <img
      src={imageUrl}
      alt={card.isFaceDown ? 'Face-down card' : card.name}
      style={styles.thumbnail}
      onPointerEnter={(e) => {
        if (e.pointerType === 'touch') return
        useGameStore.getState().hoverCard(card.id, { x: e.clientX, y: e.clientY })
      }}
      onPointerMove={(e) => {
        if (e.pointerType === 'touch') return
        useGameStore.getState().updateHoverPosition({ x: e.clientX, y: e.clientY })
      }}
      onPointerLeave={() => useGameStore.getState().hoverCard(null)}
      onClick={() => useGameStore.getState().hoverCard(card.id)}
    />
  )
}

const styles: Record<string, React.CSSProperties> = {
  thumbnail: {
    width: 44,
    aspectRatio: '63 / 88',
    objectFit: 'cover',
    borderRadius: 3,
    boxShadow: '0 2px 6px rgba(0, 0, 0, 0.6)',
    // The banners themselves ignore the pointer so they never block the board; the art opts back in
    // so it can be hovered for the full preview.
    pointerEvents: 'auto',
    cursor: 'zoom-in',
    flexShrink: 0,
    display: 'block',
  },
  fallbackAnchor: {
    position: 'relative',
    flexShrink: 0,
    pointerEvents: 'auto',
  },
  zoom: {
    position: 'absolute',
    top: 'calc(100% + 8px)',
    left: 0,
    width: 'min(250px, 70vw)',
    aspectRatio: '63 / 88',
    objectFit: 'cover',
    borderRadius: 10,
    boxShadow: '0 8px 24px rgba(0, 0, 0, 0.8)',
    pointerEvents: 'none',
  },
}
