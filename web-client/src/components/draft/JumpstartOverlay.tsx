import { useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import { getCdnArtCropUrl } from '@/utils/cardImages'
import styles from './JumpstartOverlay.module.css'

/** Pack legality, offers and deck assembly all come from the server. */
export function JumpstartOverlay() {
  const lobby = useGameStore((s) => s.lobbyState)
  const pick = useGameStore((s) => s.pickJumpstartPack)
  const leave = useGameStore((s) => s.leaveLobby)
  const error = useGameStore((s) => s.lastError)
  const [preview, setPreview] = useState<string | null>(null)
  const state = lobby?.jumpstart
  if (!state) return null
  const chosen = state.offers.find((offer) => offer.id === preview)
  return (
    <div className={styles.overlay}>
      <main className={styles.content}>
        <header className={styles.header}>
          <span className={styles.eyebrow}>JUMPSTART · TWO PACKS, ONE DECK</span>
          <button className={styles.leave} onClick={leave}>Leave lobby</button>
        </header>
        <h1>{state.offers.length ? `Choose your ${state.pickNumber === 1 ? 'first' : 'second'} theme` : 'Your deck is ready'}</h1>
        <p className={styles.intro}>
          {state.offers.length
            ? 'Pick a 20-card themed pack. Two packs combine into your deck, with all the lands you need.'
            : 'Waiting for the other players to choose their packs.'}
        </p>
        {state.selectedPacks.length > 0 && <p className={styles.selected}>Chosen: {state.selectedPacks.join(' + ')}</p>}
        {error && <p role="alert">{error.message}</p>}
        <div className={styles.packs}>
          {state.offers.map((offer) => {
            const face = offer.cards.find((card) => card.rarity === 'RARE' || card.rarity === 'MYTHIC') ?? offer.cards[0]
            return <article className={styles.pack} key={`${state.pickNumber}-${offer.id}`}>
              {face?.imageUri && <img src={getCdnArtCropUrl(face.imageUri) ?? face.imageUri} alt={face.name} className={styles.art} />}
              <div className={styles.packBody}>
                <h2>{offer.theme}</h2>
                <p>20 cards · lands included</p>
                <button className={styles.choose} onClick={() => pick(offer.id, state.pickNumber)}>Choose {offer.theme}</button>
                <button className={styles.preview} onClick={() => setPreview(preview === offer.id ? null : offer.id)}>View pack list</button>
              </div>
            </article>
          })}
        </div>
        {chosen && <section className={styles.list} aria-label={`${chosen.theme} pack list`}>
          <h2>{chosen.id}</h2>
          <ul>{Array.from(new Set(chosen.cards.map((card) => card.name))).map((name) =>
            <li key={name}>{chosen.cards.filter((card) => card.name === name).length} × {name}</li>,
          )}</ul>
        </section>}
        <p className={styles.note}>Exact published Jumpstart packs. Themes with unimplemented cards are unavailable.</p>
      </main>
    </div>
  )
}
