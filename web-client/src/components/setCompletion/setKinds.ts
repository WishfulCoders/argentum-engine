/**
 * What kind of product a set is, for the Set Completion view.
 *
 * Scryfall's `set_type` has ~20 values, far too many to filter by, and most of them hold a handful
 * of sets each. They fold into five kinds that answer the question a reader actually has — "is this a
 * set people draft, a box of preconstructed decks, or a joke?" — while the badge on each set still
 * names its exact type ("Duel Deck", "Masters"), so nothing Scryfall says is lost.
 *
 * Main sets carry no badge: they are most of the list, and a badge on everything marks nothing.
 */

export type SetKind = 'main' | 'commander' | 'supplemental' | 'precon' | 'funny'

export interface SetKindInfo {
  readonly label: string
  /** Filter-chip and tooltip text: what belongs in this kind. */
  readonly description: string
}

export const SET_KINDS: Record<SetKind, SetKindInfo> = {
  main: {
    label: 'Main sets',
    description: 'Premier expansions and core sets — the ones that rotate through Standard.',
  },
  commander: {
    label: 'Commander',
    description: 'Preconstructed Commander decks, with the new cards printed for them.',
  },
  supplemental: {
    label: 'Supplemental',
    description: 'Booster sets outside the main line: Masters, Modern Horizons, Conspiracy, Jumpstart, starter sets.',
  },
  precon: {
    label: 'Decks & boxes',
    description: 'Preconstructed products: Duel Decks, Planechase, Archenemy, From the Vault, gift boxes.',
  },
  funny: {
    label: 'Un-sets',
    description: 'Silver-bordered and acorn sets — not legal in sanctioned formats.',
  },
}

/** Display order for the filter chips. */
export const SET_KIND_ORDER: readonly SetKind[] = ['main', 'commander', 'supplemental', 'precon', 'funny']

const KIND_BY_TYPE: Record<string, SetKind> = {
  expansion: 'main',
  core: 'main',
  commander: 'commander',
  draft_innovation: 'supplemental',
  masters: 'supplemental',
  eternal: 'supplemental',
  starter: 'supplemental',
  alchemy: 'supplemental',
  duel_deck: 'precon',
  planechase: 'precon',
  archenemy: 'precon',
  from_the_vault: 'precon',
  premium_deck: 'precon',
  spellbook: 'precon',
  box: 'precon',
  treasure_chest: 'precon',
  arsenal: 'precon',
  vanguard: 'precon',
  funny: 'funny',
}

/** Scryfall's types in the words printed on the product, for the per-set badge. */
const TYPE_LABELS: Record<string, string> = {
  commander: 'Commander',
  draft_innovation: 'Supplemental',
  masters: 'Masters',
  eternal: 'Eternal',
  starter: 'Starter',
  alchemy: 'Alchemy',
  duel_deck: 'Duel Deck',
  planechase: 'Planechase',
  archenemy: 'Archenemy',
  from_the_vault: 'From the Vault',
  premium_deck: 'Premium Deck',
  spellbook: 'Signature Spellbook',
  box: 'Box set',
  treasure_chest: 'Treasure Chest',
  arsenal: 'Arsenal',
  vanguard: 'Vanguard',
  funny: 'Un-set',
}

/** A set whose type we don't recognise (or Scryfall didn't report) counts as a main set, unbadged. */
export function setKind(setType: string | null): SetKind {
  return (setType && KIND_BY_TYPE[setType]) || 'main'
}

/** The badge text for a set, or null for a main set. */
export function setTypeLabel(setType: string | null): string | null {
  if (setKind(setType) === 'main' || !setType) return null
  return TYPE_LABELS[setType] ?? setType.replace(/_/g, ' ')
}
