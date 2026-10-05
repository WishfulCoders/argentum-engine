import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState } from '@/store/selectors.ts'
import type { EntityId } from '@/types'
import { faceDownImageUrl, getCardImageUrl } from '@/utils/cardImages.ts'

/**
 * Small art thumbnail of the card behind another player's pending decision, shown in the
 * "X is making a choice" banner. Hovering it opens the regular card preview.
 *
 * The card is resolved from the viewer's own game state, which the server has already masked:
 * a card the viewer can't see (in a hidden zone) has no entry and renders nothing, and a
 * face-down permanent shows its face-down helper art rather than its real face.
 */
export function DecisionSourceThumbnail({ sourceId }: { sourceId: EntityId | null | undefined }) {
  const card = useGameStore((s) => (sourceId ? selectGameState(s)?.cards[sourceId] ?? null : null))
  if (!card) return null

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
  },
}
