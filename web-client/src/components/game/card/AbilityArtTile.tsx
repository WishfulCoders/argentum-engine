import type React from 'react'
import type { ClientCard } from '@/types/gameState'

/**
 * An ability on the stack with no art to borrow from its source — e.g. the sourceless inherent speed
 * trigger, which the server names "Speed trigger" and sends without an `imageUri`. The usual by-name
 * Scryfall fallback would request a card called "Speed trigger", which doesn't exist, and draw a
 * broken image.
 */
export function isArtlessAbility(card: Pick<ClientCard, 'cardTypes' | 'imageUri'>): boolean {
  return card.cardTypes.includes('Ability') && !card.imageUri
}

/**
 * Stand-in card face for an {@link isArtlessAbility} stack item: a dark frame with the ability's
 * name and type line, sized to whatever slot it fills.
 */
export function AbilityArtTile({
  name,
  typeLine,
  width,
  height,
  fontSize = 11,
  style,
}: {
  name: string
  typeLine: string
  width: number | string
  height: number | string
  fontSize?: number
  style?: React.CSSProperties
}) {
  return (
    <div
      role="img"
      aria-label={name}
      title={name}
      style={{
        width,
        height,
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: fontSize * 0.5,
        padding: fontSize * 0.6,
        borderRadius: 4,
        border: '1px solid rgba(180, 150, 255, 0.45)',
        background: 'radial-gradient(circle at 50% 35%, #3d2f63 0%, #211a36 70%)',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
        color: '#e6defa',
        textAlign: 'center',
        overflow: 'hidden',
        ...style,
      }}
    >
      <span style={{ fontSize: fontSize * 1.15, fontWeight: 700, lineHeight: 1.15, overflowWrap: 'anywhere' }}>
        {name}
      </span>
      <span style={{ fontSize: fontSize * 0.85, color: '#a99bd0', fontStyle: 'italic' }}>{typeLine}</span>
    </div>
  )
}
