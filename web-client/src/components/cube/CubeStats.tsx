/**
 * The numbers a curator balances a cube on — colour spread, mana curve, rarity spread — and, for a
 * booster-style cube, the pack itself: one bar per slot, coloured by the rarity that fills it, so
 * "Play Boosters" reads as a pack rather than as a sentence. A slot the cube can't fill is drawn
 * hollow, next to the count that's missing.
 */
import { useMemo } from 'react'
import type { CubeBoosterStyle, SharedCubeEntry } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import {
  CUBE_RARITIES,
  RARITY_LABEL,
  RARITY_LETTER,
  boosterStyleInfo,
  packHealth,
  rarityTallies,
  shuffledSeats,
  type RarityTally,
} from './cubeModel'
import { RARITY_CLASS } from './RarityChip'
import styles from './CubeEditor.module.css'

const COLOR_GROUPS = [
  { key: 'W', enumName: 'WHITE', label: 'White' },
  { key: 'U', enumName: 'BLUE', label: 'Blue' },
  { key: 'B', enumName: 'BLACK', label: 'Black' },
  { key: 'R', enumName: 'RED', label: 'Red' },
  { key: 'G', enumName: 'GREEN', label: 'Green' },
] as const

const COLOR_KEY_BY_ENUM: Record<string, string> = Object.fromEntries(
  COLOR_GROUPS.map((g) => [g.enumName, g.key]),
)

/** Colour spread (by identity, physical copies) and the non-land mana curve, 0–7+. */
export function colorAndCurve(entries: readonly SharedCubeEntry[], lookup: (name: string) => CardSummary | undefined) {
  const colors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0, C: 0, M: 0 }
  const curve = [0, 0, 0, 0, 0, 0, 0, 0]
  for (const entry of entries) {
    const card = lookup(entry.name)
    if (!card) continue
    const identity = card.colorIdentity ?? []
    const key = identity.length === 0 ? 'C' : identity.length > 1 ? 'M' : COLOR_KEY_BY_ENUM[identity[0]!] ?? 'C'
    colors[key] = (colors[key] ?? 0) + entry.count
    if (!card.cardTypes.includes('LAND')) {
      const bucket = Math.min(Math.max(Math.round(card.cmc), 0), curve.length - 1)
      curve[bucket] = (curve[bucket] ?? 0) + entry.count
    }
  }
  return { colors, curve, curveMax: Math.max(1, ...curve) }
}

/** Mono-colour pips plus gold and colourless, each with its count. */
export function ColorSpread({ colors }: { colors: Record<string, number> }) {
  return (
    <>
      {COLOR_GROUPS.map((group) => (
        <span key={group.key} className={styles.colorStat} title={`${group.label}: ${colors[group.key] ?? 0}`}>
          <ManaSymbol symbol={group.key} size={13} />
          {colors[group.key] ?? 0}
        </span>
      ))}
      <span className={styles.colorStat} title={`Multicolour: ${colors.M ?? 0}`}>
        {/* No printed symbol for "multicolour" — gold is the card frame's own convention. */}
        <span className={styles.goldPip} aria-label="Multicolour" />
        {colors.M ?? 0}
      </span>
      <span className={styles.colorStat} title={`Colourless: ${colors.C ?? 0}`}>
        <ManaSymbol symbol="C" size={13} />
        {colors.C ?? 0}
      </span>
    </>
  )
}

export function RaritySpread({ tally }: { tally: RarityTally }) {
  return (
    <>
      {CUBE_RARITIES.map((rarity) => (
        <span key={rarity} className={styles.colorStat} title={`${RARITY_LABEL[rarity]}: ${tally[rarity]}`}>
          <span className={`${styles.rarityChip} ${styles.rarityExplicit} ${RARITY_CLASS[rarity]}`} aria-hidden>
            {RARITY_LETTER[rarity]}
          </span>
          {tally[rarity]}
        </span>
      ))}
    </>
  )
}

