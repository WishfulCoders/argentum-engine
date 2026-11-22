/**
 * Lays the Deck list's type groups (Creatures, Instants, Lands, …) out in balanced columns.
 *
 * CSS multi-column can't do this: with `break-inside: avoid` on each group the tallest group sets
 * every column's height, so a 16-creature limited deck fills three columns and leaves the rest of a
 * wide screen empty — and a 35-creature Commander deck is one very tall column. Here a big group may
 * continue into the next column (under a "continued" header), so the columns come out level and the
 * whole deck fits in as little height as the width allows.
 *
 * Small groups never split, and a split never strands fewer than {@link MIN_PIECE} rows on either
 * side, so a continuation always reads as a real list rather than a stray line.
 */

/** Row-height units: a card row is 1; a group header and the gap above it cost a little more. */
const HEADER = 1.6
const GROUP_GAP = 0.8
/** Groups this short always stay whole — splitting four instants two-and-two helps nobody. */
const UNSPLITTABLE = 6
/** The fewest rows either half of a split group may hold. */
const MIN_PIECE = 3
/** Below this many rows per column, a column isn't worth opening: fewer, fuller columns read better. */
const MIN_ROWS_PER_COLUMN = 10

export interface ColumnGroup<T> {
  label: string
  /** Cards in the whole group (sum of counts), shown in every piece's header. */
  total: number
  entries: readonly T[]
}

export interface ColumnSlice<T> {
  label: string
  total: number
  /** This piece continues a group begun in the previous column. */
  continued: boolean
  entries: readonly T[]
}

/** How many columns of at least `minWidth` fit, capped so a small deck isn't spread thin. */
export function deckColumnCount(
  width: number,
  groups: readonly ColumnGroup<unknown>[],
  { minWidth, gap }: { minWidth: number; gap: number },
): number {
  const byWidth = Math.max(1, Math.floor((width + gap) / (minWidth + gap)))
  const byContent = Math.max(1, Math.ceil(totalHeight(groups) / MIN_ROWS_PER_COLUMN))
  return Math.min(byWidth, byContent)
}

/**
 * Splits `groups`, in order, into at most `columns` columns of as even a height as the rules allow.
 * Returns fewer columns when fewer suffice.
 */
export function balanceDeckColumns<T>(groups: readonly ColumnGroup<T>[], columns: number): ColumnSlice<T>[][] {
  const visible = groups.filter((g) => g.entries.length > 0)
  if (visible.length === 0) return []
  const k = Math.max(1, Math.floor(columns))
  if (k === 1) return [visible.map((g) => ({ label: g.label, total: g.total, continued: false, entries: g.entries }))]

  // The shortest column height that packs into k columns. Each check is linear and the search
  // space is bounded by the deck's own height, so a plain upward scan is cheap enough.
  const total = totalHeight(visible)
  const tallestWhole = Math.max(
    HEADER + MIN_PIECE,
    ...visible.filter((g) => g.entries.length <= UNSPLITTABLE).map((g) => HEADER + g.entries.length),
  )
  let height = Math.max(tallestWhole, Math.ceil(total / k))
  for (;;) {
    const packed = pack(visible, height)
    if (packed.length <= k) return packed
    height += 0.5
  }
}

function totalHeight(groups: readonly ColumnGroup<unknown>[]): number {
  let h = 0
  groups.forEach((g, i) => {
    if (g.entries.length === 0) return
    h += (i > 0 ? GROUP_GAP : 0) + HEADER + g.entries.length
  })
  return h
}

/** Greedy fill to `height`: whole groups where they fit, splits only where the rules allow. */
function pack<T>(groups: readonly ColumnGroup<T>[], height: number): ColumnSlice<T>[][] {
  const out: ColumnSlice<T>[][] = [[]]
  let used = 0
  const newColumn = () => {
    out.push([])
    used = 0
  }
  const headerCost = () => (used > 0 ? GROUP_GAP : 0) + HEADER
  const room = () => Math.floor(height - used - headerCost() + 1e-9)

  for (const group of groups) {
    const n = group.entries.length
    // Whole, here.
    if (n <= room()) {
      used += headerCost() + n
      out[out.length - 1]!.push({ label: group.label, total: group.total, continued: false, entries: group.entries })
      continue
    }
    // Too short to split, or too little room left to start it here: whole, in a fresh column.
    if (n <= UNSPLITTABLE || room() < MIN_PIECE) {
      if (used > 0) newColumn()
      if (n <= room()) {
        used += headerCost() + n
        out[out.length - 1]!.push({ label: group.label, total: group.total, continued: false, entries: group.entries })
        continue
      }
    }
    // Split across as many columns as it takes.
    let start = 0
    while (start < n) {
      const left = n - start
      let take = Math.min(left, room())
      // Leave a tail of at least MIN_PIECE rows, when that keeps this piece long enough too.
      if (left - take > 0 && left - take < MIN_PIECE && left - MIN_PIECE >= MIN_PIECE) take = left - MIN_PIECE
      if (take < Math.min(MIN_PIECE, left)) {
        // A fresh column always has room for MIN_PIECE rows (the height floor guarantees it).
        if (used > 0) {
          newColumn()
          continue
        }
        take = Math.min(left, room())
      }
      used += headerCost() + take
      out[out.length - 1]!.push({
        label: group.label,
        total: group.total,
        continued: start > 0,
        entries: group.entries.slice(start, start + take),
      })
      start += take
      if (start < n) newColumn()
    }
  }
  return out.filter((col) => col.length > 0)
}
