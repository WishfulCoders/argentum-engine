/**
 * One card, up close — the card browser's detail view, opened by clicking a tile (`/cards?card=…`).
 *
 * The art large (both faces side by side for a double-faced card, turned for a split card or a
 * battle), the Oracle text, every printing as a thumbnail strip that swaps the art, the formats it is
 * legal in, and the ways onward: start a deck around it — as its commander when it can be one — or
 * copy a link to it. The type line, set and printing chips are themselves searches, so a card leads
 * to its neighbours. ← / → step through the current results; Esc closes.
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import type { PrintingDTO } from '@/components/deckbuilder/PrintingPicker'
import type { SetInfo } from '@/components/deckbuilder/browser'
import { AbilityText, ManaCost } from '@/components/ui/ManaSymbols'
import { SetIcon } from '@/components/ui/SetIcon'
import { getCardImageUrl, landscapeImageRotateDeg } from '@/utils/cardImages'
import { DECK_FORMATS } from '@/utils/deckLegality'
import styles from './Cards.module.css'

export interface CardDetailProps {
  card: CardSummary
  setInfos: readonly SetInfo[]
  /** The printing an active `s:` filter picked, so the dialog opens on the art the grid showed. */
  preferredPrinting: { setCode: string; collectorNumber: string } | null
  /** 1-based place in the current results, and their count; null when the card isn't in them. */
  position: { index: number; total: number } | null
  onPrev: (() => void) | null
  onNext: (() => void) | null
  onClose: () => void
  /** Replace the browser's query — what the type, set and rarity chips do. */
  onSearch: (query: string) => void
  onBuildDeck: (printing: PrintingDTO | null, asCommander: boolean) => void
}

