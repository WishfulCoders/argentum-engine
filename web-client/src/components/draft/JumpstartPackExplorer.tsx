import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { JumpstartOffer, SealedCardInfo } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages'
import { useHasHover } from '@/hooks/useHasHover'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import { useDfcHoverFlip } from '../ui/useDfcHoverFlip'
import { MobileCardPreview } from './MobileDraftPool'
import { ColorPips, CurveBars } from './JumpstartPackParts'
import { colorsLabel, mergeColors, summarizePack } from './jumpstartPack'
import styles from './JumpstartOverlay.module.css'

export interface PackExplorerProps {
  /** The packs the arrows step through — the current offers, or the two chosen packs. */
  readonly packs: readonly JumpstartOffer[]
  readonly index: number
  readonly onIndex: (index: number) => void
  /** The pack already chosen; enables the "with your first pack" view. */
  readonly base?: JumpstartOffer | undefined
  /** Absent once the deck is complete — the explorer is then read-only. */
  readonly onChoose?: ((offer: JumpstartOffer) => void) | undefined
  readonly chooseDisabled?: boolean
  readonly onClose: () => void
}

/**
 * A visual spoiler for one Jumpstart pack: every card face, grouped Creatures / Spells / Lands and
 * sorted by curve. On pick two it can lay the candidate over the pack already chosen — the
 * 40-card deck you'd actually play — which is the comparison Arena makes by showing both packets.
 */
