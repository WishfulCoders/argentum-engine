import { useState } from 'react'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { handleImageError } from '@/components/game/board/shared'
import s from './Ante.module.css'

/**
 * A small card image with a caption and the shared hover preview — the unit both the game-over
 * ante settlement and the profile's Spoils grid are built from. Cards here are names, not game
 * entities (the game is over), so this drives `HoverCardPreview` directly.
 */
export function AnteCardThumb({
  cardName,
  imageUri,
  caption,
  tone,
}: {
  cardName: string
  imageUri: string | null
  caption: string
  tone: 'won' | 'lost' | 'other'
}) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null)
  return (
    <figure className={s.thumb} data-tone={tone}>
      <img
        className={s.thumbImage}
        src={getCardImageUrl(cardName, imageUri, 'normal')}
        alt={cardName}
        loading="lazy"
        onMouseEnter={(e) => setPos({ x: e.clientX, y: e.clientY })}
        onMouseMove={(e) => setPos({ x: e.clientX, y: e.clientY })}
        onMouseLeave={() => setPos(null)}
        onError={(e) => handleImageError(e, cardName, 'normal')}
      />
      <figcaption className={s.caption} title={caption}>{caption}</figcaption>
      {pos && <HoverCardPreview name={cardName} imageUri={imageUri} pos={pos} />}
    </figure>
  )
}

export function AnteCoinMark() {
  return <span className={s.coin} aria-hidden />
}
