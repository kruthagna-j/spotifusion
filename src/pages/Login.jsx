import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Chrome, Music2, ShieldCheck, ArrowRight } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'

export default function Login() {
  const { user, profile, signIn } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (user) navigate(profile?.onboardingComplete ? '/' : '/onboarding', { replace: true })
  }, [user, profile, navigate])

  async function handleGoogle() {
    setBusy(true); setError('')
    try { await signIn() }
    catch (err) {
      console.error(err)
      if (err?.code === 'auth/popup-closed-by-user') setError('Sign-in was cancelled. Try again when you are ready.')
      else if (err?.code === 'auth/unauthorized-domain') setError('This website is not authorized in Firebase Authentication.')
      else if (err?.code === 'auth/argument-error') setError('Google sign-in could not start. Please refresh and try again.')
      else setError(err?.message || 'Google sign-in failed. Please try again.')
    }
    finally { setBusy(false) }
  }

  return <div className="sf-login-page">
    <div className="sf-login-glow" aria-hidden="true" />
    <div className="sf-login-card">
      <div className="sf-login-brand"><span className="sf-login-logo"><Music2 size={26}/></span><span>Spotifusion</span></div>
      <p className="text-xs text-brand uppercase tracking-[.25em] font-black mt-10">Music without limits</p>
      <h1 className="text-4xl sm:text-5xl font-black mt-3 leading-tight">Your music.<br/><span className="text-brand">Your way.</span></h1>
      <p className="text-sm text-text-muted mt-4 leading-6">Sign in to sync your library, save favorites, personalize your music and keep your player experience connected.</p>

      <button onClick={handleGoogle} disabled={busy} className="sf-login-google">
        <Chrome size={19}/><span>{busy ? 'Connecting…' : 'Continue with Google'}</span><ArrowRight size={17} className="ml-auto"/>
      </button>
      {error && <div className="sf-login-error">{error}</div>}
      <div className="sf-login-safe"><ShieldCheck size={16}/><span>Authentication is handled securely by Firebase. Spotifusion never sees your Google password.</span></div>
      <button type="button" onClick={() => navigate('/')} className="sf-login-guest">Continue browsing as guest</button>
    </div>
  </div>
}
