/**
 * Menu-style filter panel over the catalogue query: set picker, colour chips (with an
 * includes / exactly / at-most mode), type + subtype + rarity + keyword chips, mana-value chips
 * and power / toughness ranges.
 *
 * Every control round-trips through the raw query string (via the `cardFilter` chip helpers),
 * so the panel and the free-text search bar always agree. The helpers work on the query's
 * top-level terms as the shared parser reads them, so the panel stays usable whatever was typed:
 * with `or` at the top, a chip brackets the query and narrows all of it.
 *
 * Shared between the deckbuilder page (left rail), the card browser, the cube builder and the
 * scenario builder's card browser.
 */
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { SetIcon } from '@/components/ui/SetIcon'
import {
  addToken,
  appendTerm,
  hasToken,
  queryTerms,
  removeTerms,
  removeToken,
  toggleToken,
  type CardSummary,
} from '../cardFilter'
import styles from '../deckbuilder.module.css'
import type { SetInfo } from './useCardCatalog'

export const COLOR_TOKENS: Array<{ letter: string; label: string; key: string }> = [
  { letter: 'w', label: 'W', key: 'WHITE' },
  { letter: 'u', label: 'U', key: 'BLUE' },
  { letter: 'b', label: 'B', key: 'BLACK' },
  { letter: 'r', label: 'R', key: 'RED' },
  { letter: 'g', label: 'G', key: 'GREEN' },
]

const TYPE_TOKENS = [
  'Creature',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Planeswalker',
  'Land',
]

const RARITY_TOKENS: Array<{ value: string; dot: string }> = [
  { value: 'common', dot: '#9a9a9a' },
  { value: 'uncommon', dot: '#b8c7d1' },
  { value: 'rare', dot: '#d9b450' },
  { value: 'mythic', dot: '#e8662e' },
]

/** Mana-value chips; the last one is "7 or more". */
const MANA_VALUES = [0, 1, 2, 3, 4, 5, 6, 7]
const MV_TOP = 7

const COLOR_MODE_HINT: Record<ColorOp, string> = {
  ':': 'Cards with every colour you pick — and maybe others.',
  '=': 'Cards with exactly the colours you pick.',
  '<=': 'Cards you could play using only the colours you pick.',
}

// Evergreen keywords most commonly used as filters. Surface as chips; the rest
// remain reachable via free-text `kw:` / `is:` tokens.
const KEYWORD_TOKENS = [
  'Flying',
  'Trample',
  'Vigilance',
  'Lifelink',
  'Deathtouch',
  'Haste',
  'First strike',
  'Double strike',
  'Reach',
  'Menace',
  'Hexproof',
  'Defender',
  'Indestructible',
  'Flash',
  'Ward',
]

