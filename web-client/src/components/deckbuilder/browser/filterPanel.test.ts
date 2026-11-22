import { describe, expect, it } from 'vitest'
import { addToken, appendTerm, hasToken, queryTerms, removeToken, toggleToken } from '../cardFilter'
import { extractRange, mvClick, setRange } from './FilterSection'

describe('chip helpers work on top-level terms', () => {
  it('splits a query the way the parser does', () => {
    expect(queryTerms('t:goblin (c:r or c:g) -kw:haste')).toEqual(['t:goblin', '(c:r or c:g)', '-kw:haste'])
    expect(queryTerms('t:elf or t:goblin')).toEqual([])
  })

  it('brackets an or-query before narrowing it', () => {
    expect(appendTerm('t:elf or t:goblin', 'c:g')).toBe('(t:elf or t:goblin) c:g')
    expect(addToken('t:elf', 'c:g')).toBe('t:elf c:g')
    expect(addToken('', 'c:g')).toBe('c:g')
  })

  it('never reaches inside a group the user wrote', () => {
    expect(hasToken('(c:r or t:goblin) mv<=2', 't:goblin')).toBe(false)
    expect(toggleToken('(c:r or t:goblin) mv<=2', 't:goblin')).toBe('(c:r or t:goblin) mv<=2 t:goblin')
    expect(removeToken('t:goblin "goblin king" c:r', 'c:r')).toBe('t:goblin "goblin king"')
  })
})

describe('range terms', () => {
  it('reads typed aliases and exact values', () => {
    expect(extractRange('mv<=2 t:goblin', 'cmc')).toEqual([null, 2])
    expect(extractRange('cmc=3', 'cmc')).toEqual([3, 3])
    expect(extractRange('power>=4', 'pow')).toEqual([4, null])
  })

  it('writes one exact term for a single value and replaces aliases', () => {
    expect(setRange('mv<=2 t:goblin', 'cmc', 3, 3)).toBe('t:goblin cmc=3')
    expect(setRange('t:elf or t:goblin', 'cmc', 1, 2)).toBe('(t:elf or t:goblin) cmc>=1 cmc<=2')
  })
})

describe('mana value chips', () => {
  it('picks, stretches, shrinks and clears a range', () => {
    expect(mvClick(null, null, 2)).toEqual([2, 2])
    expect(mvClick(2, 2, 4)).toEqual([2, 4])
    expect(mvClick(2, 4, 4)).toEqual([2, 3])
    expect(mvClick(2, 4, 2)).toEqual([3, 4])
    expect(mvClick(2, 4, 3)).toEqual([3, 3])
    expect(mvClick(3, 3, 3)).toEqual([null, null])
  })

  it('treats 7 as 7-or-more', () => {
    expect(mvClick(null, null, 7)).toEqual([7, null])
    expect(mvClick(7, null, 7)).toEqual([null, null])
    expect(mvClick(5, 5, 7)).toEqual([5, null])
    expect(mvClick(5, null, 7)).toEqual([5, 6])
  })
})
