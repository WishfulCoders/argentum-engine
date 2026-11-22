/**
 * The rarity a cube card is printed at, as the one-letter chip players already read rarity in, in
 * the set-symbol colours (common grey, uncommon silver, rare gold, mythic orange-red).
 *
 * A filled chip is a rarity the curator set; an outlined one is the card's own catalog rarity — the
 * difference matters because only the first travels with the cube. Clicking opens a four-way picker
 * with a reset back to the card's own rarity.
 */
import { useEffect, useRef, useState } from 'react'
import type { CubeRarity } from '@/api/account'
import { CUBE_RARITIES, RARITY_LABEL, RARITY_LETTER, type ResolvedRarity } from './cubeModel'
import styles from './CubeEditor.module.css'

export const RARITY_CLASS: Record<CubeRarity, string> = {
  COMMON: styles.rarityCommon!,
  UNCOMMON: styles.rarityUncommon!,
  RARE: styles.rarityRare!,
  MYTHIC: styles.rarityMythic!,
}

interface RarityChipProps {
  readonly cardName: string
  readonly resolved: ResolvedRarity
  /** Omit for a read-only chip. Null resets to the card's own rarity. */
  readonly onChange?: (rarity: CubeRarity | null) => void
  /** Draw it quieter — the cube is a shuffled one, where rarity decides nothing. */
  readonly muted?: boolean
}

export function RarityChip({ cardName, resolved, onChange, muted = false }: RarityChipProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const escape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', escape)
    }
  }, [open])

  const { rarity, explicit } = resolved
  const title = `${RARITY_LABEL[rarity]}${explicit ? ' (set for this cube)' : ' (card’s own rarity)'}`
  const chipClass = [
    styles.rarityChip,
    RARITY_CLASS[rarity],
    explicit ? styles.rarityExplicit : styles.rarityImplicit,
    muted ? styles.rarityMuted : '',
  ].join(' ')

  if (!onChange) {
    return (
      <span className={chipClass} title={title} aria-label={title}>
        {RARITY_LETTER[rarity]}
      </span>
    )
  }

  return (
    <span ref={rootRef} className={styles.rarityAnchor}>
      <button
        type="button"
        className={chipClass}
        title={`${title} — change`}
        aria-label={`${cardName}: ${title}. Change rarity`}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        {RARITY_LETTER[rarity]}
      </button>
      {open && (
        <span className={styles.rarityMenu} role="menu" aria-label={`Rarity of ${cardName}`}>
          {CUBE_RARITIES.map((option) => (
            <button
              key={option}
              type="button"
              role="menuitemradio"
              aria-checked={explicit && option === rarity}
              className={`${styles.rarityChip} ${RARITY_CLASS[option]} ${styles.rarityExplicit}`}
              title={RARITY_LABEL[option]}
              onClick={() => {
                onChange(option)
                setOpen(false)
              }}
            >
              {RARITY_LETTER[option]}
            </button>
          ))}
          {explicit && (
            <button
              type="button"
              role="menuitem"
              className={styles.rarityReset}
              onClick={() => {
                onChange(null)
                setOpen(false)
              }}
            >
              Card’s own
            </button>
          )}
        </span>
      )}
    </span>
  )
}