export function FilterSection({
  query,
  onQueryChange,
  catalog,
  setInfos,
  advanced,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
  setInfos: SetInfo[]
  /** `true` when the query uses or / brackets; the panel says its chips narrow all of it. */
  advanced: boolean
}) {
  const toggle = (token: string) => onQueryChange(toggleToken(query, token))
  const terms = useMemo(() => queryTerms(query), [query])

  // Numeric range filters parse current values out of the query so the boxes
  // round-trip with whatever the user typed.
  const [cmcMin, cmcMax] = useMemo(() => extractRange(query, 'cmc'), [query])
  const [powMin, powMax] = useMemo(() => extractRange(query, 'pow'), [query])
  const [touMin, touMax] = useMemo(() => extractRange(query, 'tou'), [query])

  // Sets the picker can offer: every set that has at least one card in the loaded
  // catalogue. `/api/sets` may include sets the backend hasn't implemented yet, and the
  // catalogue may include codes `/api/sets` doesn't know about — keep the intersection plus
  // a defensive fallback so neither source can silently swallow the other.
  // We count both a card's canonical `setCode` and every `printingSetCodes` reprint, so
  // reprint-only sets (e.g. 8ED, whose cards are all reprints of earlier definitions) still
  // appear — matching the `s:` matcher, which already filters on reprint set codes.
  const availableSets = useMemo(() => {
    const codes = new Set<string>()
    for (const c of catalog) {
      if (c.setCode) codes.add(c.setCode)
      if (c.printingSetCodes) for (const code of c.printingSetCodes) codes.add(code)
    }
    const present = setInfos.filter((s) => codes.has(s.code))
    const known = new Set(present.map((s) => s.code))
    for (const code of codes) {
      if (!known.has(code)) present.push({ code, name: code, releaseDate: null })
    }
    return present
  }, [setInfos, catalog])

  const activeSet = useMemo(() => {
    for (const t of terms) {
      const m = t.match(SET_TERM_RE)
      if (m) return m[1]!.toUpperCase()
    }
    return ''
  }, [terms])

  const onSetChange = (next: string) => {
    const without = removeTerms(query, (t) => SET_TERM_RE.test(t))
    onQueryChange(next ? appendTerm(without, `s:${next}`) : without)
  }

  // Color mode is derived from the query so a directly typed `c=wu` immediately
  // re-selects the "Exactly" segment without a separate state to drift.
  const colorOp = useMemo(() => detectColorOp(terms), [terms])
  const colorLetters = useMemo(() => collectColorLetters(terms), [terms])

  const keywordCount = KEYWORD_TOKENS.filter((k) => terms.includes(keywordToken(k))).length
  const statCount = [powMin, powMax, touMin, touMax].filter((v) => v !== null).length

  const toggleColorLetter = (letter: string) => {
    const next = new Set(colorLetters)
    if (next.has(letter)) next.delete(letter)
    else next.add(letter)
    onQueryChange(rewriteColorTokens(query, colorOp, next))
  }

  const changeColorMode = (newOp: ColorOp) => {
    onQueryChange(rewriteColorTokens(query, newOp, colorLetters))
  }

  return (
    <fieldset className={styles.filterFieldset}>
      <div className={styles.filterHeader}>
        <h2 className={styles.filterTitle}>
          Filters
          {terms.length > 0 && <span className={styles.filterCount}>{terms.length}</span>}
        </h2>
        <button
          type="button"
          className={styles.filterClear}
          onClick={() => onQueryChange('')}
          disabled={query.trim().length === 0}
        >
          Clear all
        </button>
      </div>
      {advanced && (
        <p className={styles.filterNote} role="status">
          Your search uses <code>or</code> or brackets — filters picked here narrow all of it.
        </p>
      )}
      {availableSets.length > 0 && (
        <section className={`${styles.section} ${styles.setPickerSection}`}>
          <h2 className={styles.sectionLabel}>Set</h2>
          <SetCombobox sets={availableSets} value={activeSet} onChange={onSetChange} />
        </section>
      )}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionLabel}>Colour</h2>
          <ColorModeSegmented op={colorOp} onChange={changeColorMode} />
        </div>
        <div className={styles.colorRow}>
          {COLOR_TOKENS.map(({ letter, label, key }) => {
            const active = colorLetters.has(letter)
            const name = key[0] + key.slice(1).toLowerCase()
            return (
              <button
                key={letter}
                className={`${styles.chip} ${styles.chipMana} ${active ? styles.chipActive : ''}`}
                onClick={() => toggleColorLetter(letter)}
                type="button"
                aria-label={name}
                aria-pressed={active}
                title={name}
              >
                <ManaSymbol symbol={label} size={20} />
              </button>
            )
          })}
          <button
            className={`${styles.chip} ${styles.chipMana} ${hasToken(query, 'is:colorless') ? styles.chipActive : ''}`}
            onClick={() => toggle('is:colorless')}
            type="button"
            aria-label="Colourless"
            aria-pressed={hasToken(query, 'is:colorless')}
            title="Colourless"
          >
            <ManaSymbol symbol="C" size={20} />
          </button>
        </div>
        <p className={styles.filterHint}>{COLOR_MODE_HINT[colorOp]}</p>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Type</h2>
        <div className={styles.filterRow}>
          {TYPE_TOKENS.map((label) => {
            const token = `t:${label}`
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
                aria-pressed={active}
              >
                {label}
              </button>
            )
          })}
          {(() => {
            // Legendary lives next to the type chips because it's the most common supertype
            // filter players reach for. `is:legendary` resolves via supertypes. The crown glyph
            // (♛) is the one the deck-row commander toggle uses, so the cue carries across.
            const token = 'is:legendary'
            const active = hasToken(query, token)
            return (
              <button
                key="legendary"
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
                aria-pressed={active}
              >
                <span aria-hidden="true" style={{ marginRight: 4, color: '#d4a017' }}>♛</span>
                Legendary
              </button>
            )
          })()}
        </div>
      </section>

      <SubtypeSection query={query} terms={terms} onQueryChange={onQueryChange} catalog={catalog} />

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Rarity</h2>
        <div className={styles.filterRow}>
          {RARITY_TOKENS.map(({ value, dot }) => {
            const token = `r:${value}`
            const active = hasToken(query, token)
            return (
              <button
                key={value}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
                aria-pressed={active}
              >
                <span className={styles.rarityDot} style={{ background: dot }} aria-hidden="true" />
                {value[0]!.toUpperCase() + value.slice(1)}
              </button>
            )
          })}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Mana value</h2>
        <div className={styles.mvRow} role="group" aria-label="Mana value">
          {MANA_VALUES.map((v) => {
            const active = mvSelected(cmcMin, cmcMax, v)
            return (
              <button
                key={v}
                type="button"
                className={`${styles.mvChip} ${active ? styles.mvChipActive : ''}`}
                aria-pressed={active}
                aria-label={v === MV_TOP ? 'Mana value 7 or more' : `Mana value ${v}`}
                onClick={() => {
                  const [lo, hi] = mvClick(cmcMin, cmcMax, v)
                  onQueryChange(setRange(query, 'cmc', lo, hi))
                }}
              >
                {v === MV_TOP ? '7+' : v}
              </button>
            )
          })}
        </div>
        <p className={styles.filterHint}>Click one value, then another to pick a range.</p>
      </section>

      <CollapsibleSection title="Power / toughness" activeCount={statCount}>
        <RangeRow
          label="Power"
          min={powMin}
          max={powMax}
          onMin={(v) => onQueryChange(setRange(query, 'pow', v, powMax))}
          onMax={(v) => onQueryChange(setRange(query, 'pow', powMin, v))}
        />
        <RangeRow
          label="Toughness"
          min={touMin}
          max={touMax}
          onMin={(v) => onQueryChange(setRange(query, 'tou', v, touMax))}
          onMax={(v) => onQueryChange(setRange(query, 'tou', touMin, v))}
        />
      </CollapsibleSection>

      <CollapsibleSection title="Keywords" activeCount={keywordCount}>
        <div className={styles.filterRow}>
          {KEYWORD_TOKENS.map((label) => {
            const token = keywordToken(label)
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
                aria-pressed={active}
              >
                {label}
              </button>
            )
          })}
        </div>
      </CollapsibleSection>
    </fieldset>
  )
}

