import { useRef, useState } from 'react'
import { Upload, HardDrive, Trash2, Heart, Search, X } from 'lucide-react'
import { useLocalSongs, addLocalSongs, deleteLocalSong, updateLocalSong } from '@/lib/localMusicDb'
import TrackRow from '@/components/TrackRow'

// Browser-supported audio types this app explicitly offers for import.
const ACCEPT = 'audio/mpeg,audio/wav,audio/x-wav,audio/ogg,audio/mp4,audio/*'

export default function LocalFilesSection() {
  const [songs, refresh] = useLocalSongs()
  const [uploading, setUploading] = useState(false)
  const [query, setQuery] = useState('')
  const inputRef = useRef(null)

  async function handleFiles(e) {
    const files = e.target.files
    if (!files?.length) return
    setUploading(true)
    await addLocalSongs(files)
    refresh()
    setUploading(false)
    e.target.value = ''
  }

  async function toggleFavorite(song) {
    await updateLocalSong(song.id, { favorite: !song.favorite })
    refresh()
  }

  const visible = query.trim()
    ? songs.filter(
        (s) =>
          s.title.toLowerCase().includes(query.toLowerCase()) ||
          s.artist.toLowerCase().includes(query.toLowerCase())
      )
    : songs

  return (
    <div>
      <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
        <div className="flex items-center gap-2 text-sm font-semibold text-text">
          <HardDrive size={16} className="text-text-muted" />
          Local Files
        </div>
        <button
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className="flex items-center gap-2 bg-surface-highlight hover:bg-surface-hover disabled:opacity-60 text-xs font-bold px-3 py-1.5 rounded-full shrink-0"
        >
          <Upload size={14} />
          {uploading ? 'Adding…' : 'Add to Device'}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          multiple
          hidden
          onChange={handleFiles}
        />
      </div>

      <p className="text-xs text-text-subdued mb-3">
        Import MP3, WAV, OGG, or M4A files you already own. They're stored on this device only
        (IndexedDB) — nothing is uploaded, so they won't appear if you open Spotifusion elsewhere,
        and they keep playing with no internet connection.
      </p>

      {songs.length > 0 && (
        <div className="relative mb-3 max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search local files"
            className="w-full bg-surface-highlight rounded-full pl-8 pr-8 py-1.5 text-xs outline-none"
          />
          {query && (
            <button
              onClick={() => setQuery('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-text-subdued hover:text-text"
            >
              <X size={14} />
            </button>
          )}
        </div>
      )}

      {songs.length > 0 && (
        <div className="flex items-center gap-1.5 text-[11px] text-brand font-semibold mb-2">
          <span className="inline-block w-1.5 h-1.5 rounded-full bg-brand" />
          Available Offline · {songs.length} song{songs.length === 1 ? '' : 's'}
        </div>
      )}

      {songs.length > 0 && visible.length === 0 && (
        <p className="text-xs text-text-subdued">No local files match "{query}".</p>
      )}

      {visible.length > 0 && (
        <div className="flex flex-col">
          {visible.map((song, i) => (
            <div key={song.id} className="group flex items-center">
              <div className="flex-1 min-w-0">
                <TrackRow track={song} index={i} contextTracks={visible} />
              </div>
              <button
                onClick={() => toggleFavorite(song)}
                className={`p-2 transition-opacity ${
                  song.favorite ? 'text-brand opacity-100' : 'text-text-muted opacity-0 group-hover:opacity-100'
                } hover:text-brand`}
                title={song.favorite ? 'Unfavorite' : 'Favorite'}
              >
                <Heart size={16} fill={song.favorite ? 'currentColor' : 'none'} />
              </button>
              <button
                onClick={() => deleteLocalSong(song.id).then(refresh)}
                className="opacity-0 group-hover:opacity-100 p-2 mr-2 text-text-muted hover:text-red-400 transition-opacity"
                title="Remove from this device"
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
