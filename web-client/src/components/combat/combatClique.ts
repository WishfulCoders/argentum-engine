import type { EntityId } from '@/types'

/** One drawn block: `blockerId` is blocking `attackerId`. */
export interface BlockEdge {
  readonly blockerId: EntityId
  readonly attackerId: EntityId
}

export interface CombatClique {
  /** Every creature tangled up with the hovered one, the hovered creature included. */
  readonly members: ReadonlySet<EntityId>
  readonly attackers: ReadonlySet<EntityId>
  readonly blockers: ReadonlySet<EntityId>
}

/**
 * The combat "clique" around a hovered creature: the connected component of the block graph
 * (blocker ↔ attacker edges) with every attacking band glued together.
 *
 * A component rather than direct neighbours because combat damage is decided per component:
 * a blocker that blocks two attackers ties both of them to it, each of those attackers drags in
 * its other blockers, and an attacker in a band is blocked whenever any band mate is — so the
 * whole band moves as one. Hovering any creature in that tangle shows all of it.
 *
 * Returns null when the hovered creature isn't in combat at all (`attackers` lists every attacker,
 * blocked or not, so an unblocked attacker still yields a clique of itself plus its band).
 */
export function computeCombatClique(
  hoveredId: EntityId,
  edges: readonly BlockEdge[],
  bands: readonly (readonly EntityId[])[],
  attackers: ReadonlySet<EntityId>,
): CombatClique | null {
  const blockerIds = new Set(edges.map((e) => e.blockerId))
  if (!attackers.has(hoveredId) && !blockerIds.has(hoveredId)) return null

  const adjacency = new Map<EntityId, Set<EntityId>>()
  const link = (a: EntityId, b: EntityId) => {
    if (a === b) return
    let na = adjacency.get(a)
    if (!na) adjacency.set(a, (na = new Set()))
    na.add(b)
    let nb = adjacency.get(b)
    if (!nb) adjacency.set(b, (nb = new Set()))
    nb.add(a)
  }
  for (const e of edges) link(e.blockerId, e.attackerId)
  for (const band of bands) {
    for (let i = 1; i < band.length; i++) link(band[0]!, band[i]!)
  }

  const members = new Set<EntityId>([hoveredId])
  const queue: EntityId[] = [hoveredId]
  while (queue.length > 0) {
    const next = queue.pop()!
    for (const neighbour of adjacency.get(next) ?? []) {
      if (members.has(neighbour)) continue
      members.add(neighbour)
      queue.push(neighbour)
    }
  }

  const cliqueAttackers = new Set<EntityId>()
  const cliqueBlockers = new Set<EntityId>()
  for (const id of members) {
    // A creature is never both in one combat; an id seen only via a band is an attacker.
    if (blockerIds.has(id) && !attackers.has(id)) cliqueBlockers.add(id)
    else cliqueAttackers.add(id)
  }
  return { members, attackers: cliqueAttackers, blockers: cliqueBlockers }
}

/** Groups attackers by their server-assigned band id; unbanded attackers are left out. */
export function bandsFromBandIds(
  attackers: readonly { readonly creatureId: EntityId; readonly bandId?: string | null | undefined }[],
): EntityId[][] {
  const byBand = new Map<string, EntityId[]>()
  for (const a of attackers) {
    if (!a.bandId) continue
    const band = byBand.get(a.bandId)
    if (band) band.push(a.creatureId)
    else byBand.set(a.bandId, [a.creatureId])
  }
  return Array.from(byBand.values())
}