export function CardDetailDialog({
  card,
  setInfos,
  preferredPrinting,
  position,
  onPrev,
  onNext,
  onClose,
  onSearch,
  onBuildDeck,
}: CardDetailProps) {
  const printings = usePrintings(card.name)
  const [picked, setPicked] = useState<string | null>(null)
  useEffect(() => setPicked(null), [card.name])

  // The printing on show: the one clicked in the strip, else the set filter's, else the catalogue's
  // default printing — so the dialog opens on exactly the art the tile had.
  const shown = useMemo(() => {
    if (!printings || printings.length === 0) return null
    const key = (p: { setCode: string; collectorNumber: string }) => `${p.setCode.toUpperCase()}:${p.collectorNumber}`
    const want =
      picked ??
      (preferredPrinting ? key(preferredPrinting) : null) ??
      (card.setCode && card.collectorNumber ? key({ setCode: card.setCode, collectorNumber: card.collectorNumber }) : null)
    return printings.find((p) => key(p) === want) ?? null
  }, [printings, picked, preferredPrinting, card.setCode, card.collectorNumber])

  const front = shown?.imageUri ?? card.imageUri ?? null
  const back = card.isDoubleFaced ? (shown?.backFaceImageUri ?? card.backFaceImageUri ?? null) : null
  const landscape = landscapeImageRotateDeg(card) === 90

  // Esc closes; ← / → step through the results. Ignored while typing (there is no input here today,
  // but the browser's search box sits behind the dialog).
  const dialogRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    dialogRef.current?.focus()
  }, [])
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
      if (e.key === 'Escape') onClose()
      else if (e.key === 'ArrowLeft' && onPrev) onPrev()
      else if (e.key === 'ArrowRight' && onNext) onNext()
      else return
      e.preventDefault()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, onPrev, onNext])

  const [copied, setCopied] = useState(false)
  const copyLink = () => {
    const url = `${window.location.origin}/cards?card=${encodeURIComponent(card.name)}`
    void navigator.clipboard?.writeText(url).then(() => {
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1800)
    })
  }

  const setName = (code: string) => setInfos.find((s) => s.code.toUpperCase() === code.toUpperCase())?.name ?? code
  const legal = new Set(card.legalFormats ?? [])
  const sets = uniqueSets(card)

  // Portalled to the body: inside the page's `main` it would sit under the shell's top bar.
  return createPortal(
    <div className={styles.backdrop} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div
        ref={dialogRef}
        className={styles.dialog}
        data-faces={back ? 2 : 1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="card-detail-name"
        tabIndex={-1}
      >
        <header className={styles.dialogBar}>
          <div className={styles.stepper}>
            <button type="button" className={styles.iconButton} onClick={onPrev ?? undefined} disabled={!onPrev} aria-label="Previous card" title="Previous card (←)">
              <Chevron dir="left" />
            </button>
            <button type="button" className={styles.iconButton} onClick={onNext ?? undefined} disabled={!onNext} aria-label="Next card" title="Next card (→)">
              <Chevron dir="right" />
            </button>
            {position && (
              <span className={styles.stepperCount}>
                {position.index.toLocaleString()} of {position.total.toLocaleString()}
              </span>
            )}
          </div>
          <button type="button" className={styles.iconButton} onClick={onClose} aria-label="Close" title="Close (Esc)">
            <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
              <path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </button>
        </header>

        <div className={styles.dialogBody}>
          <div className={styles.art} data-landscape={landscape}>
            <CardImage name={card.name} src={front} landscape={landscape} />
            {back && <CardImage name={card.backFaceName ?? `${card.name} (back)`} src={back} landscape={false} />}
            {shown && (
              <p className={styles.artCredit}>
                {shown.artist && <>Illustrated by {shown.artist} · </>}
                {shown.setName ?? setName(shown.setCode)} #{shown.collectorNumber}
              </p>
            )}
          </div>

          <div className={styles.info}>
            <div className={styles.titleRow}>
              <h2 id="card-detail-name" className={styles.cardName}>
                {card.name}
                {card.backFaceName && <span className={styles.backName}> // {card.backFaceName}</span>}
              </h2>
              {card.manaCost && <ManaCost cost={card.manaCost} size={18} gap={2} />}
            </div>

            <TypeLine card={card} onSearch={onSearch} />

            {card.oracleText && <OracleText text={card.oracleText} />}

            {(card.power != null || card.toughness != null) && (
              <div className={styles.pt}>
                {printedStat(card.power)}/{printedStat(card.toughness)}
              </div>
            )}

            <div className={styles.actions}>
              {card.canBeCommander && (
                <button type="button" className={styles.primaryAction} onClick={() => onBuildDeck(shown, true)}>
                  <CrownIcon /> Build a Commander deck
                </button>
              )}
              <button
                type="button"
                className={card.canBeCommander ? styles.secondaryAction : styles.primaryAction}
                onClick={() => onBuildDeck(shown, false)}
              >
                <PlusIcon /> Start a deck with it
              </button>
              <button type="button" className={styles.ghostAction} onClick={copyLink}>
                <LinkIcon /> {copied ? 'Link copied' : 'Copy link'}
              </button>
            </div>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>Formats</h3>
              <ul className={styles.formats}>
                {DECK_FORMATS.map((f) => {
                  const ok = legal.has(f.value.toUpperCase())
                  return (
                    <li key={f.value} className={styles.format} data-legal={ok}>
                      <span aria-hidden>{ok ? '✓' : '–'}</span> {f.label}
                      <span className={styles.srOnly}>{ok ? ' legal' : ' not legal'}</span>
                    </li>
                  )
                })}
              </ul>
            </section>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>
                {printings && printings.length > 1 ? `${printings.length} printings` : 'Set'}
                <span className={styles.sectionHint}>
                  {printings && printings.length > 1 ? 'Pick one to see its art' : null}
                </span>
              </h3>
              {printings && printings.length > 1 && (
                <div className={styles.printings} role="listbox" aria-label="Printings">
                  {printings.map((p) => {
                    const key = `${p.setCode.toUpperCase()}:${p.collectorNumber}`
                    const selected = shown != null && `${shown.setCode.toUpperCase()}:${shown.collectorNumber}` === key
                    return (
                      <button
                        key={key}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={styles.printing}
                        onClick={() => setPicked(key)}
                        title={`${p.setName ?? setName(p.setCode)} #${p.collectorNumber}${p.artist ? ` — ${p.artist}` : ''}`}
                      >
                        <img src={getCardImageUrl(card.name, p.imageUri, 'small')} alt="" loading="lazy" />
                        <span className={styles.printingSet}>
                          <SetIcon code={p.setCode} /> {p.setCode.toUpperCase()}
                        </span>
                      </button>
                    )
                  })}
                </div>
              )}
              <div className={styles.chips}>
                {sets.map((code) => (
                  <button key={code} type="button" className={styles.chip} onClick={() => onSearch(`s:${code.toLowerCase()}`)} title={`Every playable card in ${setName(code)}`}>
                    <SetIcon code={code} /> {setName(code)}
                  </button>
                ))}
                {card.rarity && (
                  <button type="button" className={styles.chip} data-rarity={card.rarity.toLowerCase()} onClick={() => onSearch(`r:${card.rarity.toLowerCase()}`)}>
                    {titleCase(card.rarity)}
                  </button>
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  )
}

function CardImage({ name, src, landscape }: { name: string; src: string | null; landscape: boolean }) {
  return (
    <div className={styles.cardFrame} data-landscape={landscape}>
      <img src={getCardImageUrl(name, src, 'large')} alt={name} className={styles.cardImg} />
    </div>
  )
}

/** The type line, each type a search: click "Goblin" to see every playable Goblin. */
function TypeLine({ card, onSearch }: { card: CardSummary; onSearch: (q: string) => void }) {
  const left = [...card.supertypes, ...card.cardTypes]
  const term = (t: string) => (/\s/.test(t) ? `t:"${t.toLowerCase()}"` : `t:${t.toLowerCase()}`)
  const chip = (t: string) => (
    <button key={t} type="button" className={styles.typeLink} onClick={() => onSearch(term(t))} title={`Search t:${t.toLowerCase()}`}>
      {titleCase(t)}
    </button>
  )
  return (
    <p className={styles.typeLine}>
      {left.map(chip)}
      {card.subtypes.length > 0 && <span className={styles.typeDash}>—</span>}
      {card.subtypes.map(chip)}
    </p>
  )
}

/** Oracle text, a paragraph per line, symbols drawn and reminder text set in italics. */
function OracleText({ text }: { text: string }) {
  return (
    <div className={styles.oracle}>
      {text.split('\n').map((line, i) => (
        <p key={i}>
          {line.split(/(\([^)]*\))/g).filter(Boolean).map((part, j) =>
            part.startsWith('(') ? (
              <em key={j} className={styles.reminder}>
                <AbilityText text={part} size={13} />
              </em>
            ) : (
              <AbilityText key={j} text={part} size={14} />
            ),
          )}
        </p>
      ))}
    </div>
  )
}

/** Every printing of a card, newest first — fetched per card; null while loading or on failure. */
function usePrintings(name: string): PrintingDTO[] | null {
  const [state, setState] = useState<{ name: string; list: PrintingDTO[] } | null>(null)
  useEffect(() => {
    let cancelled = false
    fetch(`/api/cards/${encodeURIComponent(name)}/printings`)
      .then((r) => (r.ok ? (r.json() as Promise<PrintingDTO[]>) : []))
      .then((list) => !cancelled && setState({ name, list }))
      .catch(() => !cancelled && setState({ name, list: [] }))
    return () => {
      cancelled = true
    }
  }, [name])
  return state?.name === name ? state.list : null
}

/**
 * A power or toughness as printed. The catalogue sends the SDK value's own string — `Fixed(value=3)`,
 * `Dynamic(…)` for a characteristic-defining `*`, `DynamicWithOffset(…, offset=2)` for `2+*`.
 */
export function printedStat(raw: string | null | undefined): string {
  if (raw == null || raw === '') return '—'
  const fixed = /^Fixed\(value=(-?\d+)\)$/.exec(raw)
  if (fixed) return fixed[1]!
  if (raw.startsWith('DynamicWithOffset(')) {
    const offset = /offset=(-?\d+)\)$/.exec(raw)?.[1]
    return offset && offset !== '0' ? `${offset}+*` : '*'
  }
  if (raw.startsWith('Dynamic(')) return '*'
  return raw
}

function uniqueSets(card: CardSummary): string[] {
  const codes = [card.setCode, ...(card.printingSetCodes ?? [])].filter((c): c is string => !!c)
  return Array.from(new Set(codes.map((c) => c.toUpperCase())))
}

function titleCase(s: string): string {
  return s
    .split(/([\s-])/)
    .map((w) => (w.length > 0 && /[a-z]/i.test(w) ? w[0]!.toUpperCase() + w.slice(1).toLowerCase() : w))
    .join('')
}

function Chevron({ dir }: { dir: 'left' | 'right' }) {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
      <path d={dir === 'left' ? 'M9 2.5L4.5 7 9 11.5' : 'M5 2.5L9.5 7 5 11.5'} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function PlusIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
      <path d="M7 2v10M2 7h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function CrownIcon() {
  return (
    <svg width="16" height="14" viewBox="0 0 16 14" aria-hidden focusable="false">
      <path d="M1.5 4l3.5 3 3-5 3 5 3.5-3-1.5 8h-10z" fill="currentColor" />
    </svg>
  )
}

function LinkIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
      <path d="M6 8a2.5 2.5 0 003.5 0l2-2A2.5 2.5 0 008 2.5l-1 1M8 6a2.5 2.5 0 00-3.5 0l-2 2A2.5 2.5 0 006 11.5l1-1" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}