/** `kw:first_strike` for "First strike" — KEYWORD enum names use underscores. */
function keywordToken(label: string): string {
  return `kw:${label.toLowerCase().replace(/\s+/g, '_')}`
}

const SET_TERM_RE = /^(?:s|set|e|edition):([^\s]+)$/i

/**
 * A section that folds away when nothing in it is in use — the rarely-touched filters (power,
 * keywords) don't push the common ones off screen. It opens by itself when one of its filters
 * becomes active (say, typed into the search bar), and its header counts them while folded.
 */
function CollapsibleSection({
  title,
  activeCount,
  children,
}: {
  title: string
  activeCount: number
  children: React.ReactNode
}) {
  const [open, setOpen] = useState(activeCount > 0)
  useEffect(() => {
    if (activeCount > 0) setOpen(true)
  }, [activeCount])
  return (
    <section className={styles.section}>
      <button
        type="button"
        className={styles.collapseHeader}
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <span className={open ? styles.collapseChevronOpen : styles.collapseChevron} aria-hidden="true" />
        <span className={styles.sectionLabel}>{title}</span>
        {activeCount > 0 && <span className={styles.filterCount}>{activeCount}</span>}
      </button>
      {open && children}
    </section>
  )
}

// ---------------------------------------------------------------------------
// Set combobox — the "primary lens" on the catalogue. Owns its own search,
// sort, and open/active state; communicates with the parent only through
// {sets, value, onChange}. Sort preference persists to localStorage.
// ---------------------------------------------------------------------------

