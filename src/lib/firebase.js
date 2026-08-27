import { initializeApp } from 'firebase/app'
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut as fbSignOut,
  onAuthStateChanged,
} from 'firebase/auth'
import {
  getFirestore,
  doc,
  setDoc,
  getDoc,
  serverTimestamp,
} from 'firebase/firestore'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

export const app = initializeApp(firebaseConfig)

// Use Firebase's standard browser Auth initialization. The previous build
// manually called initializeAuth() without a popupRedirectResolver. That is
// unsafe for signInWithPopup()/reauthenticateWithPopup() and can surface
// auth/argument-error in production browsers. getAuth() wires the supported
// browser dependencies, including the popup/redirect resolver.
export const auth = getAuth(app)
export const db = getFirestore(app)

const googleProvider = new GoogleAuthProvider()
googleProvider.setCustomParameters({ prompt: 'select_account' })

// Google web authentication intentionally uses the popup flow. The previous
// WebView -> redirect fallback could lose Firebase's redirect state on mobile
// browsers and embedded WebViews, producing "missing initial state".
export async function signInWithGoogle() {
  return signInWithPopup(auth, googleProvider)
}

export function signOut() {
  return fbSignOut(auth)
}

export function watchAuth(callback) {
  return onAuthStateChanged(auth, callback)
}

// Ensure a /users/{uid} profile doc exists, matching Spotify's "your library"
// data model: liked songs, playlists, recently played all key off this doc.
export async function ensureUserProfile(user) {
  const ref = doc(db, 'users', user.uid)
  const snap = await getDoc(ref)
  if (!snap.exists()) {
    await setDoc(ref, {
      displayName: user.displayName,
      email: user.email,
      photoURL: user.photoURL,
      createdAt: serverTimestamp(),
    })
  }
  return ref
}

// Permanently deletes the signed-in user's auth account. Firestore data
// cleanup (playlists, likes, recently played) is handled separately by
// deleteUserData() in library.js — call both together from the UI.
export async function deleteCurrentUserAccount() {
  const { deleteUser } = await import('firebase/auth')
  if (!auth.currentUser) throw new Error('No signed-in user')
  return deleteUser(auth.currentUser)
}
