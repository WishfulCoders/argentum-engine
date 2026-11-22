import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createSelectionSlice } from './selectionSlice'
import type { GameStore, EntityId } from '../types'
const id = (s: string) => s as EntityId

function allocationStore() {
  const store = create<GameStore>()((set, get, api) => ({
    ...createSelectionSlice(set, get, api),
  }) as unknown as GameStore)
  store.getState().startDecisionSelection({ decisionId: 'payment', prompt: 'Remove three counters',
    validOptions: [id('a'), id('b')], selectedOptions: [], minSelections: 0, maxSelections: 3,
    selectionLimits: { [id('a')]: 3, [id('b')]: 1 } })
  return store
}

describe('battlefield counter allocation', () => {
  it('allocates several counters on one permanent and removes just one with minus', () => {
    const store = allocationStore()
    for (let i = 0; i < 3; i++) store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('a'), id('a')])
    store.getState().decrementDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('a')])
    store.getState().toggleDecisionSelection(id('b'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('a'), id('b')])
  })
  it('respects server capacities and the total cap without deleting existing allocations', () => {
    const store = allocationStore()
    store.getState().toggleDecisionSelection(id('b'))
    store.getState().toggleDecisionSelection(id('b'))
    store.getState().toggleDecisionSelection(id('unknown'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('b')])
    store.getState().toggleDecisionSelection(id('a'))
    store.getState().toggleDecisionSelection(id('a'))
    store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('b'), id('a'), id('a')])
    store.getState().decrementDecisionSelection(id('b'))
    store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('a'), id('a')])
  })
  it('keeps ordinary selection as a distinct-card toggle', () => {
    const store = allocationStore()
    store.getState().startDecisionSelection({ decisionId: 'sacrifice', prompt: 'Sacrifice',
      validOptions: [id('a')], selectedOptions: [], minSelections: 1, maxSelections: 1 })
    store.getState().toggleDecisionSelection(id('a'))
    store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([])
  })
})
