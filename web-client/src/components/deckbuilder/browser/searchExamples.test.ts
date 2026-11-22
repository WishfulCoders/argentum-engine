import { describe, expect, it } from 'vitest'
import { parseQuery } from '../query'
import { SEARCH_EXAMPLE_GROUPS } from './searchExamples'

/** A cheat sheet that teaches a query the parser rejects is worse than none. */
describe('search help examples', () => {
  for (const { query } of SEARCH_EXAMPLE_GROUPS.flatMap((g) => g.examples)) {
    it(`parses ${query}`, () => {
      expect({ query, errors: parseQuery(query).errors.map((e) => e.message) }).toEqual({ query, errors: [] })
    })
  }
})
