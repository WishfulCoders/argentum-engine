import type { AnteResult, AnteTransfer } from '@/types'
import { AnteCardThumb, AnteCoinMark } from './AnteCardThumb'
import { anteTransferCaption, viewAnteResult } from './anteResult'
import s from './Ante.module.css'

/**
 * The "Ante" section of the game-over card (CR 407): what the viewer won and lost, or — for a
 * spectator or an uninvolved seat — who won what from whom. Server-decided; this only lays it out.
 */
export function AnteResultPanel({ ante, viewerId }: { ante: AnteResult; viewerId: string | null }) {
  const view = viewAnteResult(ante, viewerId)
  return (
    <section className={s.section} aria-label="Ante" data-testid="ante-result">
      <h2 className={s.heading}><AnteCoinMark />Ante</h2>
      {view.emptyLine && <p className={s.empty}>{view.emptyLine}</p>}
      <Group label="You won" tone="won" transfers={view.won} />
      <Group label="You lost" tone="lost" transfers={view.lost} />
      <Group label={view.won.length + view.lost.length > 0 ? 'Elsewhere at the table' : null} tone="other" transfers={view.others} />
      {view.refillLines.map((line) => <p key={line} className={s.subline}>{line}</p>)}
      {view.scopeLine && <p className={s.subline}>{view.scopeLine}</p>}
    </section>
  )
}

function Group({
  label,
  tone,
  transfers,
}: {
  label: string | null
  tone: 'won' | 'lost' | 'other'
  transfers: readonly AnteTransfer[]
}) {
  if (transfers.length === 0) return null
  return (
    <div className={s.group}>
      {label && <p className={s.groupLabel} data-tone={tone}>{label}</p>}
      <div className={s.cards}>
        {transfers.map((t, i) => (
          <AnteCardThumb
            key={`${t.cardName}-${t.fromPlayerId}-${i}`}
            cardName={t.cardName}
            imageUri={t.imageUri}
            caption={anteTransferCaption(t, tone)}
            tone={tone}
          />
        ))}
      </div>
    </div>
  )
}