function SetCombobox({
  sets,
  value,
  onChange,
}: {
  sets: SetInfo[]
  value: string
  onChange: (next: string) => void
}) {
  const [sortMode, setSortMode] = useState<'name' | 'date'>(() => {
    if (typeof window === 'undefined') return 'name'
    return (window.localStorage.getItem('deckbuilder.setSort') as 'name' | 'date') ?? 'name'
  })
  useEffect(() => {
    if (typeof window !== 'undefined') window.localStorage.setItem('deckbuilder.setSort', sortMode)
  }, [sortMode])

  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [activeIdx, setActiveIdx] = useState(0)

  const rootRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  // The combobox sits in a scrolling sidebar (`.left` has overflow), which would clip a wider
  // dropdown. Render the panel in a portal anchored to the input so it can overflow the sidebar
  // and sit above the card grid.
  const [panelPos, setPanelPos] = useState<{ top: number; left: number; width: number } | null>(null)

  const selected = useMemo(() => sets.find((s) => s.code === value) ?? null, [sets, value])

  const sorted = useMemo(() => {
    const arr = [...sets]
    if (sortMode === 'date') {
      arr.sort((a, b) => {
        if (!a.releaseDate && !b.releaseDate) return a.name.localeCompare(b.name)
        if (!a.releaseDate) return 1
        if (!b.releaseDate) return -1
        return b.releaseDate.localeCompare(a.releaseDate)
      })
    } else {
      arr.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
    }
    return arr
  }, [sets, sortMode])

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return sorted
    return sorted.filter(
      (s) => s.name.toLowerCase().includes(needle) || s.code.toLowerCase().includes(needle),
    )
  }, [sorted, search])

  // Close on outside click. mousedown (not click) so a selection inside fires before close.
  // The panel is portaled out of rootRef, so check it separately.
  useEffect(() => {
    if (!open) return
    function onDown(e: MouseEvent) {
      const target = e.target as Node
      if (!rootRef.current?.contains(target) && !panelRef.current?.contains(target)) {
        setOpen(false)
        setSearch('')
      }
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  // Anchor the portaled panel to the input, and keep it pinned while the sidebar scrolls or the
  // window resizes. Runs before paint to avoid a flash at the origin.
  useLayoutEffect(() => {
    if (!open) {
      setPanelPos(null)
      return
    }
    const reposition = () => {
      const rect = rootRef.current?.getBoundingClientRect()
      if (rect) setPanelPos({ top: rect.bottom + 4, left: rect.left, width: rect.width })
    }
    reposition()
    window.addEventListener('scroll', reposition, true) // capture: catch ancestor scrolls too
    window.addEventListener('resize', reposition)
    return () => {
      window.removeEventListener('scroll', reposition, true)
      window.removeEventListener('resize', reposition)
    }
  }, [open])

  // When opening, point the active row at the current selection so Enter is a no-op
  // by default and arrow keys move from "where you are".
  useEffect(() => {
    if (!open) return
    const idx = selected ? filtered.findIndex((s) => s.code === selected.code) : -1
    setActiveIdx(idx >= 0 ? idx : 0)
  }, [open, selected, filtered])

  // Keep active row in view as the user navigates.
  useEffect(() => {
    if (!open) return
    const el = listRef.current?.querySelector<HTMLElement>(`[data-idx="${activeIdx}"]`)
    el?.scrollIntoView({ block: 'nearest' })
  }, [open, activeIdx])

  function commit(code: string) {
    onChange(code)
    setSearch('')
    setOpen(false)
    inputRef.current?.blur()
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (!open) setOpen(true)
      else setActiveIdx((i) => Math.min(filtered.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIdx((i) => Math.max(0, i - 1))
    } else if (e.key === 'Enter') {
      if (!open) return
      e.preventDefault()
      const pick = filtered[activeIdx]
      if (pick) commit(pick.code)
    } else if (e.key === 'Escape' && open) {
      e.preventDefault()
      setOpen(false)
      setSearch('')
    } else if (e.key === 'Backspace' && !search && selected && !open) {
      e.preventDefault()
      commit('')
    }
  }

  const displayValue = open ? search : selected ? `${selected.name} (${selected.code})` : ''
  const placeholder = selected ? '' : 'All sets — type to search'
  const activeOptionId = open && filtered[activeIdx] ? `set-opt-${filtered[activeIdx].code}` : undefined

  return (
    <div className={styles.setCombobox} ref={rootRef}>
      <div className={`${styles.setComboInput} ${open ? styles.setComboInputOpen : ''}`}>
        {selected && !open ? (
          <SetIcon code={selected.code} className={styles.setComboIcon} />
        ) : (
          <span className={styles.setComboIcon} aria-hidden="true">⌕</span>
        )}
        <input
          ref={inputRef}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-controls="set-combobox-list"
          aria-autocomplete="list"
          {...(activeOptionId ? { 'aria-activedescendant': activeOptionId } : {})}
          value={displayValue}
          placeholder={placeholder}
          spellCheck={false}
          autoComplete="off"
          onChange={(e) => {
            setSearch(e.target.value)
            if (!open) setOpen(true)
            setActiveIdx(0)
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        {selected && !open && (
          <button
            type="button"
            className={styles.setComboClear}
            aria-label={`Clear set filter (${selected.name})`}
            onMouseDown={(e) => {
              e.preventDefault()
              commit('')
            }}
          >
            ×
          </button>
        )}
      </div>
      {open && panelPos &&
        createPortal(
        <div
          ref={panelRef}
          className={styles.setComboPanel}
          style={{ top: panelPos.top, left: panelPos.left, minWidth: panelPos.width }}
        >
          <div className={styles.setComboPanelHeader}>
            <span className={styles.setComboCount}>
              {filtered.length} {filtered.length === 1 ? 'set' : 'sets'}
            </span>
            <div className={styles.modeSegmented} role="tablist" aria-label="Sort sets">
              <button
                type="button"
                role="tab"
                aria-selected={sortMode === 'name'}
                className={`${styles.modeButton} ${sortMode === 'name' ? styles.modeButtonActive : ''}`}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setSortMode('name')}
              >
                Name
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={sortMode === 'date'}
                className={`${styles.modeButton} ${sortMode === 'date' ? styles.modeButtonActive : ''}`}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setSortMode('date')}
              >
                Release
              </button>
            </div>
          </div>
          <ul
            id="set-combobox-list"
            ref={listRef}
            className={styles.setComboList}
            role="listbox"
            aria-label="Sets"
          >
            <li
              id="set-opt-all"
              role="option"
              aria-selected={!selected}
              data-idx={-1}
              className={`${styles.setComboOption} ${styles.setComboOptionAll} ${!selected ? styles.setComboOptionSelected : ''}`}
              onMouseDown={(e) => {
                e.preventDefault()
                commit('')
              }}
            >
              <span className={styles.setComboOptionName}>All sets</span>
            </li>
            {filtered.map((s, i) => {
              const isActive = i === activeIdx
              const isSelected = s.code === value
              const year = s.releaseDate ? s.releaseDate.slice(0, 4) : '—'
              const classes = [
                styles.setComboOption,
                isActive ? styles.setComboOptionActive : '',
                isSelected ? styles.setComboOptionSelected : '',
              ]
                .filter(Boolean)
                .join(' ')
              return (
                <li
                  key={s.code}
                  id={`set-opt-${s.code}`}
                  data-idx={i}
                  role="option"
                  aria-selected={isSelected}
                  className={classes}
                  onMouseEnter={() => setActiveIdx(i)}
                  onMouseDown={(e) => {
                    e.preventDefault()
                    commit(s.code)
                  }}
                >
                  <SetIcon code={s.code} className={styles.setComboOptionIcon} />
                  <span className={styles.setComboOptionName}>{s.name}</span>
                  <span className={styles.setComboOptionMeta}>
                    <span className={styles.setComboOptionYear}>{year}</span>
                    <span className={styles.setComboOptionCode}>{s.code}</span>
                  </span>
                </li>
              )
            })}
            {filtered.length === 0 && (
              <li className={styles.setComboEmpty}>No sets match "{search}"</li>
            )}
          </ul>
        </div>,
        document.body,
      )}
    </div>
  )
}

function SubtypeSection({
  query,
  terms,
  onQueryChange,
  catalog,
}: {
  query: string
  terms: string[]
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
}) {
  const [input, setInput] = useState('')

  const allSubtypes = useMemo(() => {
    const set = new Set<string>()
    for (const c of catalog) for (const s of c.subtypes) set.add(s)
    return [...set].sort()
  }, [catalog])

  // Active subtype tokens: any t:<X> where X (case-insensitive) is a known
  // subtype. This lets the type-row chips (t:Creature, etc.) coexist without
  // showing as subtype chips here.
  const activeSubtypes = useMemo(() => {
    if (allSubtypes.length === 0) return [] as Array<{ token: string; label: string }>
    const subSet = new Set(allSubtypes.map((s) => s.toLowerCase()))
    const out: Array<{ token: string; label: string }> = []
    for (const term of terms) {
      const m = term.match(/^(-?)t:"?([^"]+?)"?$/)
      if (m && subSet.has(m[2]!.toLowerCase())) {
        out.push({ token: term, label: m[1] ? `not ${m[2]}` : m[2]! })
      }
    }
    return out
  }, [terms, allSubtypes])

  const addSubtype = (raw: string) => {
    const trimmed = raw.trim()
    if (!trimmed) return
    const canonical = allSubtypes.find((s) => s.toLowerCase() === trimmed.toLowerCase()) ?? trimmed
    const tokenValue = canonical.includes(' ') ? `"${canonical}"` : canonical
    onQueryChange(addToken(query, `t:${tokenValue}`))
    setInput('')
  }

  if (allSubtypes.length === 0) return null

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionLabel}>Subtype / tribe</h2>
      {activeSubtypes.length > 0 && (
        <div className={styles.filterRow}>
          {activeSubtypes.map(({ token, label }) => (
            <button
              key={token}
              className={`${styles.chip} ${styles.chipActive}`}
              onClick={() => onQueryChange(removeToken(query, token))}
              type="button"
              title="Remove"
              aria-label={`Remove ${label}`}
            >
              {label} <span aria-hidden="true">×</span>
            </button>
          ))}
        </div>
      )}
      <input
        list="deckbuilder-subtypes"
        className={styles.textInput}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            addSubtype(input)
          }
        }}
        onBlur={() => {
          // Auto-add on blur if the entry exactly matches a known subtype,
          // so picking from the datalist (which doesn't always fire Enter)
          // still works.
          const exact = allSubtypes.find((s) => s.toLowerCase() === input.trim().toLowerCase())
          if (exact) addSubtype(exact)
        }}
        placeholder="Type a tribe — Goblin, Wizard…"
        aria-label="Add a subtype"
      />
      <datalist id="deckbuilder-subtypes">
        {allSubtypes.map((s) => (
          <option key={s} value={s} />
        ))}
      </datalist>
    </section>
  )
}

