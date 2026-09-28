/**
 * The one index of keyboard shortcuts.
 *
 * Shortcuts were previously implemented in seven unrelated files (`useMultiplayerView.ts`,
 * `ZonePiles.tsx`, `CardPreview.tsx`, `useDfcHoverFlip.tsx`, `ReplayPage.tsx`, `ActionMenu.tsx`,
 * `DeckbuilderPage.tsx`) and documented nowhere — several were completely undiscoverable. This
 * list is what `/help` renders; adding a shortcut means adding a row here in the same change.
 */
export interface Shortcut {
  id: string
  /** Rendered as `<kbd>` chips; split on " / " for alternatives. */
  keys: string
  label: string
  /** Where the shortcut is live — a phrase, not a file path. */
  where: string
}

export const SHORTCUTS: readonly Shortcut[] = [
  {
    id: 'help',
    keys: '?',
    label: 'Open or close this list of shortcuts',
    where: 'In a game',
  },
  {
    id: 'pass',
    keys: 'Space',
    label: 'Pass priority — the same as the Pass button, while it is enabled',
    where: 'In a game',
  },
  {
    id: 'undo',
    keys: 'Ctrl+Z',
    label: 'Undo, or take back your last decision this turn (a human opponent is asked first)',
    where: 'In a game — the ⟲ button beside Auto',
  },
  {
    id: 'confirm-prompt',
    keys: 'Enter',
    label: 'Confirm the prompt on screen (card choices, scry, ordering, options)',
    where: 'In a game, while a prompt is open',
  },
  {
    id: 'yes-no',
    keys: 'Y / N',
    label: 'Answer a yes-or-no prompt',
    where: 'In a game, while a yes-or-no prompt is open',
  },
  {
    id: 'pick-option',
    keys: '1 – 9',
    label: 'Pick an option, or send a single surveilled card to a pile',
    where: 'In a game, while an option or pile prompt is open',
  },
  {
    id: 'peek-board',
    keys: 'Hold Tab',
    label: 'Hide the open prompt to look at the board; your choices so far are kept',
    where: 'In a game, while a prompt is open',
  },
  {
    id: 'lanes-move',
    keys: '↑ / ↓ / ← / →',
    label: 'With a card under the pointer: move it to the other row (↑ ↓) or along its row (← →)',
    where: 'Scry, surveil and other "this pile or that pile" prompts',
  },
  {
    id: 'opponent-boards',
    keys: '1 – 9',
    label: 'Focus an opponent’s board',
    where: 'Multiplayer games (3+ players)',
  },
  {
    id: 'overview',
    keys: '0',
    label: 'Toggle the table overview — every board side by side',
    where: 'Multiplayer games, desktop and landscape tablet',
  },
  {
    id: 'escape',
    keys: 'Esc',
    label: 'Cancel: unpin the camera, close a modal or zone browser, leave a replay',
    where: 'Everywhere',
  },
  {
    id: 'strip-swipe',
    keys: 'Swipe ← / →',
    label: 'Slide to the previous or next opponent’s board',
    where: 'Multiplayer games, on the opponent board (focused camera)',
  },
  {
    id: 'deck-browser',
    keys: 'D',
    label: 'Open or close the deck browser',
    where: 'In a game',
  },
  {
    id: 'flip-dfc',
    keys: 'F',
    label: 'Flip a double-faced card while previewing it',
    where: 'Any card preview — hand, battlefield, deckbuilder',
  },
  {
    id: 'replay-frame',
    keys: '← / →',
    label: 'Step one frame back or forward',
    where: 'Replay viewer',
  },
  {
    id: 'replay-play',
    keys: 'Space',
    label: 'Play / pause',
    where: 'Replay viewer',
  },
  {
    id: 'submit',
    keys: 'Enter',
    label: 'Submit the focused field or dialog',
    where: 'Name entry, join code, deck name, search',
  },
  {
    id: 'deckbuilder-remove',
    keys: 'Right-click / Shift-click',
    label: 'Remove one copy of a card',
    where: 'Deckbuilder',
  },
  {
    id: 'stack-yield-menu',
    keys: 'Right-click / long-press',
    label: 'Open the yield menu for an ability on the stack',
    where: 'In a game, on a stack item',
  },
]

export function shortcutById(id: string): Shortcut | undefined {
  return SHORTCUTS.find((s) => s.id === id)
}
