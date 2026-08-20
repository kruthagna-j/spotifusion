import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayed, usePlaylists } from '@/hooks/useLibraryData'
import { usePlayer } from '@/context/PlayerContext'
import { Play } from 'lucide-react'

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 18) return 'Good afternoon'
  return 'Good evening'
}

export default function Home() {
  const { user, signIn } = useAuth()
  const recent = useRecentlyPlayed(user?.uid, 8)
  const playlists = usePlaylists(user?.uid)
  const player = usePlayer()

  return (
    <div className="p-4 md:p-6">
      <h1 className="text-2xl md:text-3xl font-bold mb-6">{greeting()}</h1>

      {!user && (
        <div className="bg-surface-elevated rounded-lg p-6 mb-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <p className="font-semibold mb-1">Sign in to build your library</p>
            <p className="text-text-muted text-sm">
              Save liked songs, create playlists, and pick up recently played tracks across devices.
            </p>
          </div>
          <button
            onClick={signIn}
            className="bg-brand hover:bg-brand-hover text-black font-bold px-6 py-2.5 rounded-full shrink-0"
          >
            Sign in with Google
          </button>
        </div>
      )}

      {user && recent.length > 0 && (
        <section className="mb-10">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {recent.map((track) => (
              <button
                key={track.id}
                onClick={() => player.playTrack(track, recent)}
                className="group flex items-center gap-4 bg-surface-elevated hover:bg-surface-hover rounded-md overflow-hidden text-left transition-colors"
              >
                <img src={track.thumbnail} alt="" className="w-16 h-16 object-cover shrink-0" />
                <span className="font-semibold text-sm truncate pr-2">{track.title}</span>
                <span className="ml-auto mr-3 w-9 h-9 rounded-full bg-brand text-black hidden group-hover:flex items-center justify-center shrink-0">
                  <Play size={16} className="ml-0.5" />
                </span>
              </button>
            ))}
          </div>
        </section>
      )}

      {user && playlists.length > 0 && (
        <section>
          <h2 className="text-xl font-bold mb-4">Your playlists</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5">
            {playlists.map((p) => (
              <a
                key={p.id}
                href={`/playlist/${p.id}`}
                className="bg-surface-elevated hover:bg-surface-hover rounded-lg p-3 transition-colors"
              >
                <div className="aspect-square rounded-md bg-surface-highlight mb-3 flex items-center justify-center text-3xl">
                  🎵
                </div>
                <p className="font-semibold text-sm truncate">{p.name}</p>
                <p className="text-xs text-text-subdued truncate">{p.trackIds?.length || 0} songs</p>
              </a>
            ))}
          </div>
        </section>
      )}

      {user && recent.length === 0 && playlists.length === 0 && (
        <p className="text-text-muted text-sm">
          Search for a song to start listening — it'll show up here once you do.
        </p>
      )}
    </div>
  )
}
