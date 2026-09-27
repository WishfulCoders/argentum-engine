import type { EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { previewCardWidth } from '@/hooks/useResponsive.ts'
import styles from './DecisionUI.module.css'

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
export function DecisionCardPreview({ cardName, imageUri }: { cardName: string; imageUri?: string | null | undefined }) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri, 'large')

  const previewWidth = previewCardWidth(window.innerHeight)
  const previewHeight = Math.round(previewWidth * 1.4)

  return (
    <div className={styles.previewContainer}>
      <div
        className={styles.previewCard}
        style={{
          width: previewWidth,
          height: previewHeight,
        }}
      >
        <img
          src={cardImageUrl}
          alt={`${cardName} preview`}
          className={styles.previewImage}
        />
      </div>
    </div>
  )
}

/**
 * Card display for decision UI.
 */
export function DecisionCard({
  cardId: _cardId,
  cardName,
  imageUri,
  isSelected,
  onClick,
  cardWidth = 130,
  onMouseEnter,
  onMouseLeave,
  nonSelectable = false,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  onMouseEnter?: () => void
  onMouseLeave?: () => void
  nonSelectable?: boolean
}) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  const cardClasses = [
    styles.decisionCard,
    nonSelectable
      ? styles.decisionCardNonSelectable
      : isSelected ? styles.decisionCardSelected : styles.decisionCardDefault,
  ].filter(Boolean).join(' ')

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      className={cardClasses}
      style={{
        width: cardWidth,
        height: cardHeight,
      }}
    >
      {/* Card image */}
      <img
        src={cardImageUrl}
        alt={cardName}
        className={styles.cardImage}
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />

      {/* Fallback when image fails */}
      <div className={styles.cardFallback}>
        <span className={styles.cardFallbackName}>
          {cardName}
        </span>
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div className={styles.selectionIndicator}>
          &#10003;
        </div>
      )}
    </div>
  )
}
