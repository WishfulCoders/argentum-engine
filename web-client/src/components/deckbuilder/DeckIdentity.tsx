/**
 * The deck's identity, at the top of the deck pane: its cover, its name, and the note its owner
 * keeps on it — the three things every deck picker shows about it.
 *
 * The cover is a button. It opens {@link CoverPicker}, a gallery of the deck's own cards as art
 * crops, so "which picture is this deck" is chosen by looking at pictures. "Automatic" keeps the
 * gallery's default (the rarest card); a chosen card that later leaves the deck falls back to it
 * too, so a cover never shows something you no longer play.
 */
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { DECK_NOTE_MAX_LENGTH } from '@/store/deckLibrary'
import { heroArtUrl, rarestCard } from '@/components/deck/DeckTile'
import type { CardSummary } from './cardFilter'
import styles from './DeckIdentity.module.css'

export function DeckIdentity({
  name,
  onNameChange,
  note,
  onNoteChange,
  cover,
  coverChosen,
  onOpenCoverPicker,
  layout,
  children,
}: {
  name: string
  onNameChange: (name: string) => void
  note: string
  onNoteChange: (note: string) => void
  /** The card painting the cover right now (chosen, else automatic), with its effective art. */
  cover: CardSummary | null
  /** True when the owner picked the cover rather than leaving it automatic. */
  coverChosen: boolean
  onOpenCoverPicker: () => void
  /** `stacked` for the narrow right rail; `inline` for the wide deck view. */
  layout: 'stacked' | 'inline'
  /** Trailing controls — the format picker. */
  children?: ReactNode
}) {
  const art = heroArtUrl(cover)
  return (
    <div className={styles.identity} data-layout={layout}>
      <button
        type="button"
        className={styles.cover}
        onClick={onOpenCoverPicker}
        title={cover ? `Cover: ${cover.name}${coverChosen ? '' : ' (automatic)'} — click to change` : 'Choose a cover card'}
        aria-label="Choose the deck's cover card"
        data-testid="deck-cover-button"
      >
        {art ? (
          <img className={styles.coverArt} src={art} alt="" draggable={false} />
        ) : (
          <span className={styles.coverEmpty} aria-hidden>
            <ImageIcon />
          </span>
        )}
        <span className={styles.coverHint} aria-hidden>
          <ImageIcon size={13} />
          {art ? 'Change' : 'Cover'}
        </span>
      </button>
      <div className={styles.fields}>
        <input
          className={styles.name}
          value={name}
          onChange={(e) => onNameChange(e.target.value)}
          placeholder="Deck name"
          aria-label="Deck name"
          spellCheck={false}
        />
        <input
          className={styles.note}
          value={note}
          onChange={(e) => onNoteChange(e.target.value.slice(0, DECK_NOTE_MAX_LENGTH))}
          placeholder="Add a note — the plan, a matchup, a reminder…"
          aria-label="Deck note"
          maxLength={DECK_NOTE_MAX_LENGTH}
          data-testid="deck-note-input"
        />
      </div>
      {children && <div className={styles.trailing}>{children}</div>}
    </div>
  )
}

interface CoverCandidate {
  card: CardSummary
  /** Art for this card in this deck — its pinned printing's, else the catalogue default. */
  art: string
  isCommander: boolean
  isLand: boolean
}

const RARITY_RANK: Record<string, number> = { MYTHIC: 3, RARE: 2, UNCOMMON: 1, COMMON: 0 }

/**
 * A gallery of the deck's cards as art crops to choose the cover from. Commander(s) first, then
 * spells from the rarest and biggest down, lands last — the order you'd look for a deck's face in.
 */
