/**
 * The launch panel's "Your deck" field.
 *
 * It used to be a native `<select>` of saved deck names — or, with no saved decks, a sentence saying
 * to go and pick one in the lobby. So a first game of Constructed against the AI went: Play, a
 * lobby full of settings, Choose deck, an empty "My Decks", Examples, a tile that opened a textarea,
 * Done, I'm ready. Now the deck is named here, where everything else about the game already is, and
 * an AI game goes from the tile to the table.
 *
 * A starter deck is shown as what it is — cover art, colours, a one-line pitch — and the chosen one
 * gets a strip of facts underneath (curve, creature/spell/land split, the cards it is built around),
 * from the summary the server attaches to each starter. Picking a deck you have never seen should
 * not be a guess from its name.
 *
 * Your own decks get the same treatment, from the same server-side summary (`useDeckSummaries`): the
 * cover you chose for the deck in the deckbuilder (else its rarest card), its colours, its curve and
 * the note you wrote on it.
 *
 * Collapsed it is one row naming the deck (the panel stays short on a phone); "Change" opens the
 * choices inline, grouped by where the deck comes from: yours, the server's starter decks, a rolled
 * one where the lobby can roll it, or the explicit "choose in the lobby".
 */
import { useId, useState, type ReactNode } from 'react'
import type { UnifiedDeck } from '@/store/useUnifiedDecks'
import { commandersOf, mergeCommanderIntoCards } from '@/store/deckLibrary'
import { useDeckSummaries } from '@/store/useDeckSummaries'
import { starterDeckSize, type StarterDeck, type StarterDeckSummary } from '@/store/useStarterDecks'
import { getCdnArtCropUrl, getScryfallArtCropUrl } from '@/utils/cardImages'
import { ManaSymbol } from './ManaSymbols'
import type { PanelDeck } from '../lobby/playModes'
import styles from './PlayHub.module.css'

