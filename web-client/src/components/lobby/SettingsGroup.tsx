/**
 * One always-visible settings group: axis controls in the header, refinements below.
 *
 * There used to be a summary line under the strip ("Standard", "1v1", "Booster Draft · ECL · 3 packs")
 * — a holdover from when the groups collapsed. With every control on screen it only repeated the
 * highlighted button and the rows beneath it, so the header now carries just the blocking note.
 */
import type { ReactNode } from 'react'
import { HelpTip } from '@/components/help/HelpTip'
import styles from '../ui/GameUI.module.css'

export function SettingsGroup({
  label,
  topicId,
  axisStrip,
  blocking,
  testId,
  children,
}: {
  label: string
  /** Help topic for the *value in effect*, so `?` explains what is selected. */
  topicId: string | null
  /** The axis's buttons — always visible, never behind the chevron. */
  axisStrip?: ReactNode
  /** This group holds the reason Start is disabled. */
  blocking?: string | undefined
  testId: string
  /** The refinements, when this lobby shape has any. */
  children?: ReactNode
}) {
  const hasBody = Boolean(children)

  return (
    <div
      className={`${styles.settingsGroup} ${blocking ? styles.settingsGroupBlocking : ''}`}
      data-testid={`settings-group-${testId}`}
    >
      <div className={styles.settingsGroupHeader}>
        <div className={styles.settingsGroupLabel}>
          <span>{label}</span>
          {topicId && <HelpTip topicId={topicId} label={`What is ${label}?`} size="sm" />}
        </div>
        <div className={styles.settingsGroupMain}>
          {axisStrip}
          {blocking && (
            <div className={styles.settingsGroupSummaryRow}>
              <span className={styles.settingsGroupBlockingNote} title={blocking}>! {blocking}</span>
            </div>
          )}
        </div>
      </div>
      {hasBody && <div className={styles.settingsGroupBody}>{children}</div>}
    </div>
  )
}
