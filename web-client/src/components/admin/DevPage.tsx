/**
 * The frame for the admin dashboard and the dev tools (LLM tournament, AI sandbox): the shared
 * {@link PageShell} top bar over the card art, and a page heading with an optional "dev" tag.
 */
import type React from 'react'
import { PageShell, pageStyles } from '@/components/ui/PageShell'
import css from './devPage.module.css'

export { css as devStyles }

interface DevPageProps {
  /** Breadcrumb beside the wordmark — the area ("Admin", "Dev tools"). */
  section: string
  title: React.ReactNode
  subtitle?: React.ReactNode | undefined
  /** Marks a tool that only exists with dev endpoints on. */
  dev?: boolean
  /** A back affordance before the heading (sub-screens of a hub). */
  onBack?: (() => void) | undefined
  backLabel?: string
  /** Controls at the end of the heading row. */
  right?: React.ReactNode
  width?: 'narrow' | 'normal' | 'wide' | 'full'
  children: React.ReactNode
}

export function DevPage({ section, title, subtitle, dev = false, onBack, backLabel = '← Back', right, width = 'normal', children }: DevPageProps) {
  return (
    <PageShell title={section} width={width} plain>
      <div className={css.head}>
        {onBack && (
          <button type="button" className={`${pageStyles.buttonGhost} ${css.back}`} onClick={onBack}>
            {backLabel}
          </button>
        )}
        <div className={css.headText}>
          <h1 className={pageStyles.h1}>
            {title}
            {dev && <span className={css.devTag}>dev</span>}
          </h1>
          {subtitle && <p className={css.subtitle}>{subtitle}</p>}
        </div>
        {right && <div className={css.right}>{right}</div>}
      </div>
      {children}
    </PageShell>
  )
}
