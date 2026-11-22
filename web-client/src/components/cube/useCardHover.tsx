/**
 * The hover card preview every cube view shares with the deckbuilder and the draft overlays: point at
 * a card name to see it, press F to flip a double-faced card. Returns the handler rows call and the
 * preview element to render once, at the end of the view.
 */
import { useCallback, useState } from 'react'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'

export type CardHoverHandler = (card: CardSummary | null, e?: React.MouseEvent) => void

export function useCardHover(): { onHover: CardHoverHandler; preview: React.ReactNode } {
  const [hoveredCard, setHoveredCard] = useState<CardSummary | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const dfc = useDfcHoverFlip(hoveredCard ? { ...hoveredCard, imageUri: hoveredCard.imageUri ?? null } : null)
  const resetDfcFlip = dfc.resetFlip

  const onHover = useCallback<CardHoverHandler>(
    (card, e) => {
      setHoveredCard((prev) => {
        if (prev?.name !== card?.name) resetDfcFlip()
        return card
      })
      setHoverPos(card && e ? { x: e.clientX, y: e.clientY } : null)
    },
    [resetDfcFlip],
  )

  const preview = hoveredCard && hoverPos ? (
    <HoverCardPreview
      name={dfc.displayName ?? hoveredCard.name}
      imageUri={dfc.displayImageUri ?? hoveredCard.imageUri ?? null}
      pos={hoverPos}
      hint={dfc.hint}
      imageRotateDeg={dfc.imageRotateDeg(hoveredCard)}
    />
  ) : null

  return { onHover, preview }
}
