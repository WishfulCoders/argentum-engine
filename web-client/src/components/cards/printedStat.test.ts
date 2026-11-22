import { describe, expect, it } from 'vitest'
import { printedStat } from './CardDetailDialog'

describe('printedStat', () => {
  it('reads a fixed value', () => expect(printedStat('Fixed(value=3)')).toBe('3'))
  it('reads a characteristic-defining value as *', () =>
    expect(printedStat('Dynamic(source=AggregateBattlefield(player=You, aggregation=COUNT))')).toBe('*'))
  it('reads an offset as N+*', () =>
    expect(printedStat('DynamicWithOffset(source=Conditional(ifFalse=Fixed(amount=0)), offset=2)')).toBe('2+*'))
  it('dashes a missing value', () => expect(printedStat(null)).toBe('—'))
})
