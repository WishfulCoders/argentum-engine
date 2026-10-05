import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, SplitPilesDecision } from '@/types'
import { calculateFittingCardWidth, decisionCardMaxWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'
import { useDecisionHotkeys } from '@/hooks/useDecisionHotkeys.ts'

/**
 * Split piles decision UI - assign cards to labeled piles (e.g., Surveil).
 * For single-card decisions: shows the card with pile buttons for quick assignment.
 * For multi-card decisions: shows cards with pile assignment toggles.
 */
function CardPileSplitUI({
  decision,
  responsive,
}: {
  decision: SplitPilesDecision
  responsive: ResponsiveSizes
}) {
  const submitSplitPilesDecision = useGameStore((s) => s.submitSplitPilesDecision)
  const gameState = useGameStore((s) => s.gameState)

  // Track which pile each card is assigned to (default: pile 0 = first pile)
  const [assignments, setAssignments] = useState<Record<EntityId, number>>(() => {
    const initial: Record<EntityId, number> = {}
    for (const cardId of decision.cards) {
      initial[cardId] = 0
    }
    return initial
  })

  const labels = decision.pileLabels.length > 0
    ? decision.pileLabels
    : Array.from({ length: decision.numberOfPiles }, (_, i) => `Pile ${i + 1}`)

  const handleAssignToPile = (cardId: EntityId, pileIndex: number) => {
    setAssignments((prev) => ({ ...prev, [cardId]: pileIndex }))
  }

  const handleSubmit = (overrideAssignments?: Record<EntityId, number>) => {
    const finalAssignments = overrideAssignments ?? assignments
    // Build piles array from assignments
    const piles: EntityId[][] = Array.from({ length: decision.numberOfPiles }, () => [])
    for (const cardId of decision.cards) {
      const pileIndex = finalAssignments[cardId] ?? 0
      piles[pileIndex]?.push(cardId)
    }
    submitSplitPilesDecision(decision.id, piles)
  }

  // One card: 1..n send it to that pile. Several: Enter confirms the assignment.
  const pileKeys: Record<string, () => void> = {}
  if (decision.cards.length === 1) {
    labels.slice(0, 9).forEach((_, index) => {
      pileKeys[String(index + 1)] = () => handleSubmit({ [decision.cards[0]!]: index })
    })
  } else {
    pileKeys.Enter = () => handleSubmit()
  }
  useDecisionHotkeys(pileKeys)

  // For single-card surveil: clicking a pile button directly assigns and submits
  if (decision.cards.length === 1) {
    const cardId = decision.cards[0]!
    const cardInfoFromDecision = decision.cardInfo?.[cardId]
    const cardFromState = gameState?.cards[cardId]
    const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
    const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri

    const cardWidth = responsive.isMobile ? 120 : Math.max(180, decisionCardMaxWidth(responsive))
    const cardHeight = Math.round(cardWidth * 1.4)
    const cardImageUrl = getCardImageUrl(cardName, imageUri)

    return (
      <>
        <h2 className={styles.title}>
          {decision.context.sourceName ?? 'Surveil'}
        </h2>
        <p className={styles.hint}>
          {decision.prompt}
        </p>

        {/* Single card display */}
        <div
          style={{
            width: cardWidth,
            height: cardHeight,
            borderRadius: 'var(--radius-lg)',
            overflow: 'hidden',
            border: '2px solid var(--border-card)',
          }}
        >
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
          <div className={styles.cardFallback}>
            <span className={styles.cardFallbackName}>{cardName}</span>
          </div>
        </div>

        {/* Pile buttons */}
        <div className={styles.buttonContainer}>
          {labels.map((label, index) => (
            <button
              key={index}
              onClick={() => {
                const singleAssignment: Record<EntityId, number> = { [cardId]: index }
                handleSubmit(singleAssignment)
              }}
              className={index === 0 ? styles.yesButton : styles.noButton}
              data-testid={`pile-button-${index}`}
              data-pile-label={label}
              title={String(index + 1)}
            >
              {label}
            </button>
          ))}
        </div>
      </>
    )
  }

  // Multi-card: show cards with pile toggle buttons
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = decisionCardMaxWidth(responsive)
  const cardWidth = calculateFittingCardWidth(
    decision.cards.length,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )

  return (
    <>
      <h2 className={styles.title}>
        {decision.context.sourceName ?? 'Split Piles'}
      </h2>
      <p className={styles.hint}>
        {decision.prompt}
      </p>

      {/* Cards with pile assignment */}
      <div className={styles.cardContainer} style={{ gap }}>
        {decision.cards.map((cardId) => {
          const cardInfoFromDecision = decision.cardInfo?.[cardId]
          const cardFromState = gameState?.cards[cardId]
          const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
          const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri
          const currentPile = assignments[cardId] ?? 0
          const cardImageUrl = getCardImageUrl(cardName, imageUri)
          const cardHeight = Math.round(cardWidth * 1.4)

          return (
            <div key={cardId} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
              <div
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  borderRadius: 'var(--radius-md)',
                  overflow: 'hidden',
                  border: '2px solid var(--border-card)',
                  position: 'relative',
                  cursor: 'pointer',
                }}
                onClick={() => {
                  // Cycle to next pile
                  const nextPile = (currentPile + 1) % decision.numberOfPiles
                  handleAssignToPile(cardId, nextPile)
                }}
              >
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
                <div className={styles.cardFallback}>
                  <span className={styles.cardFallbackName}>{cardName}</span>
                </div>
              </div>
              {/* Pile assignment buttons */}
              <div style={{ display: 'flex', gap: 2 }}>
                {labels.map((label, index) => (
                  <button
                    key={index}
                    onClick={() => handleAssignToPile(cardId, index)}
                    data-testid={`pile-button-${index}`}
                    data-pile-label={label}
                    style={{
                      fontSize: 'var(--font-xs)',
                      padding: '2px 6px',
                      borderRadius: 'var(--radius-sm)',
                      border: 'none',
                      cursor: 'pointer',
                      backgroundColor: currentPile === index ? 'var(--accent-gold)' : 'var(--bg-tertiary)',
                      color: currentPile === index ? 'var(--text-dark)' : 'var(--text-secondary)',
                      fontWeight: currentPile === index ? 'var(--font-weight-bold)' : 'var(--font-weight-normal)',
                    }}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )
        })}
      </div>

      {/* Confirm button */}
      <button onClick={() => handleSubmit()} className={styles.confirmButton}>
        Confirm
      </button>
    </>
  )
}

export function SplitPilesUI(props: { decision: SplitPilesDecision; responsive: ResponsiveSizes }) {
  return props.decision.useTargetingUI
    ? <BattlefieldPileSplitUI decision={props.decision} />
    : <CardPileSplitUI {...props} />
}

function BattlefieldPileSplitUI({ decision }: { decision: SplitPilesDecision }) {
  const startSelection = useGameStore((s) => s.startDecisionSelection)
  const cancelSelection = useGameStore((s) => s.cancelDecisionSelection)
  const selection = useGameStore((s) => s.decisionSelectionState)
  const submitPiles = useGameStore((s) => s.submitSplitPilesDecision)
  const gameState = useGameStore((s) => s.gameState)
  const [activePile, setActivePile] = useState(0)
  // A restriction re-ask arrives with the engine's maximal legal blocks; start from those.
  const [piles, setPiles] = useState<EntityId[][]>(() => decision.suggestedPiles?.map((pile) => [...pile])
    ?? Array.from({ length: decision.numberOfPiles }, () => []))

  useEffect(() => {
    const options = (decision.pileOptions?.[activePile] ?? decision.cards).filter((id) => {
      const otherMemberships = piles.filter((_, index) => index !== activePile).filter((pile) => pile.includes(id)).length
      return otherMemberships < (decision.maxPileMemberships?.[id] ?? 1)
    })
    startSelection({ decisionId: decision.id, validOptions: options, selectedOptions: piles[activePile] ?? [],
      minSelections: 0, maxSelections: options.length, prompt: decision.prompt })
    return () => cancelSelection(decision.id)
  }, [decision.id, activePile, piles, startSelection, cancelSelection])

  const currentPiles = () => piles.map((pile, index) => index === activePile
    ? [...(selection?.decisionId === decision.id ? selection.selectedOptions : pile)] : pile)
  const switchPile = (index: number) => {
    setPiles(currentPiles())
    setActivePile(index)
  }
  const submit = () => {
    submitPiles(decision.id, currentPiles())
    cancelSelection(decision.id)
  }
  const displayedPiles = currentPiles()
  return <DraggableBanner className={styles.sideBannerSelection}>
    <div className={styles.bannerTitleSelection}>Blocker piles</div>
    <p className={styles.hint}>{decision.prompt}</p>
    <p className={styles.hint}>Select a pile, then click creatures on the battlefield to add or remove them.</p>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
      {displayedPiles.map((pile, index) => <button key={index} onClick={() => switchPile(index)}
        aria-pressed={activePile === index} data-testid={`blocker-pile-${index}`}
        className={activePile === index ? styles.yesButton : styles.noButton}>
        {decision.pileLabels[index] ?? `Pile ${index + 1}`} ({pile.length})
      </button>)}
    </div>
    <div className={styles.hint}>
      {(displayedPiles[activePile] ?? []).map((id) => gameState?.cards[id]?.name ?? 'Creature').join(', ') || 'Empty pile'}
    </div>
    <button onClick={submit} disabled={decision.requiredAssignments != null && displayedPiles.reduce((sum, pile) => sum + pile.length, 0) !== decision.requiredAssignments} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>{decision.requiredAssignments == null ? 'Assign piles at random' : 'Confirm blocks'}</button>
  </DraggableBanner>
}
