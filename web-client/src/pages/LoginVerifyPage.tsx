/**
 * Landing page for a magic-link click (`/login/verify?token=…`). Exchanges the token for a session,
 * stores it, and bounces home. Shown only briefly; renders an error with a retry path on failure.
 */
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { verifyLogin } from '@/api/account'
import { useAuthStore } from '@/store/authStore'
import { AccountPage, MessageCard, accountStyles as a } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'

export function LoginVerifyPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const [error, setError] = useState<string | null>(null)
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true
    const token = searchParams.get('token')
    if (!token) {
      setError('Missing sign-in token.')
      return
    }
    verifyLogin(token)
      .then((login) => {
        setSession(login)
        navigate('/', { replace: true })
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Sign-in failed.'))
  }, [searchParams, navigate, setSession])

  return (
    <AccountPage title="Sign in" width="narrow">
      <MessageCard center>
        {error ? (
          <>
            <h1 className={p.h1}>Sign-in failed</h1>
            <p className={a.muted}>{error}</p>
            <p className={a.muted}>Request a fresh link and use it soon — each one works only once.</p>
            <button type="button" className={p.buttonPrimary} onClick={() => navigate('/', { replace: true })}>
              Back home
            </button>
          </>
        ) : (
          <>
            <span className={a.spinner} aria-hidden />
            <h1 className={p.h1}>Signing you in…</h1>
            <p className={a.muted}>One moment.</p>
          </>
        )}
      </MessageCard>
    </AccountPage>
  )
}
