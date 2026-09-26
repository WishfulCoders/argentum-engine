import { describe, it, expect } from 'vitest'
import { battlefieldIdsAlreadyPublic } from './gameplayHandlers'
import type { ClientGameState } from '@/types'
import { entityId } from '@/types'

/**
 * A revealed card that is already on the battlefield is pulsed in place rather than shown in the
 * reveal overlay. "Already" has to mean before the update too: a reveal-until effect that puts the
 * matching card onto the battlefield used to drop that card — the one the reveal was for — from
 * the overlay.
 */

function state(zones: Record<string, string[]>): ClientGameState {
  return {
    zones: Object.entries(zones).map(([zoneType, ids]) => ({
      zoneId: { zoneType, ownerId: entityId('p1') },
      cardIds: ids.map(entityId),
      size: ids.length,
    })),
  } as unknown as ClientGameState
}

describe('battlefieldIdsAlreadyPublic', () => {
  it('excludes a card that came from the library in this update — it was hidden when revealed', () => {
    const before = state({ Battlefield: ['bear'], Library: ['land', 'giant'] })
    const after = state({ Battlefield: ['bear', 'giant'], Library: ['land'] })

    expect([...battlefieldIdsAlreadyPublic(before, after)]).toEqual([entityId('bear')])
  })

  it('keeps a permanent that stayed on the battlefield — beholding it is not a reveal', () => {
    const before = state({ Battlefield: ['elf'] })
    const after = state({ Battlefield: ['elf'] })

    expect(battlefieldIdsAlreadyPublic(before, after).has(entityId('elf'))).toBe(true)
  })

  it('falls back to the new state alone when there is no previous state', () => {
    const after = state({ Battlefield: ['elf'] })

    expect(battlefieldIdsAlreadyPublic(null, after).has(entityId('elf'))).toBe(true)
  })
})
