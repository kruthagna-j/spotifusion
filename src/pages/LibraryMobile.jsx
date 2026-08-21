import { useState } from 'react'
import { Plus, Heart, ListMusic } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import { createPlaylist } from '@/lib/library'
import { useNavigate, Link } from 'react-router-dom'
import LocalFilesSection from '@/components/LocalFilesSection'

// Mobile-only "Your Library" tab (desktop shows this inside the sidebar instead)
export default function LibraryMobile() {
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  async function handleCreate() {
    if (!name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    setCreating(false)
    setName('')
    navigate(`/playlist/${id}`)
  }

  return (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">Your Library</h1>
        {user && (
          <button onClick={() => setCreating(true)} className="p-2 rounded-full hover:bg-surface-hover">
            <Plus size={22} />
          </button>
        )}
      </div>

      {!user ? (
        <div className="text-center mt-4 mb-8 p-4 bg-surface-elevated rounded-lg">
          <p className="text-text-muted mb-4 text-sm">Sign in to create playlists and like songs.</p>
          <button onClick={signIn} className="bg-brand text-black font-bold px-6 py-2.5 rounded-full">
            Sign in with Google
          </button>
        </div>
      ) : (
        <>
          {creating && (
            <div className="flex gap-2 mb-4">
              <input
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
                placeholder="Playlist name"
                className="flex-1 bg-surface-elevated rounded px-3 py-2 text-sm outline-none"
              />
              <button onClick={handleCreate} className="bg-brand text-black font-bold px-4 rounded text-sm">
                Create
              </button>
            </div>
          )}

          <Link to="/liked-songs" className="flex items-center gap-3 py-2">
            <div className="w-12 h-12 rounded bg-gradient-to-br from-indigo-400 to-white flex items-center justify-center shrink-0">
              <Heart size={18} fill="white" className="text-white" />
            </div>
            <div>
              <p className="text-sm font-medium">Liked Songs</p>
              <p className="text-xs text-text-subdued">Playlist</p>
            </div>
          </Link>

          {playlists.map((p) => (
            <Link key={p.id} to={`/playlist/${p.id}`} className="flex items-center gap-3 py-2">
              <div className="w-12 h-12 rounded bg-surface-highlight flex items-center justify-center shrink-0">
                <ListMusic size={18} className="text-text-subdued" />
              </div>
              <div>
                <p className="text-sm font-medium">{p.name}</p>
                <p className="text-xs text-text-subdued">Playlist • {p.trackIds?.length || 0} songs</p>
              </div>
            </Link>
          ))}
        </>
      )}

      <div className="h-px bg-border my-6" />

      <LocalFilesSection />
    </div>
  )
}
