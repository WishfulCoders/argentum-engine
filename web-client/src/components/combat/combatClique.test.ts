import { describe, expect, it } from 'vitest'
import type { EntityId } from '@/types'
import { bandsFromBandIds, computeCombatClique, type BlockEdge } from './combatClique'

const id = (s: string) => s as EntityId
const edge = (blocker: string, attacker: string): BlockEdge => ({ blockerId: id(blocker), attackerId: id(attacker) })
const set = (...ids: string[]) => new Set(ids.map(id))
const sorted = (s: ReadonlySet<EntityId> | undefined) => [...(s ?? [])].sort()

describe('computeCombatClique', () => {
  const attackers = set('A1', 'A2', 'A3', 'A4')

  it('is null for a creature that is not in combat', () => {
    expect(computeCombatClique(id('X'), [edge('B1', 'A1')], [], attackers)).toBeNull()
  })

  it('an unblocked attacker is a clique of one', () => {
    const clique = computeCombatClique(id('A2'), [edge('B1', 'A1')], [], attackers)
    expect(sorted(clique?.members)).toEqual(['A2'])
    expect(sorted(clique?.blockers)).toEqual([])
  })

  it('an attacker pulls in all of its blockers (gang block)', () => {
    const clique = computeCombatClique(id('A1'), [edge('B1', 'A1'), edge('B2', 'A1'), edge('B3', 'A2')], [], attackers)
    expect(sorted(clique?.attackers)).toEqual(['A1'])
    expect(sorted(clique?.blockers)).toEqual(['B1', 'B2'])
  })

  it('follows a blocker that blocks several attackers through to their other blockers', () => {
    // B1 blocks A1 and A2; A2 is also blocked by B2. Hovering A1 reaches B2 transitively.
    const edges = [edge('B1', 'A1'), edge('B1', 'A2'), edge('B2', 'A2'), edge('B3', 'A3')]
    const clique = computeCombatClique(id('A1'), edges, [], attackers)
    expect(sorted(clique?.attackers)).toEqual(['A1', 'A2'])
    expect(sorted(clique?.blockers)).toEqual(['B1', 'B2'])
    // The same clique from the far end.
    expect(sorted(computeCombatClique(id('B2'), edges, [], attackers)?.members)).toEqual(['A1', 'A2', 'B1', 'B2'])
  })

  it('glues a band together, so blocking one band member brings in the rest', () => {
    const edges = [edge('B1', 'A1'), edge('B2', 'A3')]
    const clique = computeCombatClique(id('B1'), edges, [[id('A1'), id('A2'), id('A3')]], attackers)
    expect(sorted(clique?.attackers)).toEqual(['A1', 'A2', 'A3'])
    expect(sorted(clique?.blockers)).toEqual(['B1', 'B2'])
  })

  it('an unblocked band is still a clique of its members', () => {
    const clique = computeCombatClique(id('A2'), [], [[id('A1'), id('A2')]], attackers)
    expect(sorted(clique?.members)).toEqual(['A1', 'A2'])
  })
})

describe('bandsFromBandIds', () => {
  it('groups by band id and drops unbanded attackers', () => {
    const bands = bandsFromBandIds([
      { creatureId: id('A1'), bandId: 'p' },
      { creatureId: id('A2'), bandId: null },
      { creatureId: id('A3'), bandId: 'p' },
      { creatureId: id('A4') },
    ])
    expect(bands).toEqual([[id('A1'), id('A3')]])
  })
})
