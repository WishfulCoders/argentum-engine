/**
 * How a cube becomes packs: the {@link BOOSTER_STYLES} choice with the pack drawn slot by slot, the
 * pack size a shuffled cube is dealt in, and the set its basic lands come from.
 *
 * A strip across the lobby dialog (`layout="bar"`), a stacked block in the builder's cube rail
 * (`layout="stacked"`).
 */
import { BOOSTER_STYLES, boosterStyleInfo, isBoosterStyle } from './cubeModel'
import { PackStrip } from './CubeStats'
import type { CubeDraft } from './useCubeDraft'
import styles from './CubeEditor.module.css'

export function CubePackControls({ draft, layout }: { draft: CubeDraft; layout: 'bar' | 'stacked' }) {
  const { boosterStyle, setBoosterStyle, packSize, setPackSize, basicLandSetCode, setBasicLandSetCode } = draft
  const boosters = isBoosterStyle(boosterStyle)

  return (
    <div className={layout === 'bar' ? styles.packBar : styles.packStack}>
      <div className={styles.field}>
        <span className={styles.fieldLabel} id={`cube-pack-style-${layout}`}>Packs</span>
        <div className={styles.segmented} role="radiogroup" aria-labelledby={`cube-pack-style-${layout}`}>
          {BOOSTER_STYLES.map((option) => (
            <button
              key={option.style}
              type="button"
              role="radio"
              aria-checked={boosterStyle === option.style}
              className={styles.segment}
              onClick={() => setBoosterStyle(option.style)}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>
      <div className={styles.packExplain}>
        {boosters && <PackStrip style={boosterStyle} short={draft.shortSlots} />}
        <span className={styles.packBlurb}>{boosterStyleInfo(boosterStyle).blurb}</span>
      </div>
      <div className={styles.packFields}>
        {!boosters && (
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Pack size</span>
            <input
              className={`${styles.input} ${styles.numberInput}`}
              type="number"
              min={1}
              max={30}
              value={packSize}
              onChange={(e) => setPackSize(Math.max(1, Math.min(30, Number(e.target.value) || 1)))}
            />
          </label>
        )}
        <label className={`${styles.field} ${styles.landField}`}>
          <span className={styles.fieldLabel}>Basic land art</span>
          <select className={styles.select} value={basicLandSetCode} onChange={(e) => setBasicLandSetCode(e.target.value)}>
            {draft.landOptions.map((set) => (
              <option key={set.code} value={set.code}>
                {set.name}
              </option>
            ))}
          </select>
        </label>
      </div>
    </div>
  )
}
