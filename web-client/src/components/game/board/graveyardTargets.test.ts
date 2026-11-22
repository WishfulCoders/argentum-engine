import { describe, it, expect } from 'vitest'
import { collectGraveyardTargets, zoneCardTargetLabels } from './graveyardTargets'
import type { ClientCard } from '@/types/gameState'
import { ZoneType, entityId } from '@/types'

function card(over: { id: string } & Record<string, unknown>): ClientCard {
  return { name: over.id, targets: [], ...over } as unknown as ClientCard
}
const inGraveyard = (id: string, ownerId: string) =>
  card({ id, name: id, zone: { zoneType: ZoneType.GRAVEYARD, ownerId } })
const targeting = (id: string, name: string, ...cardIds: string[]) =>
  card({ id, name, targets: cardIds.map((cardId) => ({ type: 'Card', cardId })) })

describe('collectGraveyardTargets', () => {
  const yard = [inGraveyard('bears', 'p2'), inGraveyard('elves', 'p2'), inGraveyard('bolt', 'p2')]

  it('finds a buried targeted card, not the top of the pile', () => {
    const result = collectGraveyardTargets([targeting('s1', 'Afterthought Sentry', 'bears')], yard)
    expect(result.map((t) => t.card.id)).toEqual(['bears'])
    expect(result[0]?.sourceNames).toEqual(['Afterthought Sentry'])
  })

  it('ignores cards in other graveyards and non-card targets', () => {
    const stack = [
      targeting('s1', 'Sentry', 'elsewhere'),
      card({ id: 's2', name: 'Shock', targets: [{ type: 'Player', playerId: 'p2' }] }),
    ]
    expect(collectGraveyardTargets(stack, yard)).toEqual([])
  })

  it('orders by the top of the stack and merges sources per card', () => {
    const stack = [targeting('s1', 'Bottom', 'elves', 'bears'), targeting('s2', 'Top', 'bears')]
    const result = collectGraveyardTargets(stack, yard)
    expect(result.map((t) => t.card.id)).toEqual(['bears', 'elves'])
    expect(result[0]?.sourceNames).toEqual(['Top', 'Bottom'])
  })
})

describe('zoneCardTargetLabels', () => {
  const cards = {
    bears: inGraveyard('Grizzly Bears', 'p2'),
    mine: inGraveyard('Shock', 'p1'),
    exiled: card({ id: 'exiled', name: 'Craw Wurm', zone: { zoneType: ZoneType.EXILE, ownerId: 'p2' } }),
    hidden: card({ id: 'hidden', name: 'Secret', zone: { zoneType: ZoneType.HAND, ownerId: 'p2' } }),
  }
  const name = (id: string) => (id === 'p2' ? 'Bob' : 'Alice')

  it('names graveyard and exile targets from the viewer\'s perspective, skipping hidden zones', () => {
    const stackCard = targeting('s', 'Spell', 'bears', 'mine', 'exiled', 'hidden')
    expect(zoneCardTargetLabels(stackCard, cards, entityId('p1'), name)).toEqual([
      "Grizzly Bears (Bob's graveyard)",
      'Shock (your graveyard)',
      'Craw Wurm (in exile)',
    ])
  })

  it('names the zone once for several targets in it', () => {
    const more = { ...cards, elves: inGraveyard('Llanowar Elves', 'p2') }
    const stackCard = targeting('s', 'Rag Dealer', 'bears', 'elves')
    expect(zoneCardTargetLabels(stackCard, more, entityId('p1'), name)).toEqual([
      "Grizzly Bears, Llanowar Elves (Bob's graveyard)",
    ])
  })
})
