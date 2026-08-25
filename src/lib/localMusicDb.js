// Local device music database. Files never leave the browser — audio stays
// as Blobs in IndexedDB (folder-scanned files aren't even copied into a
// Blob until played — see getLocalSongBlob), and directory handles are
// stored directly in IndexedDB too (FileSystemDirectoryHandle is natively
// structured-cloneable), which is what lets a folder be "remembered" across
// sessions instead of re-picking it every visit. Nothing here ever touches
// a server.
import { useCallback, useEffect, useState } from 'react'

const DB_NAME = 'spotifusion-local'
const DB_VERSION = 2
const SONGS_STORE = 'songs'
const FOLDERS_STORE = 'folders'

// Extensions we explicitly support, per spec. `file.type` (MIME sniffing)
// is unreliable for some of these — .flac in particular is frequently
// reported as an empty string by the OS/browser — so scanning matches by
// extension, not just MIME type.
const SUPPORTED_EXTENSIONS = ['mp3', 'wav', 'm4a', 'aac', 'ogg', 'flac', 'webm']

// Generic music-note cover for tracks with no embedded artwork, so
// TrackRow/PlayerBar (which both expect a `thumbnail` URL) render fine.
export const LOCAL_TRACK_THUMB = `data:image/svg+xml;utf8,${encodeURIComponent(
  "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'>" +
    "<rect width='200' height='200' fill='#333'/>" +
    "<text x='100' y='125' font-size='90' text-anchor='middle' fill='#b3b3b3'>&#9834;</text>" +
    '</svg>'
)}`

