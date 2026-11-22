import { expect, type Page } from '@playwright/test'

/**
 * Driving the landing screen's mode catalogue.
 *
 * These live in one place because the landing screen has now been restructured three times — the
 * specs first typed into a placeholder that had been renamed, then clicked `mode-preset-*` cards,
 * then walked a three-question wizard — and each time every tournament spec had to be edited. The
 * next restructure should be one edit here.
 *
 * The catalogue (`components/ui/PlayHub.tsx`) is a grid of named modes; clicking one opens a launch
 * panel with its options defaulted, and the panel's Play button creates the lobby. A game against
 * the AI skips the lobby once its seats are filled, so {@link createLobby} — which waits for an invite
 * code — is for games with people, and {@link launchVsAi} is for the rest.
 *
 * The open mode is also the URL (`/play/<slug>`), so a spec can `page.goto('/play/draft')`.
 */

export type Mode =
  | 'constructed' | 'jump-in' | 'random' | 'momir' | 'draft' | 'sealed'
  | 'free-for-all' | 'two-headed-giant' | 'team-vs-team' | 'commander'
export type PlayWith = 'ai' | 'friends' | 'public'
/** With people, Constructed and Jump In choose between one opponent and a group bracket. */
export type HumanTable = 'one-v-one' | 'bracket'

/** The placeholder on the join field, so specs don't each hard-code the copy. */
export const JOIN_PLACEHOLDER = 'Invite code'

/** Where a multiplayer table's decks come from (the "Decks from" chips, by label). */
export type DecksFrom = 'Your decks' | 'Jump In' | 'Sealed' | 'Draft'

export interface ModeChoice {
  mode: Mode
  playWith: PlayWith
  table?: HumanTable
  decksFrom?: DecksFrom
}

/** Wait for the landing screen to be interactive (the catalogue rendered). */
export async function waitForHome(page: Page): Promise<void> {
  await expect(page.getByTestId('mode-draft')).toBeVisible({ timeout: 10000 })
}

/** Enter a name and land on the catalogue. */
export async function enterName(page: Page, name: string): Promise<void> {
  await page.goto('/')
  await page.getByPlaceholder('Your name').fill(name)
  await page.getByRole('button', { name: 'Continue' }).click()
  await waitForHome(page)
}

/** Open a mode's launch panel and set who to play with. Remembered options are overwritten. */
async function configure(page: Page, choice: ModeChoice): Promise<void> {
  await page.getByTestId(`mode-${choice.mode}`).click()
  await expect(page.getByTestId('launch-panel')).toBeVisible()
  await page.getByTestId(`play-with-${choice.playWith}`).click()
  if (choice.table) {
    await page.getByTestId(`human-table-${choice.table === 'one-v-one' ? 'one_v_one' : 'bracket'}`).click()
  }
  if (choice.decksFrom) {
    await page.getByTestId('launch-panel').getByRole('button', { name: choice.decksFrom, exact: true }).click()
  }
}

/** Create a lobby for people to join. Returns once the invite code is on screen. */
export async function createLobby(page: Page, choice: ModeChoice): Promise<string> {
  await configure(page, choice)
  await page.getByTestId('launch-play').click()
  await expect(page.getByTestId('invite-code')).toBeVisible({ timeout: 10000 })
  const lobbyId = await page.getByTestId('invite-code').textContent() ?? ''
  expect(lobbyId).toBeTruthy()
  return lobbyId
}

/** Launch a game against the AI. It starts on its own once every AI seat is filled. */
export async function launchVsAi(page: Page, mode: Mode, options: { decksFrom?: DecksFrom } = {}): Promise<void> {
  await configure(page, { mode, playWith: 'ai', ...options })
  await page.getByTestId('launch-play').click()
}

/**
 * Launch a saved setup from the rail above the catalogue.
 *
 * The rail only exists once something has been played — a fresh browser profile sees the
 * catalogue and nothing else — so a spec that wants this has to create and start a lobby first.
 * `name` is the setup's name lower-cased and hyphenated; the auto-captured slot is `'last'`.
 */
export async function launchSetup(page: Page, name: string): Promise<string> {
  await page.getByTestId(`setup-chip-${name}`).click()
  await expect(page.getByTestId('invite-code')).toBeVisible({ timeout: 10000 })
  return await page.getByTestId('invite-code').textContent() ?? ''
}

/** Save the current lobby as a named setup, from the host's ★ button. */
export async function saveSetup(page: Page, name: string): Promise<void> {
  await page.getByTestId('save-setup').click()
  await page.getByLabel('Setup name').fill(name)
  await page.getByTestId('confirm-save-setup').click()
}

/** Join an existing lobby by code from the landing screen. */
export async function joinLobby(page: Page, lobbyId: string): Promise<void> {
  await page.getByPlaceholder(JOIN_PLACEHOLDER).fill(lobbyId)
  // By test id: the open-lobbies list beside the catalogue has Join buttons of its own.
  await page.getByTestId('join-code-submit').click()
  await expect(page.getByTestId('invite-code')).toBeVisible({ timeout: 10000 })
}

/** A group sealed bracket — what the tournament specs all want. */
export const GROUP_SEALED: ModeChoice = { mode: 'sealed', playWith: 'friends' }

/** A group booster draft bracket. */
export const GROUP_DRAFT: ModeChoice = { mode: 'draft', playWith: 'friends' }
