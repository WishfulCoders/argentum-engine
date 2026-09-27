import { afterEach, describe, expect, it } from 'vitest'
import type { ReorderLibraryDecision, SelectCardsDecision, YesNoDecision } from '@/types'
import { entityId } from '@/types/entities.ts'
import {
  clearPlannedOrder,
  initialLaneState,
  isLaneDecision,
  isLibraryTopLabel,
  lanesFor,
  moveCard,
  notePendingDecision,
  planLibraryOrder,
  plannedOrderFor,
} from './libraryLanes'

const ids = (...names: string[]) => names.map(entityId)
const a = entityId('a')
const b = entityId('b')
const c = entityId('c')

/** The select decision scry N raises (LibraryPatterns.scryPipeline). */
function scry(names: string[], over: Partial<SelectCardsDecision> = {}): SelectCardsDecision {
  const cards = ids(...names)
  return {
    type: 'SelectCardsDecision',
    id: 'd-scry',
    playerId: entityId('p1'),
    prompt: 'Scry',
    context: {},
    options: cards,
    minSelections: 0,
    maxSelections: cards.length,
    ordered: false,
    selectedLabel: 'Put on bottom',
    remainderLabel: 'Put on top',
    ...over,
  } as unknown as SelectCardsDecision
}

function reorder(names: string[], id = 'd-order'): ReorderLibraryDecision {
  return { type: 'ReorderLibraryDecision', id, playerId: entityId('p1'), prompt: '', context: {}, cards: ids(...names), cardInfo: {} } as unknown as ReorderLibraryDecision
}

afterEach(() => clearPlannedOrder())

describe('isLibraryTopLabel', () => {
  it('reads the labels the SDK and card scripts use', () => {
    expect(isLibraryTopLabel('Put on top')).toBe(true)
    expect(isLibraryTopLabel('Leave on top of your library')).toBe(true)
    expect(isLibraryTopLabel('Put on bottom')).toBe(false)
    expect(isLibraryTopLabel('Put in graveyard')).toBe(false)
    expect(isLibraryTopLabel('Put on the bottom of your library')).toBe(false)
  })
})

describe('isLaneDecision', () => {
  it('takes scry and surveil', () => {
    expect(isLaneDecision(scry(['a', 'b']))).toBe(true)
    expect(isLaneDecision(scry(['a'], { selectedLabel: 'Put in graveyard' }))).toBe(true)
  })

  it('leaves unlabelled and restricted selections to the grid picker', () => {
    expect(isLaneDecision(scry(['a'], { selectedLabel: null }))).toBe(false)
    expect(isLaneDecision(scry(['a', 'b'], { onePerColor: true }))).toBe(false)
    expect(isLaneDecision(scry(['a', 'b'], { maxTotalManaValue: 3 }))).toBe(false)
    expect(isLaneDecision(scry(['a', 'b'], { ordered: true }))).toBe(false)
    expect(isLaneDecision(scry(Array.from({ length: 13 }, (_, i) => `c${i}`)))).toBe(false)
  })
})

describe('lanesFor', () => {
  it('puts the library-top lane first whichever half it is', () => {
    expect(lanesFor(scry(['a'])).map((l) => l.key)).toEqual(['remainder', 'selected'])
    const topSelected = scry(['a'], { selectedLabel: 'Put on top of your library', remainderLabel: 'Put into your graveyard' })
    expect(lanesFor(topSelected).map((l) => [l.key, l.isLibraryTop])).toEqual([['selected', true], ['remainder', false]])
  })
})

describe('moveCard', () => {
  const all = new Set([a, b, c])
  const start = initialLaneState(scry(['a', 'b', 'c']))

  it('starts every card where an empty selection sends it', () => {
    expect(start).toEqual({ selected: [], remainder: [a, b, c] })
  })

  it('moves between lanes at an index, and to the end without one', () => {
    const s1 = moveCard(start, b, 'selected', undefined, all, 3)
    expect(s1).toEqual({ selected: [b], remainder: [a, c] })
    const s2 = moveCard(s1, c, 'selected', 0, all, 3)
    expect(s2).toEqual({ selected: [c, b], remainder: [a] })
  })

  it('reorders within a lane, counting the gap the card leaves', () => {
    // Drop 'a' just before 'c' (insertion index 2 in the lane as drawn).
    expect(moveCard(start, a, 'remainder', 2, all, 3).remainder).toEqual([b, a, c])
    expect(moveCard(start, c, 'remainder', 0, all, 3).remainder).toEqual([c, a, b])
    expect(moveCard(start, a, 'remainder', 3, all, 3).remainder).toEqual([b, c, a])
  })

  it('refuses to overfill the selected lane or select a locked card', () => {
    const one = moveCard(start, a, 'selected', undefined, all, 1)
    expect(moveCard(one, b, 'selected', undefined, all, 1)).toBe(one)
    expect(moveCard(start, a, 'selected', undefined, new Set([b]), 3)).toBe(start)
    // Reordering inside a full selected lane is still allowed.
    const two = { selected: [a, b], remainder: [c] }
    expect(moveCard(two, b, 'selected', 0, all, 2).selected).toEqual([b, a])
  })
})

describe('planned library order', () => {
  it('answers the reorder of exactly the arranged cards', () => {
    planLibraryOrder('d-scry', [b, a])
    expect(plannedOrderFor(reorder(['a'], 'd-1'))).toBeNull()
    expect(plannedOrderFor(reorder(['a', 'c'], 'd-2'))).toBeNull()
    expect(plannedOrderFor(reorder(['a', 'b'], 'd-3'))).toEqual([b, a])
  })

  it('is dropped by any unrelated decision in between', () => {
    planLibraryOrder('d-scry', [a])
    notePendingDecision(scry(['a']))
    expect(plannedOrderFor(reorder(['a']))).toEqual([a])
    notePendingDecision({ type: 'YesNoDecision', id: 'd-other' } as unknown as YesNoDecision)
    expect(plannedOrderFor(reorder(['a']))).toBeNull()
  })

  it('answers one reorder only', () => {
    planLibraryOrder('d-scry', [a])
    expect(plannedOrderFor(reorder(['a'], 'd-first'))).toEqual([a])
    // The same decision asks again on every re-render until the server moves on.
    expect(plannedOrderFor(reorder(['a'], 'd-first'))).toEqual([a])
    expect(plannedOrderFor(reorder(['a'], 'd-later'))).toBeNull()
  })

  it('an empty top lane plans nothing', () => {
    planLibraryOrder('d-scry', [])
    expect(plannedOrderFor(reorder([]))).toBeNull()
  })
})
