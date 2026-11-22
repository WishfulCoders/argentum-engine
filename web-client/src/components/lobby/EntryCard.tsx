/**
 * The card a deep link lands on — `/join/:lobbyId` and `/tournament/:lobbyId` — while it asks for a
 * name, connects and joins. One glass card centred over the card art, in the page frame every
 * standalone screen shares; the two routes differ only in their copy and the tournament's facts.
 */
import type React from 'react'
import { Link } from 'react-router-dom'
import { PageShell, pageStyles } from '@/components/ui/PageShell'
import styles from './EntryCard.module.css'

interface EntryCardProps {
  /** Small caps line above the title — what kind of invite this is. */
  kicker: string
  title: string
  /** The lobby / tournament id from the link, shown so the visitor can tell it's the right one. */
  code?: string | undefined
  /** Facts about what is being joined (format, players…), when the server told us. */
  facts?: readonly { label: string; value: React.ReactNode }[] | undefined
  error?: string | null | undefined
  /** Present while the visitor still has to give a name. */
  nameEntry?: {
    value: string
    onChange: (v: string) => void
    onSubmit: () => void
    submitLabel: string
  } | undefined
  /** Shown with a spinner when neither the name entry nor an error is up. */
  progressText: string
}

export function EntryCard({ kicker, title, code, facts, error, nameEntry, progressText }: EntryCardProps) {
  return (
    <PageShell width="narrow">
      <div className={styles.center}>
        <section className={`${pageStyles.panel} ${styles.card}`} aria-live="polite">
          <span className={styles.kicker}>{kicker}</span>
          <h1 className={styles.title}>{title}</h1>
          {code && <span className={styles.code}>{code}</span>}

          {facts && facts.length > 0 && (
            <dl className={styles.facts}>
              {facts.map((f) => (
                <div key={f.label} className={styles.fact}>
                  <dt>{f.label}</dt>
                  <dd>{f.value}</dd>
                </div>
              ))}
            </dl>
          )}

          {error ? (
            <>
              <p className={styles.error} role="alert">{error}</p>
              <Link to="/" className={`${pageStyles.button} ${styles.wide}`}>Back to home</Link>
            </>
          ) : nameEntry ? (
            <form
              className={styles.form}
              onSubmit={(e) => { e.preventDefault(); nameEntry.onSubmit() }}
            >
              <label className={styles.label} htmlFor="entry-name">Your name</label>
              <input
                id="entry-name"
                type="text"
                value={nameEntry.value}
                onChange={(e) => nameEntry.onChange(e.target.value)}
                placeholder="Your name"
                autoFocus
                maxLength={20}
                className={`${pageStyles.input} ${styles.input}`}
              />
              <button
                type="submit"
                disabled={!nameEntry.value.trim()}
                className={`${pageStyles.buttonPrimary} ${styles.submit}`}
              >
                {nameEntry.submitLabel}
              </button>
            </form>
          ) : (
            <div className={styles.progress}>
              <span className={styles.spinner} aria-hidden />
              <span>{progressText}</span>
            </div>
          )}
        </section>
      </div>
    </PageShell>
  )
}