/**
 * One bar per slot of a booster style's pack. `short` marks the rarities the cube can't fill a pack
 * with, drawn hollow.
 */
export function PackStrip({
  style,
  short = new Set<string>(),
}: {
  style: CubeBoosterStyle
  short?: ReadonlySet<string>
}) {
  const pack = boosterStyleInfo(style).pack
  if (!pack) return null
  const slots: { kind: string; title: string }[] = [
    ...Array.from({ length: pack.commons }, () => ({ kind: 'common', title: 'Common' })),
    ...Array.from({ length: pack.uncommons }, () => ({ kind: 'uncommon', title: 'Uncommon' })),
    ...Array.from({ length: pack.rares }, () => ({ kind: 'rare', title: 'Rare, or mythic one time in eight' })),
    ...Array.from({ length: pack.wildcards }, () => ({ kind: 'wild', title: 'Wildcard — any rarity' })),
  ]
  const slotClass: Record<string, string> = {
    common: styles.slotCommon!,
    uncommon: styles.slotUncommon!,
    rare: styles.slotRare!,
    wild: styles.slotWild!,
  }
  return (
    <span className={styles.packStrip} role="img" aria-label={`${slots.length}-card pack`}>
      {slots.map((slot, i) => (
        <span
          key={i}
          className={`${styles.packSlot} ${slotClass[slot.kind]} ${short.has(slot.kind) ? styles.slotShort : ''}`}
          title={slot.title}
        />
      ))}
    </span>
  )
}

interface CubeStatsProps {
  readonly entries: readonly SharedCubeEntry[]
  readonly lookup: (name: string) => CardSummary | undefined
  readonly boosterStyle: CubeBoosterStyle
  readonly packSize: number
}

/** The editor's footer: colours, curve, rarity, and whether the cube fills its packs. */
export function CubeStats({ entries, lookup, boosterStyle, packSize }: CubeStatsProps) {
  const { colors, curve, curveMax } = useMemo(() => colorAndCurve(entries, lookup), [entries, lookup])
  const tallies = useMemo(() => rarityTallies(entries, (name) => lookup(name)?.rarity), [entries, lookup])
  const health = packHealth(boosterStyle, tallies.distinct)
  const total = entries.reduce((sum, e) => sum + e.count, 0)

  return (
    <div className={styles.summary}>
      <div className={styles.summaryGroup}>
        <span className={styles.summaryLabel}>Colours</span>
        <ColorSpread colors={colors} />
      </div>
      <div className={styles.summaryGroup}>
        <span className={styles.summaryLabel}>Curve</span>
        <div className={styles.curve}>
          {curve.map((count, cmc) => (
            <div key={cmc} className={styles.curveBar} title={`${count} at mana value ${cmc}`}>
              <div className={styles.curveFill} style={{ height: `${(count / curveMax) * 100}%` }} />
              <span className={styles.curveLabel}>{cmc === curve.length - 1 ? `${cmc}+` : cmc}</span>
            </div>
          ))}
        </div>
      </div>
      <div className={styles.summaryGroup}>
        <span className={styles.summaryLabel}>Rarity</span>
        <RaritySpread tally={tallies.physical} />
      </div>
      <div className={`${styles.summaryGroup} ${styles.summaryEnd}`}>
        {health ? (
          <span className={health.ok ? styles.healthOk : styles.healthBad}>
            {health.ok
              ? 'Every pack fills completely'
              : `Short for a full pack: ${health.lines
                  .filter((l) => !l.ok)
                  .map((l) => `${l.needed - l.have} more ${l.label}`)
                  .join(', ')}`}
          </span>
        ) : total > 0 ? (
          <span className={styles.healthOk}>
            Seats {shuffledSeats(total, packSize)} at 3 packs of {packSize}
          </span>
        ) : null}
      </div>
    </div>
  )
}
