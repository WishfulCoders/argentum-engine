import { useEffect, useState } from 'react'
import { type Spoils, fetchSpoils } from '@/api/account'
import { AnteCardThumb, AnteCoinMark } from '@/components/ante/AnteCardThumb'
import anteStyles from '@/components/ante/Ante.module.css'
import { pageStyles as p } from '@/components/ui/PageShell'
import { accountStyles as a } from './accountUi'

/**
 * The profile's **Spoils**: cards won in games played for ante (CR 407), newest first, each with
 * the usual hover preview and who it was won from — plus a quiet count of what went the other way.
 * Outside a Limited tournament an ante win is a trophy, and this is where the trophies live.
 */
export function SpoilsPanel() {
  const [spoils, setSpoils] = useState<Spoils | null>(null)

  useEffect(() => {
    let live = true
    void fetchSpoils()
      .then((s) => { if (live) setSpoils(s) })
      .catch(() => { if (live) setSpoils({ won: [], lost: [] }) })
    return () => { live = false }
  }, [])

  // Nothing until the first answer, so the empty state never flashes before the trophies arrive.
  if (!spoils) return null

  const { won, lost } = spoils
  const lostTitle = lost.map((l) => `${l.cardName} — to ${l.opponentName}`).join('\n')

  return (
    <section className={p.panel} data-testid="profile-spoils">
      <div className={a.sectionHead}>
        <h2 className={a.sectionTitle} style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          <AnteCoinMark />
          Spoils
        </h2>
        {won.length > 0 && (
          <span className={a.count}>{won.length} card{won.length === 1 ? '' : 's'} won for ante</span>
        )}
      </div>
      {won.length === 0 ? (
        <p className={a.muted}>No spoils yet — win a game played for ante.</p>
      ) : (
        <div className={anteStyles.cards} style={{ justifyContent: 'flex-start', marginTop: 4 }}>
          {won.map((s, i) => (
            <AnteCardThumb
              key={`${s.gameId ?? 'game'}-${s.cardName}-${i}`}
              cardName={s.cardName}
              imageUri={s.imageUri}
              caption={`won from ${s.opponentName}`}
              tone="other"
            />
          ))}
        </div>
      )}
      {lost.length > 0 && (
        <p className={a.muted} style={{ marginTop: 12, fontSize: 13 }} title={lostTitle}>
          Lost to the ante: {lost.length} card{lost.length === 1 ? '' : 's'}
          {' — '}
          {lost.slice(0, 3).map((l) => l.cardName).join(', ')}
          {lost.length > 3 ? ', …' : ''}
        </p>
      )}
    </section>
  )
}
