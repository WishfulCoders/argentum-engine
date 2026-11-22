import { describe, expect, it } from 'vitest'
import {
  activeQueues,
  formatWait,
  normaliseQueue,
  queueLabel,
  searchingIn,
  searchingInMode,
} from './queues'

describe('matchmaking queue helpers', () => {
  const counts = [
    { mode: 'RANDOM_DECK' as const, format: null, ranked: false, searching: 1 },
    { mode: 'CONSTRUCTED' as const, format: 'PAUPER' as const, ranked: true, searching: 3 },
    { mode: 'CONSTRUCTED' as const, format: 'PAUPER' as const, ranked: false, searching: 0 },
    { mode: 'CONSTRUCTED' as const, format: 'MODERN' as const, ranked: false, searching: 2 },
    { mode: 'JUMP_IN' as const, format: null, ranked: false, searching: 1 },
  ]

  it('reads one queue, keeping modes, formats, casual and ranked apart', () => {
    expect(searchingIn(counts, { mode: 'CONSTRUCTED', format: 'PAUPER', ranked: true })).toBe(3)
    expect(searchingIn(counts, { mode: 'CONSTRUCTED', format: 'PAUPER', ranked: false })).toBe(0)
    expect(searchingIn(counts, { mode: 'RANDOM_DECK', format: null, ranked: false })).toBe(1)
    expect(searchingIn(counts, { mode: 'MOMIR_BASIC', format: null, ranked: false })).toBe(0)
    expect(searchingIn(null, { mode: 'RANDOM_DECK', format: null, ranked: false })).toBe(0)
  })

  it('totals a mode across its formats and both ladders', () => {
    expect(searchingInMode(counts, 'CONSTRUCTED')).toBe(5)
    expect(searchingInMode(counts, 'JUMP_IN')).toBe(1)
    expect(searchingInMode(null, 'MOMIR_BASIC')).toBe(0)
  })

  it('lists only queues with someone in them, busiest first', () => {
    expect(activeQueues(counts).map((c) => c.searching)).toEqual([3, 2, 1, 1])
  })

  it('names a queue, leaving "Casual" off modes that are never ranked', () => {
    expect(queueLabel('RANDOM_DECK', null, false)).toBe('Casual Random deck')
    expect(queueLabel('CONSTRUCTED', 'STANDARD_BRAWL', true)).toBe('Ranked Standard Brawl')
    expect(queueLabel('JUMP_IN', null, false)).toBe('Jump In')
    expect(queueLabel('MOMIR_BASIC', null, false)).toBe('Momir Basic')
  })

  it('normalises a stored choice to a queue the server accepts', () => {
    expect(normaliseQueue({})).toEqual({ mode: 'RANDOM_DECK', format: null, ranked: false })
    expect(normaliseQueue({ mode: 'CONSTRUCTED' })).toEqual({ mode: 'CONSTRUCTED', format: 'STANDARD', ranked: false })
    expect(normaliseQueue({ mode: 'JUMP_IN', format: 'PAUPER', ranked: true }))
      .toEqual({ mode: 'JUMP_IN', format: null, ranked: false })
    expect(normaliseQueue({ mode: 'CONSTRUCTED', format: 'PAUPER', ranked: true }))
      .toEqual({ mode: 'CONSTRUCTED', format: 'PAUPER', ranked: true })
  })

  it('formats a wait as m:ss', () => {
    expect(formatWait(7_400)).toBe('0:07')
    expect(formatWait(161_000)).toBe('2:41')
    expect(formatWait(-5)).toBe('0:00')
  })
})