export function LaunchDeckChoice({
  value,
  saved,
  starters,
  canRoll,
  onChange,
}: {
  value: PanelDeck
  /** Your decks, newest first, already narrowed to what this mode accepts. */
  saved: readonly UnifiedDeck[]
  /** Starter decks for this mode, or null while they load. */
  starters: readonly StarterDeck[] | null
  canRoll: boolean
  onChange: (deck: PanelDeck) => void
}) {
  const [open, setOpen] = useState(false)
  const listId = useId()
  const glances = useDeckSummaries(saved)
  const current = describe(value, saved, starters)
  const starter = value.kind === 'EXAMPLE' ? starters?.find((d) => d.name === value.name) : undefined
  const savedDeck = value.kind === 'SAVED' ? saved.find((d) => d.name === value.name) : undefined
  // Whichever kind it is, the chosen deck's summary: a starter's own, or the one fetched for yours.
  const summary = starter?.summary ?? (savedDeck ? glances[savedDeck.id] : undefined)

  const choose = (deck: PanelDeck) => {
    onChange(deck)
    setOpen(false)
  }

  return (
    <div className={styles.deckChoice}>
      <div className={styles.deckChoiceCard} data-open={open}>
        <button
          type="button"
          className={styles.deckChoiceButton}
          aria-expanded={open}
          aria-controls={listId}
          data-testid="launch-deck"
          onClick={() => setOpen((o) => !o)}
        >
          <DeckThumb kind={value.kind} summary={summary} />
          <span className={styles.deckChoiceText}>
            <span className={styles.deckChoiceNameRow}>
              <span className={styles.deckChoiceName}>{current.name}</span>
              {summary && <Pips colors={summary.colors} />}
            </span>
            <span className={styles.deckChoiceMeta} data-note={current.isNote || undefined}>{current.meta}</span>
          </span>
          <span className={styles.setChange}>{open ? 'Close' : 'Change'}</span>
        </button>
        {!open && summary && <StarterFacts summary={summary} note={starter?.note ?? undefined} />}
      </div>

      {open && (
        <div id={listId} className={styles.deckList} role="listbox" aria-label="Your deck">
          {saved.length > 0 && (
            <DeckGroup label="Your decks">
              {saved.map((d) => {
                const glance = glances[d.id]
                return (
                  <DeckOption
                    key={d.id}
                    thumb={<DeckThumb kind="SAVED" summary={glance} />}
                    name={d.name}
                    pips={glance?.colors}
                    meta={d.note ?? `${deckSize(d)} cards`}
                    metaIsNote={!!d.note}
                    aside={d.note ? `${deckSize(d)}` : undefined}
                    selected={value.kind === 'SAVED' && value.name === d.name}
                    onSelect={() => choose({ kind: 'SAVED', name: d.name })}
                  />
                )
              })}
            </DeckGroup>
          )}
          <DeckGroup label="Starter decks">
            {starters === null && <p className={styles.deckListNote}>Loading…</p>}
            {starters?.length === 0 && <p className={styles.deckListNote}>None for this format.</p>}
            {starters?.map((d) => {
              const selected = value.kind === 'EXAMPLE' && value.name === d.name
              return (
                <DeckOption
                  key={d.id}
                  thumb={<DeckThumb kind="EXAMPLE" summary={d.summary ?? undefined} />}
                  name={d.name}
                  pips={d.summary?.colors}
                  meta={d.description}
                  aside={`${starterDeckSize(d)}`}
                  selected={selected}
                  onSelect={() => choose({ kind: 'EXAMPLE', name: d.name })}
                />
              )
            })}
          </DeckGroup>
          <DeckGroup label="Or">
            {canRoll && (
              <DeckOption
                thumb={<DeckThumb kind="RANDOM" />}
                name="Random deck"
                meta="The server builds one for you when the game starts"
                selected={value.kind === 'RANDOM'}
                onSelect={() => choose({ kind: 'RANDOM' })}
              />
            )}
            <DeckOption
              thumb={<DeckThumb kind="LOBBY" />}
              name="Choose in the lobby"
              meta="Paste a list, or decide once you're there"
              selected={value.kind === 'LOBBY'}
              onSelect={() => choose({ kind: 'LOBBY' })}
            />
          </DeckGroup>
        </div>
      )}
    </div>
  )
}

/** The collapsed row's two lines for a choice. */
function describe(
  deck: PanelDeck,
  saved: readonly UnifiedDeck[],
  starters: readonly StarterDeck[] | null,
): { name: string; meta: string; isNote?: boolean } {
  switch (deck.kind) {
    case 'SAVED': {
      const match = saved.find((d) => d.name === deck.name)
      if (match?.note) return { name: deck.name, meta: match.note, isNote: true }
      return { name: deck.name, meta: match ? `Your deck · ${deckSize(match)} cards` : 'Your deck' }
    }
    case 'EXAMPLE': {
      const match = starters?.find((d) => d.name === deck.name)
      return { name: deck.name, meta: match?.description ?? 'Starter deck' }
    }
    case 'RANDOM':
      return { name: 'Random deck', meta: 'Built for you when the game starts' }
    case 'LOBBY':
      return { name: 'Choose in the lobby', meta: 'Pick or paste a deck before you ready up' }
  }
}

function deckSize(deck: UnifiedDeck): number {
  // Saved decks keep their commander(s) out of `cards`; a partner pair adds two.
  return Object.values(mergeCommanderIntoCards(deck.cards, ...commandersOf(deck))).reduce((a, b) => a + b, 0)
}

/**
 * The chosen starter deck in numbers: its curve as a sparkline, what it is made of, and the cards
 * it is built around — the three things a player reads off a decklist before deciding to play it.
 */
