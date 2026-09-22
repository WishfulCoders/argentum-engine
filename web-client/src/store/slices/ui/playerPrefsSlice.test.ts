import { describe, it, expect, vi } from 'vitest'
import { create } from 'zustand'
import { applyHandOrder } from './playerPrefsSlice'
import { createCombatSlice } from './combatSlice'
import type { CombatState, GameStore } from '../types'
import type { EntityId } from '@/types'

const id = (s: string): EntityId => s as unknown as EntityId
const cards = (...ids: string[]) => ids.map((i) => ({ id: id(i) }))

vi.mock('../shared', () => ({ getWebSocket: () => null }))

describe('applyHandOrder', () => {
  it('keeps the server order when no order is set', () => {
    const hand = cards('a', 'b', 'c')
    expect(applyHandOrder(hand, [])).toBe(hand)
  })

  it('follows the chosen order and appends fresh draws on the right', () => {
    const hand = cards('a', 'b', 'c', 'd')
    expect(applyHandOrder(hand, [id('c'), id('a'), id('b')]).map((c) => c.id)).toEqual(['c', 'a', 'b', 'd'])
  })

  it('ignores ids that have left the hand', () => {
    const hand = cards('a', 'b')
    expect(applyHandOrder(hand, [id('x'), id('b'), id('a')]).map((c) => c.id)).toEqual(['b', 'a'])
  })
})

function blockState(overrides: Partial<CombatState> = {}): CombatState {
  return {
    interactionEpoch: 'e',
    mode: 'declareBlockers',
    actingSeat: null,
    stickyDefenderId: null,
    selectedAttackers: [],
    attackerTargets: {},
    validAttackTargets: [],
    blockerAssignments: {},
    validCreatures: [id('b1'), id('b2'), id('b3')],
    mandatoryAttackers: [],
    attackingCreatures: [id('x1'), id('x2')],
    mustBeBlockedAttackers: [],
    blockerMaxBlockCounts: {},
    bands: [],
    ...overrides,
  } as CombatState
}

function makeStore(state: CombatState) {
  const store = create<GameStore>()((set, get, api) => ({
    ...createCombatSlice(set, get, api),
    interactionEpoch: 'e',
  }) as unknown as GameStore)
  store.setState({ combatState: state })
  return store
}

describe('click-to-block', () => {
  it('assigns every picked blocker to the clicked attacker, then clears the picks', () => {
    const store = makeStore(blockState())
    store.getState().togglePendingBlocker(id('b1'))
    store.getState().togglePendingBlocker(id('b2'))
    store.getState().assignPendingBlockersTo(id('x1'))
    expect(store.getState().combatState!.blockerAssignments).toEqual({ b1: ['x1'], b2: ['x1'] })
    expect(store.getState().pendingBlockerIds).toEqual([])
  })

  it('a second click unpicks, and non-blockers cannot be picked', () => {
    const store = makeStore(blockState())
    store.getState().togglePendingBlocker(id('b1'))
    store.getState().togglePendingBlocker(id('b1'))
    store.getState().togglePendingBlocker(id('zz'))
    expect(store.getState().pendingBlockerIds).toEqual([])
  })

  it('respects each blocker\'s max block count', () => {
    const store = makeStore(blockState({ blockerAssignments: { [id('b1')]: [id('x2')] } }))
    store.getState().togglePendingBlocker(id('b1'))
    store.getState().assignPendingBlockersTo(id('x1'))
    expect(store.getState().combatState!.blockerAssignments).toEqual({ b1: ['x2'] })
  })

  it('ignores a click on something that is not attacking', () => {
    const store = makeStore(blockState())
    store.getState().togglePendingBlocker(id('b1'))
    store.getState().assignPendingBlockersTo(id('b2'))
    expect(store.getState().combatState!.blockerAssignments).toEqual({})
    expect(store.getState().pendingBlockerIds).toEqual([id('b1')])
  })
})