export function CoverPicker({
  deckCards,
  commanders,
  catalog,
  artFor,
  chosen,
  onChoose,
  onClose,
}: {
  deckCards: Record<string, number>
  /** The designated commander(s) — two for a partner pair. */
  commanders: readonly string[]
  catalog: Record<string, CardSummary>
  /** The image URL for a card as it appears in this deck (pinned printing aware). */
  artFor: (name: string) => string | null
  /** The chosen cover card, or null for automatic. */
  chosen: string | null
  /** Null = back to automatic. */
  onChoose: (name: string | null) => void
  onClose: () => void
}) {
  const [filter, setFilter] = useState('')

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const candidates = useMemo(() => {
    const out: CoverCandidate[] = []
    const names = new Set(Object.keys(deckCards).filter((n) => (deckCards[n] ?? 0) > 0))
    for (const c of commanders) names.add(c)
    for (const name of names) {
      const card = catalog[name]
      if (!card || card.basicLand) continue
      const art = heroArtUrl({ ...card, imageUri: artFor(name) ?? card.imageUri ?? null })
      if (!art) continue
      out.push({
        card,
        art,
        isCommander: commanders.includes(name),
        isLand: card.cardTypes.some((t) => t.toUpperCase() === 'LAND'),
      })
    }
    const rank = (c: CardSummary) => RARITY_RANK[(c.rarity ?? 'COMMON').toUpperCase()] ?? 0
    return out.sort(
      (a, b) =>
        Number(b.isCommander) - Number(a.isCommander) ||
        Number(a.isLand) - Number(b.isLand) ||
        rank(b.card) - rank(a.card) ||
        b.card.cmc - a.card.cmc ||
        a.card.name.localeCompare(b.card.name),
    )
  }, [deckCards, commanders, catalog, artFor])

  const automatic = useMemo(() => {
    const full = { ...deckCards }
    for (const c of commanders) if (!full[c]) full[c] = 1
    return rarestCard(full, catalog, commanders[0] ?? null)
  }, [deckCards, commanders, catalog])
  const automaticArt = automatic
    ? heroArtUrl({ ...automatic, imageUri: artFor(automatic.name) ?? automatic.imageUri ?? null })
    : null

  const shown = useMemo(() => {
    const f = filter.trim().toLowerCase()
    return f ? candidates.filter((c) => c.card.name.toLowerCase().includes(f)) : candidates
  }, [candidates, filter])

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="cover-picker-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className={styles.dialogHeader}>
          <div>
            <h2 id="cover-picker-title" className={styles.dialogTitle}>Choose a cover</h2>
            <p className={styles.dialogSubtitle}>The art that stands for this deck wherever you pick it.</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>
        {candidates.length > 12 && (
          <input
            className={styles.search}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Find a card…"
            aria-label="Filter cards"
            autoFocus
          />
        )}
        <div className={styles.gridScroll}>
          {candidates.length === 0 ? (
            <p className={styles.empty}>Add some cards to the deck to pick one of them as its cover.</p>
          ) : (
            <div className={styles.grid} role="listbox" aria-label="Cover card">
              {!filter && (
                <CoverOption
                  art={automaticArt}
                  label="Automatic"
                  badge="Auto"
                  sublabel={automatic ? `Rarest card · ${automatic.name}` : 'Rarest card'}
                  selected={chosen === null}
                  onSelect={() => onChoose(null)}
                />
              )}
              {shown.map((c) => (
                <CoverOption
                  key={c.card.name}
                  art={c.art}
                  label={c.card.name}
                  sublabel={c.isCommander ? 'Commander' : undefined}
                  selected={chosen === c.card.name}
                  onSelect={() => onChoose(c.card.name)}
                />
              ))}
              {shown.length === 0 && <p className={styles.empty}>No card in the deck matches “{filter}”.</p>}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function CoverOption({
  art,
  label,
  sublabel,
  badge,
  selected,
  onSelect,
}: {
  art: string | null
  label: string
  /** A corner chip over the art — marks the automatic choice apart from the card it lands on. */
  badge?: string | undefined
  sublabel?: string | undefined
  selected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      className={styles.option}
      onClick={onSelect}
      title={label}
    >
      <span className={styles.optionArt}>
        {art && <img src={art} alt="" loading="lazy" draggable={false} />}
        {badge && <span className={styles.optionBadge}>{badge}</span>}
        {selected && (
          <span className={styles.optionCheck} aria-hidden>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 12.5l4.5 4.5L19 7" />
            </svg>
          </span>
        )}
      </span>
      <span className={styles.optionLabel}>{label}</span>
      {sublabel && <span className={styles.optionSublabel}>{sublabel}</span>}
    </button>
  )
}

function ImageIcon({ size = 20 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <rect x="3" y="4" width="18" height="16" rx="2.5" />
      <circle cx="9" cy="10" r="1.8" />
      <path d="M21 16l-5.2-5.2a1.2 1.2 0 0 0-1.7 0L5 20" />
    </svg>
  )
}
