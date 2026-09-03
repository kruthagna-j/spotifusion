import { useEffect, useRef, useState } from 'react'
import { HardDrive, ShieldCheck, AlertTriangle, RefreshCw, Heart, Trash2, Search, Plus, FolderOpen } from 'lucide-react'
import { useLocalSongs, deleteLocalSong, updateLocalSong, addLocalSongs, addMusicFolder, SUPPORTS_FOLDER_ACCESS } from '@/lib/localMusicDb'
import TrackRow from '@/components/TrackRow'

const ACCESS_KEY = 'spotifusion:device-media-access:v1'

function getNativeMediaBridge() {
  if (typeof window === 'undefined') return null
  return window.SpotifusionMedia || window.AndroidMedia || null
}

export default function LocalFilesSection() {
  const [songs, refresh] = useLocalSongs()
  const [query, setQuery] = useState('')
  const [enabled, setEnabled] = useState(() => {
    try { return localStorage.getItem(ACCESS_KEY) === 'true' } catch { return false }
  })
  const [permission, setPermission] = useState('unknown')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [importBusy, setImportBusy] = useState(false)
  const [importMessage, setImportMessage] = useState('')
  const fileInputRef = useRef(null)

  async function handleFilesPicked(e) {
    const files = e.target.files
    if (!files || files.length === 0) return
    setImportBusy(true)
    setImportMessage('')
    try {
      const added = await addLocalSongs(files)
      refresh()
      setImportMessage(added.length ? `Added ${added.length} song${added.length === 1 ? '' : 's'}.` : 'No supported audio files found in that selection.')
    } catch (err) {
      setImportMessage(err.message || 'Could not add those files.')
    } finally {
      setImportBusy(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function handleAddFolder() {
    setImportBusy(true)
    setImportMessage('')
    try {
      const added = await addMusicFolder()
      refresh()
      setImportMessage(added?.length ? `Added ${added.length} song${added.length === 1 ? '' : 's'} from the folder.` : 'No supported audio files found in that folder.')
    } catch (err) {
      if (err?.name !== 'AbortError') setImportMessage(err.message || 'Could not read that folder.')
    } finally {
      setImportBusy(false)
    }
  }

  async function syncNativeMedia() {
    const bridge = getNativeMediaBridge()
    if (!bridge) {
      setPermission('unavailable')
      setMessage('Device media access is available in the Spotifusion Android app. A normal browser cannot access the phone music library directly.')
      return
    }

    setBusy(true)
    setMessage('')
    try {
      const granted = typeof bridge.requestAudioPermission === 'function'
        ? await bridge.requestAudioPermission()
        : typeof bridge.requestMediaPermission === 'function'
          ? await bridge.requestMediaPermission()
          : false

      if (!granted) {
        setPermission('denied')
        setEnabled(false)
        try { localStorage.setItem(ACCESS_KEY, 'false') } catch {}
        setMessage('Music access was not granted. You can turn this on again anytime.')
        return
      }

      setPermission('granted')
      setEnabled(true)
      try { localStorage.setItem(ACCESS_KEY, 'true') } catch {}

      if (typeof bridge.getAudioTracks === 'function') {
        const result = await bridge.getAudioTracks()
        const tracks = typeof result === 'string' ? JSON.parse(result) : result
        if (Array.isArray(tracks) && typeof bridge.importAudioTracks === 'function') {
          await bridge.importAudioTracks(tracks)
        }
      }
      refresh()
    } catch (error) {
      console.error('Device media access failed:', error)
      setPermission('error')
      setMessage('Could not access device music. Please check the Android media permission and try again.')
    } finally {
      setBusy(false)
    }
  }

  async function toggleAccess() {
    if (enabled) {
      setEnabled(false)
      setPermission('off')
      try { localStorage.setItem(ACCESS_KEY, 'false') } catch {}
      return
    }
    await syncNativeMedia()
  }

  useEffect(() => {
    const bridge = getNativeMediaBridge()
    if (!enabled || !bridge) return
    syncNativeMedia()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function toggleFavorite(song) {
    await updateLocalSong(song.id, { favorite: !song.favorite })
    refresh()
  }

  const visible = query.trim()
    ? songs.filter((s) => s.title.toLowerCase().includes(query.toLowerCase()) || s.artist.toLowerCase().includes(query.toLowerCase()))
    : songs

  const status = permission === 'granted' || enabled ? 'On' : 'Off'

  return (
    <div>
      <div className="rounded-2xl border border-white/10 bg-surface-elevated/70 p-4 md:p-5">
        <div className="flex items-center gap-3">
          <span className="w-10 h-10 rounded-xl bg-brand/10 text-brand grid place-items-center shrink-0">
            <HardDrive size={18} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold">Access device music</p>
            <p className="text-xs text-text-subdued mt-1 leading-5">
              When enabled, Spotifusion can read the music stored on this device and show it in Local Music. Your audio is not uploaded to Spotifusion.
            </p>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={enabled}
            aria-label="Access device music"
            disabled={busy}
            onClick={toggleAccess}
            className={`relative w-12 h-7 rounded-full shrink-0 transition ${enabled ? 'bg-brand' : 'bg-white/15'} ${busy ? 'opacity-60' : ''}`}
          >
            <span className={`absolute top-1 w-5 h-5 rounded-full bg-white shadow transition-transform ${enabled ? 'translate-x-6' : 'translate-x-1'}`} />
          </button>
        </div>

        <div className="mt-3 flex items-center gap-2 text-[11px] font-semibold">
          {busy ? <><RefreshCw size={13} className="animate-spin" /> Requesting media permission…</> : permission === 'denied' ? <><AlertTriangle size={13} className="text-yellow-500" /> Permission denied</> : permission === 'unavailable' ? <><AlertTriangle size={13} className="text-yellow-500" /> Android app required for direct device-library access</> : <><ShieldCheck size={13} className={enabled ? 'text-brand' : 'text-text-subdued'} /> Media access {status}</>}
        </div>

        {message && <p className="text-xs text-yellow-500 mt-3 leading-5">{message}</p>}
      </div>

      <div className="rounded-2xl border border-white/10 bg-surface-elevated/70 p-4 md:p-5 mt-4">
        <div className="flex items-center gap-3 mb-3">
          <span className="w-10 h-10 rounded-xl bg-brand/10 text-brand grid place-items-center shrink-0">
            <Plus size={18} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold">Add music from this device</p>
            <p className="text-xs text-text-subdued mt-1 leading-5">
              Works in any browser. Files stay on this device — nothing is uploaded. Supports MP3, WAV, M4A, AAC, OGG, FLAC.
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <input
            ref={fileInputRef}
            type="file"
            accept="audio/*,.mp3,.wav,.m4a,.aac,.ogg,.flac,.webm"
            multiple
            onChange={handleFilesPicked}
            className="hidden"
            id="local-audio-file-input"
          />
          <button
            type="button"
            disabled={importBusy}
            onClick={() => fileInputRef.current?.click()}
            className="inline-flex items-center gap-2 bg-brand text-black font-bold text-xs px-4 py-2.5 rounded-full disabled:opacity-60"
          >
            {importBusy ? <RefreshCw size={14} className="animate-spin" /> : <Plus size={14} />}
            Add music files
          </button>
          {SUPPORTS_FOLDER_ACCESS && (
            <button
              type="button"
              disabled={importBusy}
              onClick={handleAddFolder}
              className="inline-flex items-center gap-2 bg-white/10 font-bold text-xs px-4 py-2.5 rounded-full disabled:opacity-60"
            >
              <FolderOpen size={14} />
              Add a folder
            </button>
          )}
        </div>
        {importMessage && <p className="text-xs text-text-subdued mt-3 leading-5">{importMessage}</p>}
      </div>

      {songs.length > 0 && (
        <div className="mt-5">
          <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
            <div className="flex items-center gap-2 text-sm font-semibold text-text"><HardDrive size={16} className="text-text-muted" />Device Music</div>
            <span className="text-xs text-text-subdued">{songs.length} song{songs.length === 1 ? '' : 's'}</span>
          </div>
          <div className="relative max-w-xs mb-3">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued" aria-hidden="true" />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search device music" aria-label="Search device music" className="w-full bg-surface-highlight rounded-full pl-8 pr-3 py-2 text-xs outline-none" />
          </div>
          <div className="flex flex-col">
            {visible.map((song, i) => (
              <div key={song.id} className="group flex items-center">
                <div className="flex-1 min-w-0"><TrackRow track={song} index={i} contextTracks={visible} /></div>
                <button onClick={() => toggleFavorite(song)} aria-label={song.favorite ? `Unfavorite ${song.title}` : `Favorite ${song.title}`} className={`p-2 ${song.favorite ? 'text-brand opacity-100' : 'text-text-muted opacity-0 group-hover:opacity-100'} hover:text-brand`}>
                  <Heart size={16} fill={song.favorite ? 'currentColor' : 'none'} />
                </button>
                <button onClick={() => deleteLocalSong(song.id).then(refresh)} aria-label={`Remove ${song.title} from Spotifusion`} className="opacity-0 group-hover:opacity-100 p-2 mr-2 text-text-muted hover:text-red-400">
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {enabled && songs.length === 0 && !busy && (
        <div className="text-center py-12 text-text-muted text-sm">No device music was returned by the Android media provider.</div>
      )}
      {!enabled && songs.length === 0 && !busy && (
        <div className="text-center py-8 text-text-muted text-sm">No local music yet — add files or a folder above to get started.</div>
      )}
    </div>
  )
}
