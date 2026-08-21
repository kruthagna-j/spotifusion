// Firestore-backed "Your Library": playlists, liked songs, recently played.
// Schema:
//   users/{uid}
//   users/{uid}/likedSongs/{videoId}         -> track object + likedAt
//   users/{uid}/recentlyPlayed/{videoId}     -> track object + playedAt
//   users/{uid}/playlists/{playlistId}       -> { name, description, coverUrl, trackIds: [videoId,...], updatedAt }
import { db } from './firebase'
import {
  collection,
  doc,
  setDoc,
  deleteDoc,
  getDocs,
  query,
  orderBy,
  limit,
  serverTimestamp,
  arrayUnion,
  arrayRemove,
  onSnapshot,
} from 'firebase/firestore'

const uidPath = (uid) => `users/${uid}`

// ---------- User settings (bring-your-own YouTube API key) ----------
export function watchUserSettings(uid, callback) {
  return onSnapshot(doc(db, uidPath(uid)), (snap) => callback(snap.exists() ? snap.data() : {}))
}

export function setYoutubeApiKey(uid, apiKey) {
  return setDoc(doc(db, uidPath(uid)), { youtubeApiKey: apiKey || null }, { merge: true })
}

// ---------- Liked songs ----------
export function likeSong(uid, track) {
  const ref = doc(db, `${uidPath(uid)}/likedSongs/${track.id}`)
  return setDoc(ref, { ...track, likedAt: serverTimestamp() })
}

export function unlikeSong(uid, videoId) {
  return deleteDoc(doc(db, `${uidPath(uid)}/likedSongs/${videoId}`))
}

export function watchLikedSongs(uid, callback) {
  const q = query(collection(db, `${uidPath(uid)}/likedSongs`), orderBy('likedAt', 'desc'))
  return onSnapshot(q, (snap) => callback(snap.docs.map((d) => d.data())))
}

// ---------- Recently played ----------
export function recordPlay(uid, track) {
  const ref = doc(db, `${uidPath(uid)}/recentlyPlayed/${track.id}`)
  return setDoc(ref, { ...track, playedAt: serverTimestamp() })
}

export function watchRecentlyPlayed(uid, callback, max = 12) {
  const q = query(
    collection(db, `${uidPath(uid)}/recentlyPlayed`),
    orderBy('playedAt', 'desc'),
    limit(max)
  )
  return onSnapshot(q, (snap) => callback(snap.docs.map((d) => d.data())))
}

// ---------- Playlists ----------
export function watchPlaylists(uid, callback) {
  const q = query(collection(db, `${uidPath(uid)}/playlists`), orderBy('updatedAt', 'desc'))
  return onSnapshot(q, (snap) =>
    callback(snap.docs.map((d) => ({ id: d.id, ...d.data() })))
  )
}

export async function createPlaylist(uid, name, description = '') {
  const ref = doc(collection(db, `${uidPath(uid)}/playlists`))
  await setDoc(ref, {
    name,
    description,
    coverUrl: null,
    trackIds: [],
    tracks: {},
    updatedAt: serverTimestamp(),
  })
  return ref.id
}

export function deletePlaylist(uid, playlistId) {
  return deleteDoc(doc(db, `${uidPath(uid)}/playlists/${playlistId}`))
}

export function addTrackToPlaylist(uid, playlistId, track) {
  const ref = doc(db, `${uidPath(uid)}/playlists/${playlistId}`)
  return setDoc(
    ref,
    {
      trackIds: arrayUnion(track.id),
      [`tracks.${track.id}`]: track,
      updatedAt: serverTimestamp(),
    },
    { merge: true }
  )
}

export function removeTrackFromPlaylist(uid, playlistId, trackId) {
  const ref = doc(db, `${uidPath(uid)}/playlists/${playlistId}`)
  return setDoc(
    ref,
    { trackIds: arrayRemove(trackId), updatedAt: serverTimestamp() },
    { merge: true }
  )
}

// Deletes all of a user's Firestore data (profile doc + every subcollection).
// Call this before deleteCurrentUserAccount() when a user asks to delete
// their account, so no orphaned data is left behind.
export async function deleteAllUserData(uid) {
  const subcollections = ['likedSongs', 'recentlyPlayed', 'playlists']
  for (const name of subcollections) {
    const snap = await getDocs(collection(db, `${uidPath(uid)}/${name}`))
    await Promise.all(snap.docs.map((d) => deleteDoc(d.ref)))
  }
  await deleteDoc(doc(db, uidPath(uid)))
}
