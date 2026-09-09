import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight, Play } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus, usePlaylistsStatus } from '@/hooks/useLibraryData'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'
import { SkeletonCardGrid } from '@/components/Skeleton'

const FILTERS = ['All', 'Songs', 'Albums', 'Artists', 'Jukebox']

const FEATURED_ITEMS = [
  { id: 'f-1', title: 'Chill Vibes', subtitle: 'Playlist', gradient: 'from-cyan-500 to-blue-600', textCol: 'text-cyan-200' },
  { id: 'f-2', title: 'Top 50', subtitle: 'YouTube', gradient: 'from-rose-500 to-amber-600', textCol: 'text-amber-200' },
  { id: 'f-3', title: 'Trending Now', subtitle: 'Playlist', gradient: 'from-purple-600 to-pink-600', textCol: 'text-pink-200' },
]

function SectionHeader({ title }) {
  return (
    <div className="flex items-center justify-between mb-3">
      <h2 className="text-lg md:text-xl font-bold">{title}</h2>
      <ChevronRight size={20} className="text-text-muted" aria-hidden="true" />
    </div>
  )
}

// Mobile quick-access row card — matches the Figma mobile Home pattern
// exactly: 175x58 pill, #202020 fill, rounded-[10px], 43px square thumb.
function QuickAccessCard({ track, onPlay }) {
  return (
    <button
      onClick={onPlay}
      className="group flex items-center gap-0 bg-surface-elevated hover:bg-surface-hover rounded-[10px] h-[58px] w-full overflow-hidden text-left transition-colors"
    >
      <img src={track.thumbnail} alt="" className="w-[43px] h-[43px] rounded-[3px] object-cover ml-[8px] shrink-0" />
      <span className="text-sm px-3 truncate">{track.title}</span>
      <Play
        size={16}
        aria-hidden="true"
        className="ml-auto mr-3 opacity-0 group-hover:opacity-100 shrink-0 transition-opacity"
      />
    </button>
  )
}

// Desktop square card — matches the Figma desktop Home pattern: 170x170
// artwork, title + count row underneath.
function SquareCard({ image, title, subtitle, onPlay, to }) {
  const inner = (
    <>
      <div className="relative aspect-square rounded-[10px] bg-surface-highlight mb-3 overflow-hidden">
        {image ? (
          <img src={image} alt="" className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-3xl">🎵</div>
        )}
        {onPlay && (
          <button
            onClick={(e) => {
              e.preventDefault()
              onPlay()
            }}
            aria-label={`Play ${title}`}
            className="absolute bottom-2 right-2 w-10 h-10 rounded-full bg-brand text-black flex items-center justify-center opacity-0 group-hover:opacity-100 translate-y-1 group-hover:translate-y-0 transition-all shadow-card"
          >
            <Play size={18} className="ml-0.5" />
          </button>
        )}
      </div>
      <p className="font-semibold text-sm truncate">{title}</p>
      {subtitle && <p className="text-xs text-text-muted truncate">{subtitle}</p>}
    </>
  )
  return to ? (
    <Link to={to} className="group block">
      {inner}
    </Link>
  ) : (
    <div className="group">{inner}</div>
  )
}