export const SUPPORTS_FOLDER_ACCESS = typeof window !== 'undefined' && 'showDirectoryPicker' in window

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(SONGS_STORE)) {
        db.createObjectStore(SONGS_STORE, { keyPath: 'id' })
      }
      if (!db.objectStoreNames.contains(FOLDERS_STORE)) {
        db.createObjectStore(FOLDERS_STORE, { keyPath: 'id' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function withStore(storeName, mode, fn) {
  return openDb().then(
    (db) =>
      new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, mode)
        const store = tx.objectStore(storeName)
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

const withSongs = (mode, fn) => withStore(SONGS_STORE, mode, fn)
const withFolders = (mode, fn) => withStore(FOLDERS_STORE, mode, fn)

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

function fileExtension(filename) {
  const m = filename.match(/\.([a-zA-Z0-9]+)$/)
  return m ? m[1].toLowerCase() : ''
}

function isSupportedAudioFile(filename, mimeType) {
  if (mimeType?.startsWith('audio/')) return true
  return SUPPORTED_EXTENSIONS.includes(fileExtension(filename))
}

// Parses "01 - Artist Name - Song Name.mp3" / "Artist - Song.mp3" style
// filenames into a real title/artist guess instead of showing the whole
// filename verbatim, per spec. Falls back to the cleaned filename as the
// title when the pattern doesn't match.
function parseFilename(filename) {
  const base = filename.replace(/\.[^/.]+$/, '')
  const withoutTrackNumber = base.replace(/^\s*\d{1,3}[\s.\-_]+/, '')
  const parts = withoutTrackNumber.split(/\s*-\s*/).filter(Boolean)
  const clean = (s) => s.replace(/[_]+/g, ' ').trim()
  if (parts.length >= 2) {
    return { artist: clean(parts[0]), title: clean(parts.slice(1).join(' - ')) }
  }
  return { artist: 'Unknown artist', title: clean(withoutTrackNumber) || 'Untitled' }
}

// Strip the (large) blob out before handing a record to UI code — callers
// get metadata only; use getLocalSongBlob() to fetch the actual audio.
function toPublicShape(record) {
  if (!record) return null
  const { blob: _blob, fileHandle: _fileHandle, durationSeconds, ...meta } = record
  return {
    ...meta,
    thumbnail: meta.artwork, // TrackRow/PlayerBar read `thumbnail`; spec schema calls it `artwork`
    duration: secondsToMinSec(durationSeconds),
    durationSeconds,
  }
}

async function buildRecordFromFile(file, { folderId = null, relativePath = null } = {}) {
  const durationSeconds = await readDuration(file)
  const { artist, title } = parseFilename(file.name)
  // Stable ID for folder-scanned files (folderId + path) so rescanning the
  // same folder updates existing entries instead of duplicating them.
  // Individually-added files (no folder) keep a random ID.
  const id = folderId
    ? `local-f-${folderId}-${encodeURIComponent(relativePath)}`
    : `local-${crypto.randomUUID()}`
  return {
    id,
    title,
    artist,
    album: null,
    artwork: LOCAL_TRACK_THUMB,
    durationSeconds,
    source: 'local',
    fileName: file.name,
    fileSize: file.size,
    favorite: false,
    folderId,
    addedAt: Date.now(),
    blob: folderId ? undefined : file, // folder-scanned: re-fetched from the handle at play time instead of duplicated into a blob now
  }
}

/**
 * Add one File (from an <input type="file">, drag-drop, or a single pick)
 * to the local library. Returns the created song's public metadata.
 */
export async function addLocalSong(file) {
  if (!isSupportedAudioFile(file.name, file.type)) {
    throw new Error(`"${file.name}" isn't a supported audio file.`)
  }
  const record = await buildRecordFromFile(file)
  await withSongs('readwrite', (store) => store.put(record))
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
  const record = await withSongs('readonly', (store) => store.get(id))
  return toPublicShape(record)
}

export async function getAllLocalSongs() {
  const records = await withSongs('readonly', (store) => store.getAll())
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

/** Fetch the actual playable audio Blob for a song. Individually-added
 * files already have their blob stored; folder-scanned files re-fetch the
 * File from their saved FileSystemFileHandle on demand (avoids duplicating
 * every folder song into a full Blob copy just for having been scanned). */
export async function getLocalSongBlob(id) {
  const record = await withSongs('readonly', (store) => store.get(id))
  if (!record) return null
  if (record.blob) return record.blob
  if (record.fileHandle) {
    try {
      const perm = await record.fileHandle.queryPermission({ mode: 'read' })
      if (perm !== 'granted') return null // caller should prompt to reconnect the folder
      return await record.fileHandle.getFile()
    } catch {
      return null
    }
  }
  return null
}

/** Patch arbitrary fields (e.g. { favorite: true }, or a corrected title/artist). */
export async function updateLocalSong(id, patch) {
  const existing = await withSongs('readonly', (store) => store.get(id))
  if (!existing) return null
  const updated = { ...existing, ...patch, id } // id is never overwritable via patch
  await withSongs('readwrite', (store) => store.put(updated))
  return toPublicShape(updated)
}

export async function deleteLocalSong(id) {
  await withSongs('readwrite', (store) => store.delete(id))
}

export async function clearLocalSongs() {
  await withSongs('readwrite', (store) => store.clear())
  await withFolders('readwrite', (store) => store.clear())
}

/** Total bytes used by all locally stored audio files (for Settings → Storage). */
export async function getLocalStorageStats() {
  const records = await withSongs('readonly', (store) => store.getAll())
  return {
    count: records.length,
    totalBytes: records.reduce((sum, r) => sum + (r.fileSize || 0), 0),
  }
}

// ---- Folder scanning ---------------------------------------------------

/** Recursively walks a directory handle, calling onFile for every
 * supported audio file found (in any subfolder). onProgress(count) fires
 * as files are discovered, for a live "Found N songs" UI. */
async function walkDirectory(dirHandle, path, onFile, onProgress, count = { n: 0 }) {
  for await (const entry of dirHandle.values()) {
    const entryPath = path ? `${path}/${entry.name}` : entry.name
    if (entry.kind === 'file') {
      if (isSupportedAudioFile(entry.name)) {
        onFile(entry, entryPath)
        count.n += 1
        onProgress?.(count.n)
      }
    } else if (entry.kind === 'directory') {
      await walkDirectory(entry, entryPath, onFile, onProgress, count)
    }
  }
  return count.n
}

/**
 * Opens the folder picker, scans the chosen folder (and subfolders)
 * recursively for supported audio files, and remembers the folder handle
 * so it can be reconnected automatically (permission allowing) on future
 * visits instead of re-picking it every time.
 */
export async function addMusicFolder(onProgress) {
  const dirHandle = await window.showDirectoryPicker()
  const folderId = `folder-${crypto.randomUUID()}`

  await withFolders('readwrite', (store) =>
    store.put({ id: folderId, name: dirHandle.name, handle: dirHandle, addedAt: Date.now() })
  )

  await scanFolder(folderId, dirHandle, onProgress)
  return folderId
}

/** (Re)scans a single folder — used both right after adding it and when
 * reconnecting on a later visit. Existing songs from this folder are
 * upserted (same stable id), not duplicated. */
export async function scanFolder(folderId, dirHandle, onProgress) {
  const puts = []
  await walkDirectory(
    dirHandle,
    '',
    (fileHandle, relativePath) => {
      puts.push(
        (async () => {
          const file = await fileHandle.getFile()
          const record = await buildRecordFromFile(file, { folderId, relativePath })
          record.fileHandle = fileHandle // re-fetch the File lazily at play time
          await withSongs('readwrite', (store) => store.put(record))
        })()
      )
    },
    onProgress
  )
  await Promise.all(puts)
  return puts.length
}

export async function getMusicFolders() {
  return withFolders('readonly', (store) => store.getAll())
}

/** 'granted' | 'prompt' | 'denied' — 'prompt' means the browser needs an
 * explicit user gesture (a click) before access is restored; that's a
 * browser security rule, not something that can be bypassed silently. */
export async function checkFolderPermission(handle) {
  try {
    return await handle.queryPermission({ mode: 'read' })
  } catch {
    return 'denied'
  }
}

/** Must be called from a real user gesture (e.g. a button's onClick). */
export async function requestFolderPermission(handle) {
  try {
    return await handle.requestPermission({ mode: 'read' })
  } catch {
    return 'denied'
  }
}

/** Removes a folder and every song that came from it (individually-added
 * songs are untouched). */
export async function removeMusicFolder(folderId) {
  await withFolders('readwrite', (store) => store.delete(folderId))
  const all = await withSongs('readonly', (store) => store.getAll())
  const toDelete = all.filter((s) => s.folderId === folderId)
  await Promise.all(toDelete.map((s) => withSongs('readwrite', (store) => store.delete(s.id))))
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

/** Folders + their live permission state, refreshed on demand (e.g. after
 * reconnecting one). Silently rescans any folder that already has
 * permission, so returning users see fresh contents without clicking
 * anything — but never auto-requests permission for one that needs it,
 * since browsers require a user gesture for that. */
export function useMusicFolders() {
  const [folders, setFolders] = useState([])

  const refresh = useCallback(async () => {
    const stored = await getMusicFolders()
    const withStatus = await Promise.all(
      stored.map(async (f) => ({ ...f, permission: await checkFolderPermission(f.handle) }))
    )
    setFolders(withStatus)
    // Silently refresh contents of already-granted folders.
    for (const f of withStatus) {
      if (f.permission === 'granted') {
        scanFolder(f.id, f.handle).catch(() => {})
      }
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return [folders, refresh]
}
