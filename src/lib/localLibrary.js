// Local device audio library. Files never leave the browser — stored as
// Blobs in IndexedDB, keyed by a generated id. This is what backs the
// "Files on this device" section, separate from the cloud (Firestore)
// library used for YouTube-backed tracks.
import { useCallback, useEffect, useState } from 'react'

const DB_NAME = 'spotifusion-local'
const STORE = 'tracks'

// Generic music-note cover so local tracks render fine in TrackRow/PlayerBar,
// which both expect a `thumbnail` URL.
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

function secondsToIsoDuration(totalSeconds) {
  const s = Math.max(0, Math.round(totalSeconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  let iso = 'PT'
  if (h) iso += `${h}H`
  if (m) iso += `${m}M`
  iso += `${sec}S`
  return iso
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

// Adds one or more File objects (from an <input type="file"> or drop event)
// to the local library. Returns the newly created track metadata.
export async function addLocalFiles(fileList) {
  const db = await openDb()
  const added = []
  for (const file of Array.from(fileList)) {
    if (!file.type.startsWith('audio/')) continue
    const duration = await readDuration(file)
    const id = `local-${crypto.randomUUID()}`
    const track = {
      id,
      title: cleanTitle(file.name),
      artist: 'Files on this device',
      thumbnail: LOCAL_TRACK_THUMB,
      duration: secondsToIsoDuration(duration),
      source: 'local',
      addedAt: Date.now(),
    }
    await new Promise((resolve, reject) => {
      const tx = db.transaction(STORE, 'readwrite')
      tx.objectStore(STORE).put({ ...track, blob: file })
      tx.oncomplete = resolve
      tx.onerror = () => reject(tx.error)
    })
    added.push(track)
  }
  db.close()
  return added
}

export async function getAllLocalTracks() {
  const db = await openDb()
  const tracks = await new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).getAll()
    req.onsuccess = () =>
      resolve(
        req.result
          .sort((a, b) => b.addedAt - a.addedAt)
          .map(({ blob, ...meta }) => meta)
      )
    req.onerror = () => reject(req.error)
  })
  db.close()
  return tracks
}

export async function getLocalTrackBlob(id) {
  const db = await openDb()
  const blob = await new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).get(id)
    req.onsuccess = () => resolve(req.result?.blob || null)
    req.onerror = () => reject(req.error)
  })
  db.close()
  return blob
}

export async function deleteLocalTrack(id) {
  const db = await openDb()
  await new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).delete(id)
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
  db.close()
}

// React hook: list of local tracks + a refresh() to call after add/delete.
export function useLocalTracks() {
  const [tracks, setTracks] = useState([])
  const refresh = useCallback(() => {
    getAllLocalTracks().then(setTracks)
  }, [])
  useEffect(() => {
    refresh()
  }, [refresh])
  return [tracks, refresh]
}
