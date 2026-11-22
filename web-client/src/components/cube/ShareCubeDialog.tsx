/**
 * Share a cube with friends.
 *
 * A link has to open on someone else's machine, so only a cube saved to an account can have one; the
 * link always shows the owner's latest *saved* version, which is why unsaved edits are saved first
 * rather than silently left out. Anyone can still copy the cube as a text list — the format the
 * editor's Paste tab reads back — so a guest is never stuck.
 */
import { useState } from 'react'
import { createPortal } from 'react-dom'
import { sharedCubePath } from '@/api/account'
import { LoginModal } from '@/components/auth/LoginModal'
import { useAuthStore } from '@/store/authStore'
import type { UnifiedCube } from '@/store/useUnifiedCubes'
import styles from './CubeEditor.module.css'

interface ShareCubeDialogProps {
  /** The cube as last saved, or null when it has never been saved. */
  readonly saved: UnifiedCube | null
  /** The editor holds changes the saved copy doesn't have. */
  readonly dirty: boolean
  readonly isLoggedIn: boolean
  /** Plain-text list of the cube as it stands. */
  readonly text: string
  /** Save the editor's current state; resolves to the saved cube, or null if saving failed. */
  readonly onSave?: () => Promise<UnifiedCube | null>
  readonly share: (cube: UnifiedCube) => Promise<string>
  readonly unshare: (cube: UnifiedCube) => Promise<void>
  readonly onClose: () => void
}

export function ShareCubeDialog({
  saved,
  dirty,
  isLoggedIn,
  text,
  onSave,
  share,
  unshare,
  onClose,
}: ShareCubeDialogProps) {
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const [code, setCode] = useState<string | null>(saved?.shareCode ?? null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState<'link' | 'text' | null>(null)
  const [loginOpen, setLoginOpen] = useState(false)

  const url = code ? `${window.location.origin}${sharedCubePath(code)}` : null
  const inCloud = saved?.cloudId != null

  async function copy(value: string, what: 'link' | 'text') {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(what)
      window.setTimeout(() => setCopied(null), 1800)
    } catch {
      setError('Your browser blocked the clipboard — select the text and copy it yourself.')
    }
  }

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setBusy(false)
    }
  }

  /** Save first when the link would otherwise show an older version, then create the link. */
  const createLink = () =>
    run(async () => {
      let target = saved
      if ((dirty || !inCloud) && onSave) target = await onSave()
      if (!target || target.cloudId == null) throw new Error('Save the cube to your account first')
      const shareCode = await share(target)
      setCode(shareCode)
      await copy(`${window.location.origin}${sharedCubePath(shareCode)}`, 'link')
    })

  const revoke = () =>
    run(async () => {
      if (saved) await unshare(saved)
      setCode(null)
    })

  return createPortal(
    <>
      <div className={styles.shareBackdrop} onClick={onClose} />
      <div className={styles.shareDialog} role="dialog" aria-modal="true" aria-labelledby="share-cube-title">
        <h2 id="share-cube-title" className={styles.shareTitle}>Share this cube</h2>

        {!accountsEnabled ? (
          <p className={styles.shareBody}>
            This server doesn’t keep accounts, so cubes can’t be shared by link here. Copy the list instead —
            a friend can paste it into their own cube editor.
          </p>
        ) : !isLoggedIn ? (
          <>
            <p className={styles.shareBody}>
              A share link opens on your friend’s machine, so the cube has to live in your account. Sign in and
              it is saved there — your cubes on this device come along.
            </p>
            <button type="button" className={styles.primaryButton} onClick={() => setLoginOpen(true)}>
              Sign in to share by link
            </button>
          </>
        ) : url ? (
          <>
            <p className={styles.shareBody}>
              Anyone with the link can view this cube, save a copy and play it. It always shows your latest saved
              version{dirty ? ' — save your changes for them to appear' : ''}.
            </p>
            <div className={styles.shareLinkRow}>
              <input className={styles.input} value={url} readOnly aria-label="Share link" onFocus={(e) => e.target.select()} />
              <button type="button" className={styles.primaryButton} onClick={() => void copy(url, 'link')}>
                {copied === 'link' ? 'Copied' : 'Copy link'}
              </button>
            </div>
            <button type="button" className={styles.linkButton} disabled={busy} onClick={() => void revoke()}>
              Stop sharing — the link stops working
            </button>
          </>
        ) : (
          <>
            <p className={styles.shareBody}>
              Create a link anyone can open to view this cube, save a copy and play it. It always shows your latest
              saved version, and you can turn it off at any time.
            </p>
            <button type="button" className={styles.primaryButton} disabled={busy} onClick={() => void createLink()}>
              {busy ? 'Creating link…' : dirty || !inCloud ? 'Save and create link' : 'Create link'}
            </button>
          </>
        )}

        {error && <p className={styles.error}>{error}</p>}

        <div className={styles.shareFooter}>
          <button type="button" className={styles.secondaryButton} onClick={() => void copy(text, 'text')}>
            {copied === 'text' ? 'List copied' : 'Copy as text list'}
          </button>
          <button type="button" className={styles.secondaryButton} onClick={onClose}>
            Done
          </button>
        </div>
      </div>
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </>,
    document.body,
  )
}
