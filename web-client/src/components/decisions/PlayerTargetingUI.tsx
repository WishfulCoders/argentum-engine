import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseTargetsDecision } from '@/types'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'

/**
 * Player-only targeting UI for ChooseTargetsDecision.
 * Shows a side banner with Cancel button when the decision supports cancellation.
 */
export function PlayerTargetingUI({
  decision,
}: {
  decision: ChooseTargetsDecision
}) {
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const emptyChoiceLabel = decision.targetRequirements[0]?.emptyChoiceLabel
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)

  const handleCancel = () => {
    submitCancelDecision(decision.id)
  }

  return (
    <DraggableBanner className={styles.sideBannerTarget}>
      <div className={styles.bannerTitle}>
        Choose Target
      </div>
      <div className={styles.prompt}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        Click a player's life total
      </div>
      {emptyChoiceLabel && (
        <div className={styles.buttonContainerSmall}>
          <button className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
            onClick={() => submitTargetsDecision(decision.id, { 0: [] })}>
            {emptyChoiceLabel}
          </button>
        </div>
      )}
      {decision.canCancel && (
        <div className={styles.buttonContainerSmall}>
          <button onClick={handleCancel} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Cancel
          </button>
        </div>
      )}
    </DraggableBanner>
  )
}
