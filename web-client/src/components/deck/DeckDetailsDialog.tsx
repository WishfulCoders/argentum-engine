/**
 * Name and note for a saved deck, edited together — what a deck gallery's ✎ opens.
 *
 * Replaces a bare `window.prompt('Rename deck')`: a note is half of how a deck is told apart in a
 * picker, so it belongs in the same place you rename it. The cover is chosen elsewhere (the deck's
 * header in the deckbuilder), where its cards are on screen to choose from.
 */
import { useEffect, useId, useRef, useState } from 'react'
import { DECK_NOTE_MAX_LENGTH } from '@/store/deckLibrary'
import styles from './DeckDetailsDialog.module.css'

export function DeckDetailsDialog({
  name: initialName,
  note: initialNote,
  onSave,
  onCancel,
}: {
  name: string
  note: string
  onSave: (details: { name: string; note: string }) => void
  onCancel: () => void
}) {
  const [name, setName] = useState(initialName)
  const [note, setNote] = useState(initialNote)
  const nameRef = useRef<HTMLInputElement>(null)
  const titleId = useId()

  useEffect(() => {
    nameRef.current?.select()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onCancel])

  const trimmed = name.trim()
  const unchanged = trimmed === initialName.trim() && note.trim() === initialNote.trim()

  return (
    <div className={styles.backdrop} onClick={onCancel}>
      <form
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => {
          e.preventDefault()
          if (trimmed) onSave({ name: trimmed, note: note.trim() })
        }}
      >
        <h2 id={titleId} className={styles.title}>Deck details</h2>
        <label className={styles.field}>
          <span className={styles.label}>Name</span>
          <input
            ref={nameRef}
            className={styles.input}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Deck name"
            spellCheck={false}
          />
        </label>
        <label className={styles.field}>
          <span className={styles.labelRow}>
            <span className={styles.label}>Note</span>
            <span className={styles.counter} data-near={note.length > DECK_NOTE_MAX_LENGTH - 20 || undefined}>
              {note.length}/{DECK_NOTE_MAX_LENGTH}
            </span>
          </span>
          <textarea
            className={`${styles.input} ${styles.textarea}`}
            value={note}
            onChange={(e) => setNote(e.target.value.slice(0, DECK_NOTE_MAX_LENGTH))}
            placeholder="The plan, a matchup, a reminder…"
            maxLength={DECK_NOTE_MAX_LENGTH}
            rows={2}
          />
        </label>
        <div className={styles.actions}>
          <button type="button" className={styles.secondary} onClick={onCancel}>
            Cancel
          </button>
          <button type="submit" className={styles.primary} disabled={!trimmed || unchanged}>
            Save
          </button>
        </div>
      </form>
    </div>
  )
}
