import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Upload, FolderPlus, Folder, HardDrive, Trash2, Heart, Search, X, FolderOpen,
  ShieldCheck, RefreshCw, AlertTriangle, SlidersHorizontal,
} from 'lucide-react'
import {
  useLocalSongs, addLocalSongs, deleteLocalSong, updateLocalSong, useMusicFolders,
  addMusicFolder, requestFolderPermission, removeMusicFolder, scanFolder, SUPPORTS_FOLDER_ACCESS,
} from '@/lib/localMusicDb'
import TrackRow from '@/components/TrackRow'

const ACCEPT = '.mp3,.wav,.m4a,.aac,.ogg,.flac,.webm,audio/*'
const LOCAL_PERSONALIZATION_KEY = 'spotifusion:local-personalization:v1'

export default function LocalFilesSection() {
  const location = useLocation()
  const navigate = useNavigate()
  const inSettings = location.pathname === '/settings'
  const [songs, refresh] = useLocalSongs()
  const [folders, refreshFolders] = useMusicFolders()
  const [uploading, setUploading] = useState(false)
  const [scanStatus, setScanStatus] = useState(null)
  const [query, setQuery] = useState('')
  const [persisted, setPersisted] = useState(null)
  const [personalize, setPersonalize] = useState(() => {
    try { return localStorage.getItem(LOCAL_PERSONALIZATION_KEY) === 'true' } catch { return false }
  })
  const inputRef = useRef(null)

  useEffect(() => { navigator.storage?.persisted?.().then(setPersisted) }, [])
  useEffect(() => { try { localStorage.setItem(LOCAL_PERSONALIZATION_KEY, String(personalize)) } catch {} }, [personalize])

  async function requestPersistentStorage() {
    if (!navigator.storage?.persist) return
    const granted = await navigator.storage.persist()
    setPersisted(granted)
  }

  async function handleFiles(e) {
    const files = e.target.files
    if (!files?.length) return
    setUploading(true)
    try { await addLocalSongs(files); refresh() } finally { setUploading(false); e.target.value = '' }
  }

  async function handleAddFolder() {
    setScanStatus({ name: 'your folder', found: 0 })
    try {
      await addMusicFolder((count) => setScanStatus((s) => (s ? { ...s, found: count } : s)))
      refreshFolders(); refresh()
    } catch (err) { if (err.name !== 'AbortError') console.error(err) }
    finally { setScanStatus(null) }
  }

  async function handleReconnect(folder) {
    const result = await requestFolderPermission(folder.handle)
    if (result !== 'granted') return
    setScanStatus({ name: folder.name, found: 0 })
    try {
      await scanFolder(folder.id, folder.handle, (count) => setScanStatus((s) => (s ? { ...s, found: count } : s)))
      refreshFolders(); refresh()
    } finally { setScanStatus(null) }
  }

  async function handleRemoveFolder(folder) {
    await removeMusicFolder(folder.id); refreshFolders(); refresh()
  }

  async function toggleFavorite(song) {
    await updateLocalSong(song.id, { favorite: !song.favorite }); refresh()
  }

  function handlePersonalizationToggle(next) {
    setPersonalize(next)
    if (next) navigate('/onboarding?source=local')
  }

  const visible = query.trim()
    ? songs.filter((s) => s.title.toLowerCase().includes(query.toLowerCase()) || s.artist.toLowerCase().includes(query.toLowerCase()))
    : songs

  return (
    <div>
      {inSettings && (
        <div className="mb-5 rounded-2xl border border-white/10 bg-surface-elevated/70 p-4 md:p-5">
          <div className="flex items-center gap-3">
            <span className="w-10 h-10 rounded-xl bg-brand/10 text-brand grid place-items-center shrink-0"><SlidersHorizontal size={18}/></span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold">Personalize local media</p>
              <p className="text-xs text-text-subdued mt-1 leading-5">Optional. Turn this on to choose multiple languages and favorite artists for local-media recommendations. Your local music itself remains completely optional.</p>
            </div>
            <button type="button" role="switch" aria-checked={personalize} aria-label="Personalize local media" onClick={() => handlePersonalizationToggle(!personalize)} className={`relative w-12 h-7 rounded-full shrink-0 transition ${personalize ? 'bg-brand' : 'bg-white/15'}`}>
              <span className={`absolute top-1 w-5 h-5 rounded-full bg-white shadow transition-transform ${personalize ? 'translate-x-6' : 'translate-x-1'}`} />
            </button>
          </div>
          {personalize && <p className="text-[11px] text-brand font-semibold mt-3">Personalization is enabled. Languages and favorite artists can be changed anytime from this option.</p>}
        </div>
      )}

      <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
        <div className="flex items-center gap-2 text-sm font-semibold text-text"><HardDrive size={16} className="text-text-muted" />Local Files</div>
        <div className="flex items-center gap-2">
          {SUPPORTS_FOLDER_ACCESS && <button onClick={handleAddFolder} disabled={!!scanStatus} aria-label="Add a music folder — discovers every song in it automatically" className="flex items-center gap-2 bg-brand hover:bg-brand-hover disabled:opacity-60 text-black text-xs font-bold px-3 py-1.5 rounded-full shrink-0"><FolderPlus size={14} />Add Music Folder</button>}
          <button onClick={() => inputRef.current?.click()} disabled={!!scanStatus || uploading} aria-label="Add individual audio files from this device" className="flex items-center gap-2 bg-surface-highlight hover:bg-surface-hover disabled:opacity-60 text-xs font-bold px-3 py-1.5 rounded-full shrink-0"><Upload size={14} />{uploading ? 'Adding…' : SUPPORTS_FOLDER_ACCESS ? 'Add Files' : 'Add to Device'}</button>
        </div>
        <input ref={inputRef} type="file" accept={ACCEPT} multiple hidden onChange={handleFiles} />
      </div>

      <p className="text-xs text-text-subdued mb-3">Your local music stays on your device — Spotifusion never uploads your local audio files.{SUPPORTS_FOLDER_ACCESS ? ' Add a whole folder once and every supported song inside it (including subfolders) is discovered automatically.' : ' Supports MP3, WAV, M4A, AAC, OGG, FLAC, and WEBM.'}</p>

      {scanStatus && <div className="flex items-center gap-2 text-sm bg-surface-highlight rounded-md px-3 py-2 mb-3"><RefreshCw size={14} className="animate-spin shrink-0" />Scanning {scanStatus.name}… found {scanStatus.found} song{scanStatus.found === 1 ? '' : 's'}</div>}

      {folders.length > 0 && <div className="flex flex-col gap-1.5 mb-4">{folders.map((folder) => <div key={folder.id} className="flex items-center gap-3 bg-surface-highlight rounded-md px-3 py-2"><Folder size={16} className="text-text-muted shrink-0" /><span className="text-sm truncate flex-1">{folder.name}</span>{folder.permission === 'granted' ? <span className="text-[11px] text-brand font-semibold shrink-0">Connected</span> : <button onClick={() => handleReconnect(folder)} className="flex items-center gap-1 text-[11px] font-semibold text-yellow-500 hover:text-yellow-400 shrink-0"><AlertTriangle size={12} />Reconnect Music Folder</button>}<button onClick={() => handleRemoveFolder(folder)} aria-label={`Remove folder ${folder.name}`} className="text-text-muted hover:text-red-400 shrink-0"><Trash2 size={14} /></button></div>)}</div>}

      {songs.length > 0 && persisted === false && <button onClick={requestPersistentStorage} className="flex items-center gap-2 text-xs text-yellow-500 bg-yellow-500/10 hover:bg-yellow-500/20 rounded-md px-3 py-2 mb-3"><ShieldCheck size={14} className="shrink-0" />The browser may clear these files under storage pressure — tap to grant permanent storage permission.</button>}

      {songs.length > 0 && <div className="flex items-center justify-between mb-3 gap-2 flex-wrap"><div className="relative max-w-xs flex-1 min-w-[180px]"><Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued" aria-hidden="true" /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search local music" aria-label="Search local music" className="w-full bg-surface-highlight rounded-full pl-8 pr-8 py-1.5 text-xs outline-none" />{query && <button onClick={() => setQuery('')} aria-label="Clear search" className="absolute right-2 top-1/2 -translate-y-1/2 text-text-subdued hover:text-text"><X size={14} /></button>}</div><span className="text-xs text-text-subdued shrink-0">{songs.length} song{songs.length === 1 ? '' : 's'}{folders.length > 0 ? ` · ${folders.length} folder${folders.length === 1 ? '' : 's'}` : ''}</span></div>}

      {songs.length === 0 && !scanStatus && <div className="text-center py-16"><FolderOpen size={40} className="mx-auto mb-3 text-text-subdued" aria-hidden="true" /><p className="text-text-muted text-sm">No local music yet — add a folder or a few files.</p></div>}
      {songs.length > 0 && visible.length === 0 && <p className="text-xs text-text-subdued">No local music matches "{query}".</p>}

      {visible.length > 0 && <div className="flex flex-col">{visible.map((song, i) => <div key={song.id} className="group flex items-center"><div className="flex-1 min-w-0"><TrackRow track={song} index={i} contextTracks={visible} /></div><button onClick={() => toggleFavorite(song)} aria-label={song.favorite ? `Unfavorite ${song.title}` : `Favorite ${song.title}`} aria-pressed={song.favorite} className={`p-2 transition-opacity ${song.favorite ? 'text-brand opacity-100' : 'text-text-muted opacity-0 group-hover:opacity-100'} hover:text-brand`} title={song.favorite ? 'Unfavorite' : 'Favorite'}><Heart size={16} fill={song.favorite ? 'currentColor' : 'none'} /></button><button onClick={() => deleteLocalSong(song.id).then(refresh)} aria-label={`Remove ${song.title} from this device`} className="opacity-0 group-hover:opacity-100 p-2 mr-2 text-text-muted hover:text-red-400 transition-opacity" title="Remove from this device"><Trash2 size={16} /></button></div>)}</div>}
    </div>
  )
}
