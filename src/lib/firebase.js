import { initializeApp } from 'firebase/app'
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  signOut as fbSignOut,
  onAuthStateChanged,
  updateProfile,
} from 'firebase/auth'
import { getFirestore, doc, setDoc, getDoc, updateDoc, serverTimestamp } from 'firebase/firestore'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

export const app = initializeApp(firebaseConfig)
export const auth = getAuth(app)
export const db = getFirestore(app)

export const googleProvider = new GoogleAuthProvider()
googleProvider.setCustomParameters({ prompt: 'select_account' })

function isMobileBrowser() {
  if (typeof navigator === 'undefined') return false
  return /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent) ||
    (navigator.maxTouchPoints > 1 && window.matchMedia?.('(max-width: 767px)').matches)
}

export async function signInWithGoogle() {
  // Redirect is much more reliable than a popup in mobile browsers and
  // embedded WebViews. Desktop keeps the fast popup experience.
  if (isMobileBrowser()) return signInWithRedirect(auth, googleProvider)
  return signInWithPopup(auth, googleProvider)
}

export async function completeGoogleRedirect() {
  try {
    return await getRedirectResult(auth)
  } catch (error) {
    console.error('[Spotifusion] Google redirect sign-in failed:', error)
    throw error
  }
}

export function signOut() { return fbSignOut(auth) }
export function watchAuth(callback) { return onAuthStateChanged(auth, callback) }

export async function ensureUserProfile(user) {
  const ref = doc(db, 'users', user.uid)
  const snap = await getDoc(ref)
  if (!snap.exists()) {
    await setDoc(ref, {
      displayName: user.displayName || 'Spotifusion user',
      email: user.email || '',
      photoURL: user.photoURL || '',
      language: 'English',
      favoriteArtists: [],
      onboardingComplete: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  }
  return ref
}

export async function getUserPreferences(uid) {
  if (!uid) return null
  const snap = await getDoc(doc(db, 'users', uid))
  return snap.exists() ? snap.data() : null
}

export async function saveUserPreferences(uid, preferences) {
  if (!uid) throw new Error('You must be signed in.')
  await updateDoc(doc(db, 'users', uid), { ...preferences, updatedAt: serverTimestamp() })
  return preferences
}

export async function updateUserDisplayName(name) {
  if (!auth.currentUser) throw new Error('No signed-in user.')
  const clean = String(name || '').trim()
  if (!clean) throw new Error('Name cannot be empty.')
  await updateProfile(auth.currentUser, { displayName: clean })
  await setDoc(doc(db, 'users', auth.currentUser.uid), { displayName: clean, updatedAt: serverTimestamp() }, { merge: true })
}

export async function deleteCurrentUserAccount() {
  const { deleteUser } = await import('firebase/auth')
  if (!auth.currentUser) throw new Error('No signed-in user')
  return deleteUser(auth.currentUser)
}
