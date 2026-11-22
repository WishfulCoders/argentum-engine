import type { ClientCard, EntityId } from '@/types'
import { ZoneType } from '@/types'

/** A graveyard card that one or more stack objects currently target. */
export interface GraveyardTarget {
  readonly card: ClientCard
  /** Names of the stack objects targeting it, top of the stack first, de-duplicated. */
  readonly sourceNames: readonly string[]
}

/**
 * The cards in one graveyard that objects on the stack target, in the order they were first
 * targeted (walking the stack top-down). Graveyard cards are tiny on the board and usually buried
 * under the top card, so the board pulls these out into a readable callout.
 */
export function collectGraveyardTargets(
  stackCards: readonly ClientCard[],
  graveyardCards: readonly ClientCard[],
): GraveyardTarget[] {
  if (graveyardCards.length === 0) return []
  const byId = new Map(graveyardCards.map((c) => [c.id, c]))
  const order: EntityId[] = []
  const sources = new Map<EntityId, string[]>()
  // The stack selector returns bottom-first; the top object is the one about to resolve.
  for (let i = stackCards.length - 1; i >= 0; i--) {
    const stackCard = stackCards[i]
    if (!stackCard?.targets) continue
    for (const target of stackCard.targets) {
      if (target.type !== 'Card' || !byId.has(target.cardId)) continue
      let names = sources.get(target.cardId)
      if (!names) {
        names = []
        sources.set(target.cardId, names)
        order.push(target.cardId)
      }
      if (!names.includes(stackCard.name)) names.push(stackCard.name)
    }
  }
  return order.map((id) => ({ card: byId.get(id)!, sourceNames: sources.get(id) ?? [] }))
}

/**
 * Readable names for a stack object's card targets that sit in a public zone (graveyard, exile),
 * e.g. "Grizzly Bears (your graveyard)". Targets in the same zone share one suffix:
 * "Serra Angel, Counterspell (Alice's graveyard)". Permanents and players already get an
 * unambiguous arrow; a card in a pile doesn't, so the stack's text box spells these out. Cards in
 * hidden zones are skipped — the client can't (and mustn't) name them.
 */
export function zoneCardTargetLabels(
  stackCard: ClientCard,
  cards: Readonly<Record<EntityId, ClientCard>>,
  viewerId: EntityId | null,
  playerName: (playerId: EntityId) => string,
): string[] {
  const groups: { zone: string; names: string[] }[] = []
  for (const target of stackCard.targets ?? []) {
    if (target.type !== 'Card') continue
    const card = cards[target.cardId]
    const zone = card?.zone
    if (!card || !zone) continue
    let zoneLabel: string
    if (zone.zoneType === ZoneType.GRAVEYARD) {
      zoneLabel = `${zone.ownerId === viewerId ? 'your' : `${playerName(zone.ownerId)}'s`} graveyard`
    } else if (zone.zoneType === ZoneType.EXILE) {
      zoneLabel = 'in exile'
    } else {
      continue
    }
    const group = groups.find((g) => g.zone === zoneLabel)
    if (group) group.names.push(card.name)
    else groups.push({ zone: zoneLabel, names: [card.name] })
  }
  return groups.map((g) => `${g.names.join(', ')} (${g.zone})`)
}
