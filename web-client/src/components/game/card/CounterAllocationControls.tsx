import { useGameStore } from '@/store/gameStore'
import type { EntityId } from '@/types'

/** Allocate repeated selections on the permanent, using only the server's capacity. */
export function CounterAllocationControls({ cardId, cardName, cardWidth = 160, presentation = 'card' }: {
  cardId: EntityId
  cardName: string
  cardWidth?: number
  presentation?: 'card' | 'inline'
}) {
  const capacity = useGameStore((s) => s.decisionSelectionState?.selectionLimits?.[cardId] ?? 0)
  const allocated = useGameStore((s) => s.decisionSelectionState?.selectedOptions.filter((id) => id === cardId).length ?? 0)
  const totalAtMax = useGameStore((s) => s.decisionSelectionState != null &&
    s.decisionSelectionState.selectedOptions.length >= s.decisionSelectionState.maxSelections)
  if (capacity <= 0) return null
  const inline = presentation === 'inline'
  const compact = !inline && cardWidth < 110
  const buttonSize = compact ? 24 : 28
  const buttonStyle = { width: buttonSize, minWidth: buttonSize, height: buttonSize,
    flexShrink: 0, borderRadius: 4, border: 'none', fontSize: 18 }
  return (
    <div
      data-testid={`counter-allocation-${cardId}${inline ? '-inline' : ''}`}
      style={{ ...(inline ? {} : { position: 'absolute' as const, top: 4, left: '50%',
          transform: 'translateX(-50%)', zIndex: 25 }),
        display: 'flex', flexWrap: compact || inline ? 'wrap' : 'nowrap', justifyContent: 'center',
        gap: 4, alignItems: 'center', width: inline ? '100%' : compact ? Math.max(60, cardWidth) : cardWidth,
        boxSizing: 'border-box', pointerEvents: 'auto',
        background: 'rgba(0, 0, 0, 0.9)', padding: 3, borderRadius: 5 }}
      onClick={(e) => e.stopPropagation()}
    >
      {inline && <span style={{ width: '100%', fontSize: 12, color: '#ddd',
        overflowWrap: 'anywhere', textAlign: 'center' }}>{cardName}</span>}
      {compact && <span style={{ color: '#eab308', width: '100%', textAlign: 'center',
        fontSize: 12, lineHeight: '14px', whiteSpace: 'nowrap' }}>{allocated} / {capacity}</span>}
      <button
        type="button" aria-label={`Remove one allocation from ${cardName}${inline ? ' in allocation list' : ''}`}
        style={buttonStyle} disabled={allocated === 0}
        onClick={() => useGameStore.getState().decrementDecisionSelection(cardId)}
      >−</button>
      {!compact && <span style={{ color: '#eab308', minWidth: 36, flexShrink: 0,
        fontSize: 12, textAlign: 'center', whiteSpace: 'nowrap' }}>{allocated} / {capacity}</span>}
      <button
        type="button" aria-label={`Allocate one counter from ${cardName}${inline ? ' in allocation list' : ''}`}
        style={buttonStyle} disabled={allocated >= capacity || totalAtMax}
        onClick={() => useGameStore.getState().toggleDecisionSelection(cardId)}
      >+</button>
    </div>
  )
}