export function JumpstartPackExplorer({ packs, index, onIndex, base, onChoose, chooseDisabled, onClose }: PackExplorerProps) {
  const pack = packs[index]!
  const [combined, setCombined] = useState(base !== undefined)
  const [hovered, setHovered] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [tapped, setTapped] = useState<SealedCardInfo | null>(null)
  const hasHover = useHasHover()
  const dfc = useDfcHoverFlip(hovered)
  const dialogRef = useRef<HTMLDivElement>(null)
  const showCombined = combined && base !== undefined && base.id !== pack.id

  const cards = useMemo(() => showCombined ? [...base!.cards, ...pack.cards] : pack.cards, [showCombined, base, pack])
  const summary = useMemo(() => summarizePack(cards), [cards])
  const packSummary = useMemo(() => summarizePack(pack.cards), [pack])
  const baseSummary = useMemo(() => base ? summarizePack(base.cards) : undefined, [base])
  const baseNames = useMemo(() => new Set(base?.cards.map((c: SealedCardInfo) => c.name)), [base])
  const packNames = useMemo(() => new Set(pack.cards.map((c: SealedCardInfo) => c.name)), [pack])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { e.preventDefault(); onClose() }
      if (packs.length > 1 && e.key === 'ArrowRight') onIndex((index + 1) % packs.length)
      if (packs.length > 1 && e.key === 'ArrowLeft') onIndex((index + packs.length - 1) % packs.length)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [index, packs.length, onIndex, onClose])

  useEffect(() => { dialogRef.current?.focus() }, [])
  useEffect(() => { dialogRef.current?.querySelector(`.${styles.explorerBody}`)?.scrollTo({ top: 0 }) }, [index, showCombined])

  const origin = (name: string): 'base' | 'pack' | 'both' | null => {
    if (!showCombined) return null
    const inBase = baseNames.has(name), inPack = packNames.has(name)
    return inBase && inPack ? 'both' : inBase ? 'base' : 'pack'
  }
  const colors = showCombined ? mergeColors(baseSummary!.colors, packSummary.colors) : packSummary.colors

  return createPortal(
    <div className={styles.explorerBackdrop} onClick={onClose}>
      <div ref={dialogRef} tabIndex={-1} role="dialog" aria-modal="true" aria-label={`${pack.theme} pack`}
        className={styles.explorer} onClick={(e) => e.stopPropagation()}>
        <header className={styles.explorerHeader}>
          <div className={styles.explorerTitle}>
            <span className={styles.eyebrow}>{showCombined ? 'YOUR DECK WITH' : packs.length > 1 ? `PACK ${index + 1} OF ${packs.length}` : 'PACK'}</span>
            <h2>{showCombined ? <><span className={styles.baseText}>{base!.theme}</span> + <span className={styles.packText}>{pack.theme}</span></> : pack.theme}</h2>
            <div className={styles.explorerMeta}>
              <ColorPips colors={colors} />
              <span>{colorsLabel(colors)}</span>
              <span>·</span>
              <span>{cards.length} cards</span>
              <span>·</span>
              <span>{summary.creatures} creatures, {summary.spells} spells, {summary.lands} lands</span>
            </div>
          </div>
          <CurveBars size="large" curve={packSummary.curve} base={showCombined ? baseSummary!.curve : undefined} />
          <button className={styles.close} onClick={onClose} aria-label="Close pack">✕</button>
        </header>
        {base && base.id !== pack.id && <div className={styles.viewToggle} role="group" aria-label="Show">
          <button aria-pressed={!showCombined} onClick={() => setCombined(false)}>This pack · 20</button>
          <button aria-pressed={showCombined} onClick={() => setCombined(true)}>With {base.theme} · 40</button>
          {showCombined && <span className={styles.legend}>
            <span className={styles.legendBase}>{base.theme}</span>
            <span className={styles.legendPack}>{pack.theme}</span>
          </span>}
        </div>}
        <div className={styles.explorerBody}>
          {summary.sections.map((section) => <section key={section.key} className={styles.explorerSection}>
            <h3>{section.label} <span>{section.total}</span></h3>
            <ul className={styles.cardGrid}>
              {section.entries.map(({ card, count }) => {
                const from = origin(card.name)
                return <li key={card.name} className={`${styles.cardTile} ${from ? styles[`from_${from}`] : ''}`}>
                  <button
                    aria-label={`${card.name}${count > 1 ? `, ${count} copies` : ''}`}
                    onMouseEnter={(e) => { setHovered(card); setHoverPos({ x: e.clientX, y: e.clientY }) }}
                    onMouseMove={(e) => setHoverPos({ x: e.clientX, y: e.clientY })}
                    onMouseLeave={() => { setHovered(null); dfc.resetFlip() }}
                    onClick={() => { if (!hasHover) setTapped(card) }}
                  >
                    <img src={getCardImageUrl(card.name, card.imageUri, 'normal')} alt="" loading="lazy" />
                    {count > 1 && <span className={styles.copies}>×{count}</span>}
                  </button>
                </li>
              })}
            </ul>
          </section>)}
        </div>
        <footer className={styles.explorerFooter}>
          {packs.length > 1 ? <div className={styles.pager}>
            <button onClick={() => onIndex((index + packs.length - 1) % packs.length)} aria-label="Previous pack">←</button>
            <span>{packs.map((p, i) => <button key={p.id} aria-label={p.theme} aria-current={i === index}
              className={i === index ? styles.dotActive : styles.dot} onClick={() => onIndex(i)} />)}</span>
            <button onClick={() => onIndex((index + 1) % packs.length)} aria-label="Next pack">→</button>
          </div> : <span />}
          {onChoose && <button className={styles.choose} disabled={chooseDisabled} onClick={() => onChoose(pack)}>
            Choose {pack.theme}
          </button>}
        </footer>
      </div>
      {hovered && hasHover && <HoverCardPreview
        name={dfc.displayName ?? hovered.name}
        imageUri={dfc.displayImageUri ?? hovered.imageUri}
        pos={hoverPos}
        rulings={hovered.rulings}
        hint={dfc.hint}
        imageRotateDeg={dfc.imageRotateDeg(hovered)}
      />}
      {tapped && <div onClick={(e) => e.stopPropagation()}><MobileCardPreview card={tapped} onClose={() => setTapped(null)} /></div>}
    </div>,
    document.body,
  )
}
