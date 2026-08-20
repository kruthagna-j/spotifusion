import { useState, useRef, useEffect } from 'react'
import { MoreHorizontal, Plus, Check } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import { addTrackToPlaylist, createPlaylist } from '@/lib/library'

export default function AddToPlaylistMenu({ track }) {
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const [open, setOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const ref = useRef(null)

  useEffect(() => {
    function onClickOutside(e) {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false)
        setCreating(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  async function handleCreateAndAdd() {
    if (!name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    await addTrackToPlaylist(user.uid, id, track)
    setCreating(false)
    setName('')
    setOpen(false)
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={(e) => {
          e.stopPropagation()
          if (!user) return signIn()
          setOpen((v) => !v)
        }}
        className="opacity-0 group-hover:opacity-100 transition-opacity p-1 text-text-muted hover:text-text"
        title="Add to playlist"
      >
        <MoreHorizontal size={18} />
      </button>

      {open && (
        <div
          onClick={(e) => e.stopPropagation()}
          className="absolute right-0 top-full mt-1 w-56 bg-surface-elevated shadow-2xl rounded-md py-2 z-30 border border-border"
        >
          <p className="px-3 py-1 text-xs text-text-subdued font-semibold uppercase">Add to playlist</p>

          {!creating ? (
            <>
              <button
                onClick={() => setCreating(true)}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-surface-hover text-left"
              >
                <Plus size={16} /> New playlist
              </button>
              <div className="max-h-48 overflow-y-auto scrollbar-none">
                {playlists.map((p) => {
                  const already = p.trackIds?.includes(track.id)
                  return (
                    <button
                      key={p.id}
                      disabled={already}
                      onClick={async () => {
                        await addTrackToPlaylist(user.uid, p.id, track)
                        setOpen(false)
                      }}
                      className="w-full flex items-center justify-between gap-2 px-3 py-2 text-sm hover:bg-surface-hover text-left disabled:opacity-50"
                    >
                      <span className="truncate">{p.name}</span>
                      {already && <Check size={14} className="text-brand shrink-0" />}
                    </button>
                  )
                })}
                {playlists.length === 0 && (
                  <p className="px-3 py-2 text-xs text-text-subdued">No playlists yet.</p>
                )}
              </div>
            </>
          ) : (
            <div className="px-3 py-1 flex gap-2">
              <input
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreateAndAdd()}
                placeholder="Playlist name"
                className="flex-1 bg-surface-hover rounded px-2 py-1 text-sm outline-none min-w-0"
              />
              <button
                onClick={handleCreateAndAdd}
                className="bg-brand text-black text-xs font-bold px-2 rounded shrink-0"
              >
                Add
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
