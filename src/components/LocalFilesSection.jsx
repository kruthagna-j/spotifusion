import { useRef, useState } from 'react'
import { Upload, HardDrive, Trash2 } from 'lucide-react'
import { useLocalTracks, addLocalFiles, deleteLocalTrack } from '@/lib/localLibrary'
import TrackRow from '@/components/TrackRow'

export default function LocalFilesSection() {
  const [tracks, refresh] = useLocalTracks()
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef(null)

  async function handleFiles(e) {
    const files = e.target.files
    if (!files?.length) return
    setUploading(true)
    await addLocalFiles(files)
    refresh()
    setUploading(false)
    e.target.value = ''
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-3 gap-2">
        <div className="flex items-center gap-2 text-sm font-semibold text-text">
          <HardDrive size={16} className="text-text-muted" />
          Files on this device
        </div>
        <button
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className="flex items-center gap-2 bg-surface-highlight hover:bg-surface-hover disabled:opacity-60 text-xs font-bold px-3 py-1.5 rounded-full shrink-0"
        >
          <Upload size={14} />
          {uploading ? 'Adding…' : 'Add audio files'}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept="audio/*"
          multiple
          hidden
          onChange={handleFiles}
        />
      </div>

      <p className="text-xs text-text-subdued mb-4">
        Add MP3s or other audio files you already own. They play from this browser only — nothing
        is uploaded anywhere, so they won't show up if you open Spotifusion on another device.
      </p>

      {tracks.length > 0 && (
        <div className="flex flex-col">
          {tracks.map((track, i) => (
            <div key={track.id} className="group flex items-center">
              <div className="flex-1 min-w-0">
                <TrackRow track={track} index={i} contextTracks={tracks} />
              </div>
              <button
                onClick={() => deleteLocalTrack(track.id).then(refresh)}
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