function StarterFacts({ summary, note }: { summary: StarterDeckSummary; note?: string | undefined }) {
  const peak = Math.max(1, ...summary.curve)
  return (
    <div className={styles.deckFacts} data-testid="launch-deck-facts">
      <div className={styles.deckFactsRow}>
        <div className={styles.curve} aria-label={`Mana curve: ${summary.curve.join(', ')}`} role="img">
          {summary.curve.map((n, i) => (
            <span key={i} className={styles.curveCol} title={`${i === 7 ? '7+' : i} mana: ${n}`}>
              <span className={styles.curveBar} style={{ height: `${Math.max(n > 0 ? 12 : 4, (n / peak) * 100)}%` }} data-empty={n === 0} />
              <span className={styles.curveLabel}>{i === 7 ? '7+' : i}</span>
            </span>
          ))}
        </div>
        <dl className={styles.deckSplit}>
          <div><dt>Creatures</dt><dd>{summary.creatures}</dd></div>
          <div><dt>Spells</dt><dd>{summary.spells}</dd></div>
          <div><dt>Lands</dt><dd>{summary.lands}</dd></div>
        </dl>
      </div>
      {summary.keyCards.length > 0 && (
        <p className={styles.keyCards}>
          <span className={styles.keyCardsLabel}>Built around</span> {summary.keyCards.join(' · ')}
        </p>
      )}
      {note && <p className={styles.deckTip}>{note}</p>}
    </div>
  )
}

function Pips({ colors }: { colors: readonly string[] }) {
  if (colors.length === 0) return null
  return (
    <span className={styles.pips} aria-label={`Colours: ${colors.join('')}`}>
      {colors.map((c) => <ManaSymbol key={c} symbol={c} size={14} />)}
    </span>
  )
}

function DeckGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className={styles.deckGroup} role="group" aria-label={label}>
      <span className={styles.deckGroupLabel}>{label}</span>
      {children}
    </div>
  )
}

function DeckOption({
  thumb,
  name,
  pips,
  meta,
  metaIsNote = false,
  aside,
  selected,
  onSelect,
}: {
  thumb: ReactNode
  name: string
  pips?: readonly string[] | undefined
  meta: string
  /** The meta line is the owner's note on the deck — set in italics. */
  metaIsNote?: boolean
  /** A small right-hand figure — a starter's card count. */
  aside?: string | undefined
  selected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      className={styles.deckOption}
      onClick={onSelect}
    >
      {thumb}
      <span className={styles.deckChoiceText}>
        <span className={styles.deckChoiceNameRow}>
          <span className={styles.deckChoiceName}>{name}</span>
          {pips && <Pips colors={pips} />}
        </span>
        <span className={styles.deckChoiceMeta} data-note={metaIsNote || undefined}>{meta}</span>
      </span>
      {selected ? (
        <svg className={styles.deckOptionCheck} viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
          <path d="M5 12.5l4.5 4.5L19 7" />
        </svg>
      ) : aside ? (
        <span className={styles.deckOptionAside}>{aside}</span>
      ) : null}
    </button>
  )
}

const GLYPHS: Record<PanelDeck['kind'], string> = {
  SAVED: 'M8 4h9a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M4 7v11a3 3 0 0 0 3 3',
  EXAMPLE: 'M12 3l2.4 5 5.6.8-4 3.9.9 5.6-4.9-2.6-4.9 2.6.9-5.6-4-3.9 5.6-.8z',
  RANDOM: 'M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z M9 9h.01 M15 9h.01 M12 12h.01 M9 15h.01 M15 15h.01',
  LOBBY: 'M4 12h12 M12 6l6 6-6 6',
}

/**
 * A deck's cover art, cropped, from its summary; every other kind (and a deck whose summary hasn't
 * arrived yet) gets its glyph on a tinted square.
 */
function DeckThumb({ kind, summary }: { kind: PanelDeck['kind']; summary?: StarterDeckSummary | undefined }) {
  const cover = summary?.coverCard
  const art = cover ? (getCdnArtCropUrl(summary?.coverImageUri) ?? getScryfallArtCropUrl(cover)) : null
  if (art) {
    return (
      <span
        className={styles.deckArt}
        style={{ backgroundImage: `url("${art}")` }}
        title={cover ?? undefined}
        aria-hidden
      />
    )
  }
  return (
    <span className={styles.deckGlyph} data-kind={kind} aria-hidden>
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d={GLYPHS[kind]} />
      </svg>
    </span>
  )
}
