import type { SealedCardInfo } from '@/types'
import { ManaSymbol } from '../ui/ManaSymbols'
import type { ColorCode } from './jumpstartPack'
import styles from './JumpstartOverlay.module.css'

export function ColorPips({ colors, size = 18 }: { colors: readonly ColorCode[]; size?: number }) {
  if (colors.length === 0) return <span className={styles.pips}><ManaSymbol symbol="C" size={size} /></span>
  return <span className={styles.pips} aria-label={`Colors: ${colors.join('')}`}>
    {colors.map((c) => <ManaSymbol key={c} symbol={c} size={size} />)}
  </span>
}

const CURVE_LABELS = ['1', '2', '3', '4', '5', '6+']

/**
 * Nonland mana curve. With [base], the bars stack the chosen pack (teal, below) under the
 * candidate (gold, above), so a second pick can be judged by the deck it makes.
 */
export function CurveBars({ curve, base, size = 'small' }: {
  curve: readonly number[]
  base?: readonly number[] | undefined
  size?: 'small' | 'large'
}) {
  const totals = curve.map((n, i) => n + (base?.[i] ?? 0))
  const max = Math.max(4, ...totals)
  return <div className={size === 'large' ? styles.curveLarge : styles.curve} role="img"
    aria-label={`Mana curve: ${totals.map((n, i) => `${n} at ${CURVE_LABELS[i]}`).join(', ')}`}>
    {curve.map((n, i) => <div key={i} className={styles.curveCol}>
      {size === 'large' && <span className={styles.curveCount}>{totals[i] || ''}</span>}
      <div className={styles.curveTrack}>
        <div className={styles.curveTop} style={{ height: `${(n / max) * 100}%` }} />
        {base && <div className={styles.curveBase} style={{ height: `${((base[i] ?? 0) / max) * 100}%` }} />}
      </div>
      <span className={styles.curveLabel}>{CURVE_LABELS[i]}</span>
    </div>)}
  </div>
}

const RARITY_CLASS: Record<string, string | undefined> = {
  MYTHIC: styles.mythic, RARE: styles.rare, UNCOMMON: styles.uncommon,
}

export function RarityDot({ card }: { card: SealedCardInfo }) {
  return <span className={`${styles.rarityDot} ${RARITY_CLASS[card.rarity] ?? ''}`} aria-hidden />
}
