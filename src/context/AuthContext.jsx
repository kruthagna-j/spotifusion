import { createContext, useContext, useEffect, useState } from 'react'
import { auth, watchAuth, signInWithGoogle, signOut, ensureUserProfile } from '@/lib/firebase'
import { getRedirectResult } from 'firebase/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(undefined) // undefined = loading, null = signed out
  const [error, setError] = useState(null)

  useEffect(() => {
    // Completes sign-in when we used signInWithRedirect (Android WebView path)
    getRedirectResult(auth).catch((err) => setError(err.message))

    const unsub = watchAuth(async (u) => {
      if (u) {
        await ensureUserProfile(u)
      }
      setUser(u)
    })
    return unsub
  }, [])

  const value = {
    user,
    loading: user === undefined,
    error,
    signIn: () => signInWithGoogle().catch((err) => setError(err.message)),
    signOut,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