export default function Home() {
  const { user, signIn } = useAuth()
  const { data: recent, loading: recentLoading } = useRecentlyPlayedStatus(user?.uid, 8)
  const { data: playlists, loading: playlistsLoading } = usePlaylistsStatus(user?.uid)
  const [localSongs] = useLocalSongs()
  const player = usePlayer()
  const [filter, setFilter] = useState('All')

  const showPlaylists = filter === 'All' || filter === 'Playlists'
  const showLocal = filter === 'All' || filter === 'Local Files'

  const nothingAtAll =
    user &&
    !recentLoading &&
    !playlistsLoading &&
    recent.length === 0 &&
    playlists.length === 0 &&
    localSongs.length === 0

  return (
    <div className="p-4 md:p-6">
      {!user && (
        <div className="bg-surface-elevated rounded-[10px] p-6 mb-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border border-border">
          <div>
            <p className="font-semibold mb-1">Sign in to build your library</p>
            <p className="text-text-muted text-sm">
              Save liked songs, create playlists, and pick up recently played tracks across devices.
            </p>
          </div>
          <button
            onClick={signIn}
            className="bg-brand hover:bg-brand-hover text-black font-bold px-6 py-2.5 rounded-full shrink-0 transition-colors"
          >
            Sign in with Google
          </button>
        </div>
      )}

      {/* Filter pills — matches the Figma Home pattern (category pills at the
          top), scoped to Spotifusion's real content types instead of the
          reference's Music/Podcasts/Audiobooks, which don't exist here. */}
      <div className="flex items-center gap-2 mb-6 overflow-x-auto scrollbar-none">
        {FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            aria-pressed={filter === f}
            className={`shrink-0 text-sm font-semibold px-4 py-2 rounded-[8px] transition-colors ${
              filter === f ? 'bg-brand text-white' : 'bg-surface-elevated text-text hover:bg-surface-hover'
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      {/* Featured section matching design reference */}
      <section className="mb-8">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg md:text-xl font-bold text-white">Featured</h2>
          <span className="text-xs font-semibold text-brand cursor-pointer hover:underline">See all</span>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          {FEATURED_ITEMS.map((item) => (
            <div
              key={item.id}
              className={`relative aspect-video rounded-xl bg-gradient-to-br ${item.gradient} p-4 flex flex-col justify-end text-white shadow-lg overflow-hidden cursor-pointer hover:scale-[1.02] transition-transform`}
            >
              <div className="absolute inset-0 bg-black/20" />
              <div className="relative z-10">
                <p className="font-black text-sm md:text-base leading-tight drop-shadow">{item.title}</p>
                <p className={`text-xs ${item.textCol} font-medium`}>{item.subtitle}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {user && recentLoading && (
        <section className="mb-10">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3" aria-hidden="true">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="skeleton h-[58px] rounded-[10px]" />
            ))}
          </div>
        </section>
      )}

      {user && !recentLoading && recent.length > 0 && (
        <section className="mb-10">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 md:hidden">
            {recent.map((track) => (
              <QuickAccessCard key={track.id} track={track} onPlay={() => player.playTrack(track, recent)} />
            ))}
          </div>
          <div className="hidden md:block">
            <SectionHeader title="Recently Played" />
            <div className="grid grid-cols-3 lg:grid-cols-5 xl:grid-cols-6 gap-5">
              {recent.map((track) => (
                <SquareCard
                  key={track.id}
                  image={track.thumbnail}
                  title={track.title}
                  subtitle={track.artist}
                  onPlay={() => player.playTrack(track, recent)}
                />
              ))}
            </div>
          </div>
        </section>
      )}

      {user && showPlaylists && playlistsLoading && (
        <section className="mb-10">
          <SectionHeader title="Your Playlists" />
          <SkeletonCardGrid
            count={5}
            className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5"
          />
        </section>
      )}

      {user && showPlaylists && !playlistsLoading && playlists.length > 0 && (
        <section className="mb-10">
          <SectionHeader title="Your Playlists" />
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5">
            {playlists.map((p) => (
              <SquareCard
                key={p.id}
                to={`/playlist/${p.id}`}
                title={p.name}
                subtitle={`${p.trackIds?.length || 0} songs`}
                onPlay={
                  p.trackIds?.length
                    ? () => player.playTrack(p.tracks?.[p.trackIds[0]], Object.values(p.tracks || {}))
                    : undefined
                }
              />
            ))}
          </div>
        </section>
      )}

      {showLocal && localSongs.length > 0 && (
        <section className="mb-10">
          <SectionHeader title="Local Files" />
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5">
            {localSongs.slice(0, 10).map((song) => (
              <SquareCard
                key={song.id}
                image={song.thumbnail}
                title={song.title}
                subtitle={song.artist}
                onPlay={() => player.playTrack(song, localSongs)}
              />
            ))}
          </div>
        </section>
      )}

      {nothingAtAll && localSongs.length === 0 && (
        <p className="text-text-muted text-sm">
          Search for a song to start listening, or add local files — they'll show up here.
        </p>
      )}

      {!user && localSongs.length === 0 && (
        <p className="text-text-muted text-sm">
          Or add your own audio files from the Local Files tab — no account needed.
        </p>
      )}
    </div>
  )
}
