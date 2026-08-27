import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { onAuthStateChanged } from 'firebase/auth'
import { auth, signInWithGoogle, completeGoogleRedirect, signOut as fbSignOut, ensureUserProfile, getUserPreferences } from '@/lib/firebase'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const signInInFlight = useRef(false)

  useEffect(() => {
    let alive = true
    // Complete an outstanding mobile redirect before waiting for the auth
    // observer. Errors are surfaced through the login page instead of causing
    // a blank/white application.
    completeGoogleRedirect().catch(() => null)

    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (!alive) return
      setUser(currentUser)
      if (currentUser) {
        try {
          await ensureUserProfile(currentUser)
          const data = await getUserPreferences(currentUser.uid)
          if (alive) setProfile(data)
        } catch (err) {
          console.error('[Spotifusion] Could not load user profile:', err)
          if (alive) setProfile(null)
        }
      } else {
        setProfile(null)
      }
      if (alive) setLoading(false)
    })
    return () => { alive = false; unsubscribe() }
  }, [])

  async function signIn() {
    if (signInInFlight.current) return
    signInInFlight.current = true
    try {
      return await signInWithGoogle()
    } finally {
      // Popup resolves here. Redirect intentionally leaves the page and the
      // next page load completes the Firebase redirect result.
      signInInFlight.current = false
    }
  }

  async function signOut() {
    await fbSignOut()
    setProfile(null)
  }

  if (loading) {
    return <div className="h-screen flex items-center justify-center bg-bg text-text-muted text-sm">Loading Spotifusion…</div>
  }

  return <AuthContext.Provider value={{ user, profile, setProfile, signIn, signOut, authLoading: loading }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
