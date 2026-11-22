/**
 * The search help panel's cheat sheet: example queries grouped by what the reader is trying to
 * find, each with a plain-words answer. Written for someone who has never used Scryfall — every
 * example is clickable, and `searchExamples.test.ts` checks each one parses cleanly.
 */
export interface SearchExample {
  query: string
  finds: string
}

export interface SearchExampleGroup {
  title: string
  examples: SearchExample[]
}

export const SEARCH_EXAMPLE_GROUPS: SearchExampleGroup[] = [
  {
    title: 'Name and text',
    examples: [
      { query: 'bolt', finds: 'Names containing “bolt”' },
      { query: '!"Lightning Bolt"', finds: 'Exactly this card' },
      { query: 'o:"draw a card"', finds: 'Rules text containing a phrase' },
      { query: 'kw:flying', finds: 'Cards with flying' },
    ],
  },
  {
    title: 'Type',
    examples: [
      { query: 't:creature', finds: 'Creatures' },
      { query: 't:goblin', finds: 'Goblins' },
      { query: 't:"legendary creature"', finds: 'Legendary creatures' },
      { query: 'is:permanent', finds: 'Anything that stays on the battlefield' },
    ],
  },
  {
    title: 'Colour',
    examples: [
      { query: 'c:r', finds: 'Red cards (multicolour ones too)' },
      { query: 'c=wu', finds: 'Exactly white and blue' },
      { query: 'c<=rg', finds: 'Fits a red-green deck' },
      { query: 'c:izzet', finds: 'Guild names work: izzet, golgari, …' },
      { query: 'c:colorless', finds: 'Colourless cards' },
    ],
  },
  {
    title: 'Cost and stats',
    examples: [
      { query: 'mv<=2', finds: 'Mana value 2 or less' },
      { query: 'pow>=4', finds: 'Power 4 or more' },
      { query: 'tou<=1', finds: 'Toughness 1 or less' },
      { query: 'pow>tou', finds: 'More power than toughness' },
      { query: 'mana:{1}{G}', finds: 'Costs exactly {1}{G}' },
    ],
  },
  {
    title: 'Set, rarity and format',
    examples: [
      { query: 's:blb', finds: 'Printed in Bloomburrow' },
      { query: 'r:mythic', finds: 'Mythic rares' },
      { query: 'r>=rare', finds: 'Rare or better' },
      { query: 'f:standard', finds: 'Legal in Standard' },
    ],
  },
  {
    title: 'Combine',
    examples: [
      { query: 't:goblin c:r', finds: 'A space means and: red goblins' },
      { query: 't:elf or t:goblin', finds: 'Either one' },
      { query: '-t:creature', finds: 'A minus excludes: non-creatures' },
      { query: '(c:u or c:b) t:creature', finds: 'Brackets group: blue or black creatures' },
    ],
  },
]