// ---------------------------------------------------------------------------
// Color mode segmented control + helpers
// ---------------------------------------------------------------------------

type ColorOp = ':' | '=' | '<='

function ColorModeSegmented({
  op,
  onChange,
}: {
  op: ColorOp
  onChange: (op: ColorOp) => void
}) {
  const options: Array<{ op: ColorOp; label: string; title: string }> = [
    { op: ':', label: 'Includes', title: 'Cards that include the chosen colour(s)' },
    { op: '=', label: 'Exactly', title: 'Cards whose colours are exactly the chosen set' },
    { op: '<=', label: 'At most', title: 'Cards you could play using only the chosen colours' },
  ]
  return (
    <div className={styles.modeSegmented} role="group" aria-label="Colour comparison mode">
      {options.map((opt) => (
        <button
          key={opt.op}
          className={op === opt.op ? styles.modeButtonActive : styles.modeButton}
          aria-pressed={op === opt.op}
          onClick={() => onChange(opt.op)}
          title={opt.title}
          type="button"
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

const COLOR_TERM_RE = /^c(<=|=|:)([wubrg]+)$/i

function detectColorOp(terms: string[]): ColorOp {
  for (const t of terms) {
    const m = t.match(COLOR_TERM_RE)
    if (m) return m[1] as ColorOp
  }
  return ':'
}

function collectColorLetters(terms: string[]): Set<string> {
  const out = new Set<string>()
  for (const t of terms) {
    const m = t.match(COLOR_TERM_RE)
    if (m) for (const ch of m[2]!.toLowerCase()) out.add(ch)
  }
  return out
}

function rewriteColorTokens(query: string, op: ColorOp, letters: Set<string>): string {
  // Strip every existing colour term regardless of operator so we don't leave a stale
  // `c:X` behind when switching to exactly / at most.
  let next = removeTerms(query, (t) => COLOR_TERM_RE.test(t))
  if (letters.size === 0) return next
  const sorted = [...letters].sort().join('')
  if (op === ':') {
    // Per-letter terms — "must contain each chosen colour" reads cleanly.
    for (const l of sorted) next = appendTerm(next, `c:${l}`)
    return next
  }
  return appendTerm(next, `c${op}${sorted}`)
}

/** Whether mana-value chip [v] (7 = "7+") lies in the selected range. */
function mvSelected(min: number | null, max: number | null, v: number): boolean {
  if (min === null && max === null) return false
  const lo = min ?? 0
  const hi = max ?? Infinity
  return v === MV_TOP ? hi >= MV_TOP : lo <= v && v <= hi
}

/**
 * The range after clicking mana-value chip [v]: an empty selection becomes that value; a value
 * outside the range stretches it; an end of a wider range shrinks it; the lone selected value
 * clears it; a value inside narrows to just that one.
 */
export function mvClick(min: number | null, max: number | null, v: number): [number | null, number | null] {
  const top = v === MV_TOP
  if (min === null && max === null) return top ? [MV_TOP, null] : [v, v]
  const lo = min ?? 0
  const hi = max ?? Infinity
  const single = top ? lo === MV_TOP && hi === Infinity : lo === v && hi === v
  if (single) return [null, null]
  const inside = top ? hi >= MV_TOP : lo <= v && v <= hi
  if (!inside) {
    const nlo = Math.min(lo, top ? MV_TOP : v)
    const nhi = top || hi === Infinity ? null : Math.max(hi, v)
    return [nlo === 0 ? null : nlo, nhi]
  }
  if (top) return [lo === 0 ? null : lo, MV_TOP - 1]
  if (v === lo) return [v + 1, hi === Infinity ? null : hi]
  if (v === hi) return [lo === 0 ? null : lo, v - 1]
  return [v, v]
}

function RangeRow({
  label,
  min,
  max,
  onMin,
  onMax,
}: {
  label: string
  min: number | null
  max: number | null
  onMin: (v: string) => void
  onMax: (v: string) => void
}) {
  return (
    <div className={styles.rangeRow}>
      <span className={styles.rangeLabel}>{label}</span>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={20}
        className={styles.numericInput}
        value={min ?? ''}
        placeholder="any"
        aria-label={`${label} at least`}
        onChange={(e) => onMin(e.target.value)}
      />
      <span aria-hidden="true">to</span>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={20}
        className={styles.numericInput}
        value={max ?? ''}
        placeholder="any"
        aria-label={`${label} at most`}
        onChange={(e) => onMax(e.target.value)}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Range helpers (numeric inputs ↔ query string)
//
// Read and write `<key>>=N`, `<key><=N` and `<key>=N` terms so the panel and the raw query
// stay in sync. Typed aliases (`mv<=2`, `power>=4`) count too.
// ---------------------------------------------------------------------------

const RANGE_ALIASES: Record<string, string> = {
  cmc: 'cmc|mv|manavalue',
  pow: 'pow|power',
  tou: 'tou|toughness',
}

function rangeTermRe(key: string): RegExp {
  return new RegExp(`^(?:${RANGE_ALIASES[key] ?? key})(>=|<=|=|:)(\\d+)$`, 'i')
}

export function extractRange(query: string, key: string): [number | null, number | null] {
  const re = rangeTermRe(key)
  let min: number | null = null
  let max: number | null = null
  for (const t of queryTerms(query)) {
    const m = t.match(re)
    if (!m) continue
    const n = parseInt(m[2]!, 10)
    if (m[1] === '>=') min = n
    else if (m[1] === '<=') max = n
    else min = max = n
  }
  return [min, max]
}

export function setRange(
  query: string,
  key: string,
  min: string | number | null,
  max: string | number | null,
): string {
  const re = rangeTermRe(key)
  let next = removeTerms(query, (t) => re.test(t))
  const minN = typeof min === 'string' ? (min === '' ? null : parseInt(min, 10)) : min
  const maxN = typeof max === 'string' ? (max === '' ? null : parseInt(max, 10)) : max
  const lo = minN !== null && Number.isFinite(minN) ? minN : null
  const hi = maxN !== null && Number.isFinite(maxN) ? maxN : null
  if (lo !== null && lo === hi) return appendTerm(next, `${key}=${lo}`)
  if (lo !== null) next = appendTerm(next, `${key}>=${lo}`)
  if (hi !== null) next = appendTerm(next, `${key}<=${hi}`)
  return next
}
