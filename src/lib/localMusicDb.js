// Local device music database. Files never leave the browser — stored as
// Blobs in IndexedDB, keyed by a generated id. Survives refreshes (IndexedDB
// is persistent, unlike sessionStorage/memory). This backs the "Local Files"
// / "Files on this device" UI, entirely separate from the cloud (Firestore)
// library used for YouTube-backed tracks — nothing here ever touches a
// server.
import { useCallback, useEffect, useState } from 'react'

const DB_NAME = 'spotifusion-local'
const STORE = 'songs'

// Generic music-note cover for tracks with no embedded artwork, so
// TrackRow/PlayerBar (which both expect a `thumbnail` URL) render fine.
export const LOCAL_TRACK_THUMB = `data:image/svg+xml;utf8,${encodeURIComponent(
  "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'>" +
    "<rect width='200' height='200' fill='#333'/>" +
    "<text x='100' y='125' font-size='90' text-anchor='middle' fill='#b3b3b3'>&#9834;</text>" +
    '</svg>'
)}`

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => {
      req.result.createObjectStore(STORE, { keyPath: 'id' })
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function withStore(mode, fn) {
  return openDb().then(
    (db) =>
      new Promise((resolve, reject) => {
        const tx = db.transaction(STORE, mode)
        const store = tx.objectStore(STORE)
        const result = fn(store)
        tx.oncomplete = () => {
          db.close()
          resolve(result?.result ?? result)
        }
        tx.onerror = () => {
          db.close()
          reject(tx.error)
        }
      })
  )
}

function secondsToMinSec(totalSeconds) {
  const s = Math.max(0, Math.round(totalSeconds || 0))
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

function readDuration(file) {
  return new Promise((resolve) => {
    const audio = document.createElement('audio')
    audio.preload = 'metadata'
    const url = URL.createObjectURL(file)
    audio.src = url
    audio.onloadedmetadata = () => {
      resolve(audio.duration)
      URL.revokeObjectURL(url)
    }
    audio.onerror = () => {
      resolve(0)
      URL.revokeObjectURL(url)
    }
  })
}

function cleanTitle(filename) {
  return filename.replace(/\.[^/.]+$/, '').replace(/[_-]+/g, ' ').trim() || 'Untitled'
}

// Strip the (large) blob out before handing a record to UI code — callers
// get metadata only; use getLocalSongBlob() to fetch the actual audio.
function toPublicShape(record) {
  if (!record) return null
  const { blob, durationSeconds, ...meta } = record
  return {
    ...meta,
    thumbnail: meta.artwork, // TrackRow/PlayerBar read `thumbnail`; spec schema calls it `artwork`
    duration: secondsToMinSec(durationSeconds),
    durationSeconds,
  }
}

/**
 * Add one File (from an <input type="file">, drag-drop, or the File System
 * Access API) to the local library. Accepts mp3/wav/ogg/m4a — anything the
 * browser reports as audio/*. Returns the created song's public metadata.
 */
export async function addLocalSong(file) {
  if (!file.type.startsWith('audio/')) {
    throw new Error(`"${file.name}" doesn't look like an audio file.`)
  }
  const durationSeconds = await readDuration(file)
  const id = `local-${crypto.randomUUID()}`
  const record = {
    id,
    title: cleanTitle(file.name),
    artist: 'Unknown artist',
    album: null,
    artwork: LOCAL_TRACK_THUMB,
    durationSeconds,
    source: 'local',
    fileName: file.name,
    fileSize: file.size,
    favorite: false,
    addedAt: Date.now(),
    blob: file,
  }
  await withStore('readwrite', (store) => store.put(record))
  return toPublicShape(record)
}

/** Convenience for multi-file pickers/drops. */
export async function addLocalSongs(fileList) {
  const results = []
  for (const file of Array.from(fileList)) {
    try {
      results.push(await addLocalSong(file))
    } catch (err) {
      console.warn(err.message)
    }
  }
  return results
}

export async function getLocalSong(id) {
  const record = await withStore('readonly', (store) => store.get(id))
  return toPublicShape(record)
}

export async function getAllLocalSongs() {
  const records = await withStore('readonly', (store) => store.getAll())
  return records.sort((a, b) => b.addedAt - a.addedAt).map(toPublicShape)
}

export async function searchLocalSongs(query) {
  const q = query.trim().toLowerCase()
  if (!q) return getAllLocalSongs()
  const all = await getAllLocalSongs()
  return all.filter(
    (s) => s.title.toLowerCase().includes(q) || s.artist.toLowerCase().includes(q)
  )
}

/** Fetch the actual playable audio Blob for a song (kept separate from
 * metadata so lists don't have to load every audio file into memory). */
export async function getLocalSongBlob(id) {
  const record = await withStore('readonly', (store) => store.get(id))
  return record?.blob || null
}

/** Patch arbitrary fields (e.g. { favorite: true }, or a corrected title/artist). */
export async function updateLocalSong(id, patch) {
  const existing = await withStore('readonly', (store) => store.get(id))
  if (!existing) return null
  const updated = { ...existing, ...patch, id } // id is never overwritable via patch
  await withStore('readwrite', (store) => store.put(updated))
  return toPublicShape(updated)
}

export async function deleteLocalSong(id) {
  await withStore('readwrite', (store) => store.delete(id))
}

export async function clearLocalSongs() {
  await withStore('readwrite', (store) => store.clear())
}

/** Total bytes used by all locally stored audio files (for Settings → Storage). */
export async function getLocalStorageStats() {
  const records = await withStore('readonly', (store) => store.getAll())
  return {
    count: records.length,
    totalBytes: records.reduce((sum, r) => sum + (r.fileSize || 0), 0),
  }
}

// ---- React hooks -----------------------------------------------------

/** List of local songs + a refresh() to call after add/delete/update. */
export function useLocalSongs() {
  const [songs, setSongs] = useState([])
  const refresh = useCallback(() => {
    getAllLocalSongs().then(setSongs)
  }, [])
  useEffect(() => {
    refresh()
  }, [refresh])
  return [songs, refresh]
}

export function useLocalStorageStats() {
  const [stats, setStats] = useState({ count: 0, totalBytes: 0 })
  const refresh = useCallback(() => {
    getLocalStorageStats().then(setStats)
  }, [])
  useEffect(() => {
    refresh()
  }, [refresh])
  return [stats, refresh]
}
