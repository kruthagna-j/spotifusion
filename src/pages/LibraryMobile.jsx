import { useEffect, useState } from 'react'
import { Plus, Heart, ListMusic } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import { createPlaylist } from '@/lib/library'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import LocalFilesSection from '@/components/LocalFilesSection'

// Mobile-only "Your Library" tab (desktop shows this inside the sidebar instead)
export default function LibraryMobile() {
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const navigate = useNavigate()
  const location = useLocation()
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  // Opened via the mobile bottom-nav "Create" tab.
  useEffect(() => {
    if (location.state?.openCreate && user) {
      setCreating(true)
      // Clear the flag so it doesn't re-trigger on back/forward navigation.
      navigate(location.pathname, { replace: true, state: {} })
    }
  }, [location, user, navigate])

  async function handleCreate() {
    if (!name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    setCreating(false)
    setName('')
    navigate(`/playlist/${id}`)
  }

  return (
    <div className="sf-library-page p-4 sm:p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-black tracking-tight">Your Library</h1>
        {user && (
          <button onClick={() => setCreating(true)} className="sf-round-action" aria-label="Create playlist">
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
            <div className="sf-library-icon sf-library-icon--liked">
              <Heart size={18} fill="white" className="text-white" />
            </div>
            <div>
              <p className="text-sm font-medium">Liked Songs</p>
              <p className="text-xs text-text-subdued">Playlist</p>
            </div>
          </Link>

          {playlists.map((p) => (
          <Link key={p.id} to={`/playlist/${p.id}`} className="sf-library-card flex items-center gap-3">
              <div className="sf-library-icon">
                <ListMusic size={18} className="text-text-subdued" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-bold truncate">{p.name}</p>
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
