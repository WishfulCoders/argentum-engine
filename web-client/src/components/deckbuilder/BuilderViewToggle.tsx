/**
 * Search cards | Deck list — the two views inside one builder mode.
 *
 * Deliberately styled as underlined tabs with icons, unlike the pill-shaped {@link BuilderModeSwitch}
 * above it: the switch picks *what* you build (a deck or a cube), these pick *how you look at it*
 * (the card search, or the whole list), and two identical pill toggles stacked on each other read
 * as the same control. The list tab carries the card count so it's plainly "the thing you're building".
 */
import styles from './BuilderViewToggle.module.css'

export type BuilderView = 'cards' | 'list'

export function BuilderViewToggle({
  noun,
  count,
  view,
  onChange,
  className,
}: {
  /** What is being built: "Deck" or "Cube". */
  noun: 'Deck' | 'Cube'
  count: number
  view: BuilderView
  onChange: (view: BuilderView) => void
  className?: string | undefined
}) {
  return (
    <div className={`${styles.tabs} ${className ?? ''}`} role="group" aria-label="View">
      <button
        type="button"
        className={styles.tab}
        aria-pressed={view === 'cards'}
        onClick={() => onChange('cards')}
        title="Search and filter the card catalog; click a card to add it"
      >
        <svg className={styles.icon} viewBox="0 0 16 16" aria-hidden>
          <circle cx="7" cy="7" r="4.5" />
          <path d="M10.5 10.5 14 14" />
        </svg>
        Search cards
      </button>
      <button
        type="button"
        className={styles.tab}
        aria-pressed={view === 'list'}
        onClick={() => onChange('list')}
        title={`See the whole ${noun.toLowerCase()} at once, grouped into columns`}
      >
        <svg className={styles.icon} viewBox="0 0 16 16" aria-hidden>
          <path d="M2 4h12M2 8h12M2 12h12" />
        </svg>
        {noun} list
        <span className={styles.count}>{count}</span>
      </button>
    </div>
  )
}
