import { describe, expect, it } from 'vitest'
import { balanceDeckColumns, deckColumnCount, type ColumnGroup } from './deckColumns'

const group = (label: string, n: number): ColumnGroup<string> => ({
  label,
  total: n,
  entries: Array.from({ length: n }, (_, i) => `${label}${i}`),
})

const rows = (col: ReturnType<typeof balanceDeckColumns<string>>[number]) =>
  col.reduce((sum, slice) => sum + slice.entries.length, 0)

describe('balanceDeckColumns', () => {
  it('keeps every card, in order, exactly once', () => {
    const groups = [group('Creatures', 33), group('Instants', 12), group('Sorceries', 10), group('Lands', 13)]
    for (const k of [1, 2, 3, 4, 5, 6]) {
      const flat = balanceDeckColumns(groups, k).flat().flatMap((s) => s.entries)
      expect(flat).toEqual(groups.flatMap((g) => g.entries))
    }
  })

  it('continues a tall group into the next column instead of leaving columns empty', () => {
    // The limited-deck shape: one tall group and several short ones.
    const groups = [group('Creatures', 16), group('Instants', 4), group('Sorceries', 3), group('Artifacts', 4), group('Enchantments', 1), group('Lands', 6)]
    const cols = balanceDeckColumns(groups, 4)
    expect(cols).toHaveLength(4)
    const heights = cols.map(rows)
    expect(Math.max(...heights)).toBeLessThan(16)
    expect(cols.flat().filter((s) => s.label === 'Creatures').map((s) => s.continued)).toEqual([false, true])
  })

  it('never splits a short group or strands fewer than three rows', () => {
    const groups = [group('Creatures', 20), group('Instants', 5), group('Sorceries', 6), group('Lands', 17)]
    for (const k of [2, 3, 4, 5]) {
      const slices = balanceDeckColumns(groups, k).flat()
      for (const short of ['Instants', 'Sorceries']) expect(slices.filter((s) => s.label === short)).toHaveLength(1)
      for (const s of slices) {
        const whole = groups.find((g) => g.label === s.label)!
        if (s.entries.length < whole.entries.length) expect(s.entries.length).toBeGreaterThanOrEqual(3)
      }
    }
  })

  it('carries the whole group total on every piece', () => {
    const cols = balanceDeckColumns([group('Creatures', 30)], 3)
    expect(cols.flat().every((s) => s.total === 30)).toBe(true)
  })

  it('returns nothing for an empty deck', () => {
    expect(balanceDeckColumns([group('Creatures', 0)], 3)).toEqual([])
  })
})

describe('deckColumnCount', () => {
  it('fits columns to the width', () => {
    const big = [group('Creatures', 80)]
    expect(deckColumnCount(1100, big, { minWidth: 250, gap: 16 })).toBe(4)
    expect(deckColumnCount(300, big, { minWidth: 250, gap: 16 })).toBe(1)
  })

  it("doesn't spread a small deck thin", () => {
    expect(deckColumnCount(2000, [group('Creatures', 6), group('Lands', 5)], { minWidth: 250, gap: 16 })).toBe(2)
  })
})
