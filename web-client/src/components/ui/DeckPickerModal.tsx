/**
 * DeckPickerModal — the shell every "what does this seat bring?" dialog opens in.
 *
 * There were four of these: the quick lobby's own seat and the premade-decks tournament seat shared
 * one, the quick lobby's AI seat had a near-copy, and the pod's AI seat used the generic confirm
 * panel with a hand-tuned `maxWidth`. So the same question arrived in three different sizes
 * depending on which seat you clicked. One shell, one header, one Done button.
 *
 * Named for the seat it belongs to (`title`) with one line of orientation under it (`subtitle`),
 * because these are always opened *from a player row* and the first thing to be sure of is whose
 * deck is about to change.
 *
 * The footer can carry the lobby's next step (`primary`) — "Ready up", or "Start game" against the
 * AI — so picking a deck and committing to it is one motion rather than Done, find the action bar,
 * press Ready. Done stays beside it for when you only came to look.
 */
import type { ReactNode } from 'react'
import styles from './GameUI.module.css'

export function DeckPickerModal({
  title,
  subtitle = 'Choose the deck for this player seat.',
  hidden = false,
  primary,
  selection,
  onClose,
  children,
}: {
  title: string
  subtitle?: string
  /**
   * Keep the dialog — and the picker inside it — mounted but out of sight. A picker submits as it
   * resolves, so a seat whose deck is already decided needs it alive before anyone opens it.
   */
  hidden?: boolean
  /** The lobby's next step, offered where the deck was just chosen. Closes the dialog when run. */
  primary?: { label: string; disabled: boolean; reason?: string | undefined; onRun: () => void } | undefined
  /** The deck currently chosen, named in the footer — the gallery may have scrolled it away. */
  selection?: string | null | undefined
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div
      className={styles.confirmBackdrop}
      role="dialog"
      aria-modal="true"
      aria-hidden={hidden || undefined}
      style={hidden ? { display: 'none' } : undefined}
      onClick={onClose}
    >
      <div className={styles.deckPickerModal} onClick={(event) => event.stopPropagation()}>
        <div className={styles.deckPickerModalHeader}>
          <div>
            <div className={styles.confirmTitle}>{title}</div>
            <p className={styles.confirmBody}>{subtitle}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className={styles.deckPickerModalClose}
            aria-label="Close deck picker"
          >
            ×
          </button>
        </div>
        {children}
        <div className={styles.deckPickerModalFooter}>
          {primary?.disabled && primary.reason ? (
            <span className={styles.deckPickerModalReason}>{primary.reason}</span>
          ) : selection ? (
            <span className={styles.deckPickerModalSelection}>
              <span className={styles.deckPickerModalSelectionLabel}>Playing</span>
              <span className={styles.deckPickerModalSelectionName}>{selection}</span>
            </span>
          ) : null}
          <button
            type="button"
            onClick={onClose}
            className={primary ? styles.deckPickerModalSecondary : styles.deckPickerModalPrimary}
          >
            Done
          </button>
          {primary && (
            <button
              type="button"
              onClick={() => { primary.onRun(); onClose() }}
              disabled={primary.disabled}
              className={styles.deckPickerModalPrimary}
              data-testid="deck-modal-primary"
            >
              {primary.label}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
