import { forwardRef } from 'react'
import { CARD_BACK_IMAGE_URL } from '@/utils/cardImages.ts'
import styles from './Flip.module.css'

/**
 * The flipped card itself: a two-faced element the overlays spin in 3D. Positioned and posed by
 * its parent through refs (the flight runs per frame, outside React), so it renders once.
 */
export const FlipOrb = forwardRef<
  HTMLDivElement,
  { imageUrl: string; className?: string | undefined; faceClassName?: string | undefined }
>(function FlipOrb({ imageUrl, className, faceClassName }, ref) {
  return (
    <div ref={ref} className={`${styles.orb} ${className ?? ''}`}>
      <div className={`${styles.orbFace} ${faceClassName ?? ''}`} style={{ backgroundImage: `url("${imageUrl}")` }} />
      <div
        className={`${styles.orbFace} ${styles.orbBack} ${faceClassName ?? ''}`}
        style={{ backgroundImage: `url("${CARD_BACK_IMAGE_URL}")` }}
      />
    </div>
  )
})
