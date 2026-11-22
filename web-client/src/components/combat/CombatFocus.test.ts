import { describe, expect, it } from 'vitest'
import type { EntityId } from '@/types'
import { computeCombatClique, type BlockEdge } from './combatClique'
import { describeFocus } from './CombatFocus'

const id = (s: string) => s as EntityId
const edge = (blocker: string, attacker: string): BlockEdge => ({ blockerId: id(blocker), attackerId: id(attacker) })
const attackers = new Set([id('A1'), id('A2'), id('A3')])

function focus(hovered: string, edges: BlockEdge[], bands: EntityId[][] = [], blocksKnown = true, blockedWithoutBlockers: EntityId[] = []) {
  const clique = computeCombatClique(id(hovered), edges, bands, attackers)!
  return describeFocus(id(hovered), clique, edges, bands, blocksKnown, new Set(blockedWithoutBlockers))
}

describe('describeFocus', () => {
  it('a blocker lists the attackers it blocks', () => {
    expect(focus('B1', [edge('B1', 'A1'), edge('B1', 'A2')])).toEqual({ role: 'blocking', others: [id('A1'), id('A2')], bandSize: 0 })
  })

  it('an attacker lists only its own blockers, not the wider clique', () => {
    const edges = [edge('B1', 'A1'), edge('B1', 'A2'), edge('B2', 'A2')]
    expect(focus('A1', edges)).toEqual({ role: 'blockedBy', others: [id('B1')], bandSize: 0 })
  })

  it('a banded attacker with no block of its own is blocked by its band mates\' blockers', () => {
    const bands = [[id('A1'), id('A2')]]
    expect(focus('A2', [edge('B1', 'A1')], bands)).toEqual({ role: 'blockedBy', others: [id('B1')], bandSize: 2 })
  })

  it('says unblocked only once blocks are known', () => {
    expect(focus('A3', [edge('B1', 'A1')])?.role).toBe('unblocked')
    expect(focus('A3', [], [], false)).toBeNull()
  })

  it('an attacker whose blockers all left combat stays blocked, not unblocked', () => {
    expect(focus('A3', [], [], true, [id('A3')])).toEqual({ role: 'blocked', others: [], bandSize: 0 })
  })

  it('shows just the band before blocks are known', () => {
    expect(focus('A1', [], [[id('A1'), id('A2')]], false)).toEqual({ role: null, others: [], bandSize: 2 })
  })
})
