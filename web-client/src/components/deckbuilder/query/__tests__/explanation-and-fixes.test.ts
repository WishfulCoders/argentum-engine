import { describe, expect, it } from 'vitest'
import { applyFix, isAdvancedQuery, parseQuery, SEARCH_KEYS } from '../index'
import { CARDS, names } from './fixtures'

/**
 * What the search bar shows *about* a query — its plain-words reading and one-click fixes — read
 * from the compiled `:mtg-search` module, plus the numeric filters a broken catalog field once hid.
 */
describe('search query explanation', () => {
  const explain = (q: string) => parseQuery(q).explanation

  it('reads a flat query as a sentence', () => {
    expect(explain('bolt pow>=4')).toBe('name contains "bolt" and power ≥ 4')
    expect(explain('t:legendary creature')).toBe('type line has "legendary" and name contains "creature"')
    expect(explain('t:"legendary creature"')).toBe('type line has "legendary" and "creature"')
  })

  it('names colours and the hybrid-aware "at most"', () => {
    expect(explain('c:wu')).toBe('colour identity includes white and blue')
    expect(explain('c=izzet')).toBe('colour identity is exactly blue and red')
    expect(explain('c<=rw')).toBe('playable using only white and red')
  })

  it('compares fields by name', () => {
    expect(explain('pow>tou')).toBe('power > toughness')
    expect(explain('mv<=3')).toBe('mana value ≤ 3')
  })

  it('brackets or-groups and negations', () => {
    expect(explain('(c:u or c:r) t:creature')).toBe(
      '(colour identity includes blue or colour identity includes red) and type line has "creature"',
    )
    expect(explain('-t:creature')).toBe('not (type line has "creature")')
  })

  it('is null for a blank query and for one with errors', () => {
    expect(explain('')).toBeNull()
    expect(explain('powr>3')).toBeNull()
  })
})

describe('search error fixes', () => {
  it('a mistyped key carries a fix that replaces just the key', () => {
    const q = 't:goblin -powr>=3'
    const [error] = parseQuery(q).errors
    expect(error?.fix).toBeDefined()
    expect(applyFix(q, error!.fix!)).toBe('t:goblin -pow>=3')
  })

  it('an error with nothing to suggest has no fix', () => {
    expect(parseQuery('c:k').errors[0]?.fix).toBeUndefined()
  })

  it('rejects regex flags the browser and server would read differently', () => {
    expect(parseQuery('o:/draw/g').errors[0]?.message).toContain('Unknown regex flag')
  })
})

describe('numeric filters', () => {
  const run = (q: string) => names(CARDS.filter(parseQuery(q).predicate))

  it('power and toughness compare their printed numbers', () => {
    expect(run('pow>=4')).toEqual(['Niv-Mizzet, Parun', 'Serra Angel'])
    expect(run('tou<=1')).toEqual(['Llanowar Elves', 'Tarmogoyf']) // Tarmogoyf's 1+* reads as 1
  })

  it('a * power is not a number, so it matches no comparison', () => {
    expect(run('pow>=0')).not.toContain('Tarmogoyf')
  })

  it('loyalty reads the printed starting loyalty', () => {
    const jace = { ...CARDS[0]!, name: 'Jace Test', cardTypes: ['PLANESWALKER'], loyalty: '3' }
    expect(parseQuery('loy=3').predicate(jace)).toBe(true)
    expect(parseQuery('loy>3').predicate(jace)).toBe(false)
  })
})

describe('query metadata', () => {
  it('flags queries the filter chips cannot round-trip', () => {
    expect(isAdvancedQuery('t:creature c:r')).toBe(false)
    expect(isAdvancedQuery('-t:creature')).toBe(false)
    expect(isAdvancedQuery('t:creature or t:planeswalker')).toBe(true)
    expect(parseQuery('(c:r) t:goblin').advanced).toBe(true)
  })

  it('lists every key from the matcher registry', () => {
    const aliases = SEARCH_KEYS.flatMap((k) => k.aliases)
    for (const key of ['name', 't', 'c', 'pow', 'tou', 'loy', 'is', 's', 'f']) expect(aliases).toContain(key)
  })
})
