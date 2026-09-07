import { useParams, useNavigate } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import {
  Play,
  Shuffle,
  Trash2,
  Music,
  Search,
  X,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Pencil,
} from 'lucide-react'
import { doc, onSnapshot } from 'firebase/firestore'
import { db } from '@/lib/firebase'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import {
  removeTrackFromPlaylist,
  deletePlaylist,
  reorderPlaylistTracks,
  updatePlaylistDetails,
} from '@/lib/library'
import { SkeletonRowList } from '@/components/Skeleton'

const SORT_OPTIONS = [
  { key: 'custom', label: 'Custom order' },
  { key: 'title', label: 'Title' },
  { key: 'artist', label: 'Artist' },
  { key: 'album', label: 'Album' },
  { key: 'recent', label: 'Recently added' },
]

function sortTracks(tracks, sortKey) {
  if (sortKey === 'custom') return tracks
  const sorted = [...tracks]
  if (sortKey === 'title') sorted.sort((a, b) => a.title.localeCompare(b.title))
  else if (sortKey === 'artist') sorted.sort((a, b) => a.artist.localeCompare(b.artist))
  else if (sortKey === 'album') {
    sorted.sort((a, b) => {
      if (!a.album && !b.album) return 0
      if (!a.album) return 1 // no-album tracks sort last, not fabricated to the top
      if (!b.album) return -1
      return a.album.localeCompare(b.album)
    })
  } else if (sortKey === 'recent') {
    // Tracks added before this feature shipped have no addedAt — treated as
    // oldest (0) rather than guessed, so they sort after anything with a
    // real timestamp instead of randomly interleaving.
    sorted.sort((a, b) => (b.addedAt || 0) - (a.addedAt || 0))
  }
  return sorted
}

export default function Playlist() {
  const { id } = useParams()
  const { user } = useAuth()
  const player = usePlayer()
  const navigate = useNavigate()
  const [playlist, setPlaylist] = useState(null)
  const [loading, setLoading] = useState(true)
  const [sortKey, setSortKey] = useState('custom')
  const [sortMenuOpen, setSortMenuOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [findQuery, setFindQuery] = useState('')
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')

  useEffect(() => {
    if (!user) {
      setLoading(false)
      return
    }
    setLoading(true)
    return onSnapshot(doc(db, `users/${user.uid}/playlists/${id}`), (snap) => {
      setPlaylist(snap.exists() ? { id: snap.id, ...snap.data() } : null)
      setLoading(false)
    })
  }, [user, id])

  const allTracks = useMemo(
    () => (playlist?.trackIds || []).map((tid) => playlist.tracks?.[tid]).filter(Boolean),
    [playlist]
  )

  const filteredTracks = useMemo(() => {
    const q = findQuery.trim().toLowerCase()
    if (!q) return allTracks
    return allTracks.filter(
      (t) => t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q)
    )
  }, [allTracks, findQuery])

  const visibleTracks = useMemo(() => sortTracks(filteredTracks, sortKey), [filteredTracks, sortKey])

  if (!user) {
    return <div className="p-6 text-text-muted text-sm">Sign in to view this playlist.</div>
  }

  if (loading) {
    return (
      <div>
        <div className="flex items-end gap-6 p-6">
          <div className="skeleton w-32 h-32 md:w-48 md:h-48 rounded-md shrink-0" />
          <div className="flex flex-col gap-3 flex-1">
            <div className="skeleton h-3 w-16 rounded" />
            <div className="skeleton h-10 w-2/3 rounded" />
          </div>
        </div>
        <div className="p-4 md:p-6">
          <SkeletonRowList count={5} />
        </div>
      </div>
    )
  }

  if (!playlist) {
    return <div className="p-6 text-text-muted">Playlist not found.</div>
  }

  function playShuffled() {
    if (!allTracks.length) return
    const shuffled = [...allTracks].sort(() => Math.random() - 0.5)
    if (!player.shuffle) player.toggleShuffle()
    player.playTrack(shuffled[0], shuffled)
  }

  function moveTrack(fromIndex, toIndex) {
    if (toIndex < 0 || toIndex >= allTracks.length) return
    const nextIds = [...playlist.trackIds]
    const [moved] = nextIds.splice(fromIndex, 1)
    nextIds.splice(toIndex, 0, moved)
    reorderPlaylistTracks(user.uid, id, nextIds)
  }

  return (
    <div>
      <div className="flex items-end gap-6 p-6 bg-gradient-to-b from-surface-elevated to-transparent">
        <div className="w-32 h-32 md:w-48 md:h-48 rounded-md shadow-card bg-surface-highlight flex items-center justify-center text-5xl shrink-0">
          🎵
        </div>
        <div>
          <p className="text-xs font-bold uppercase">Playlist</p>
          <button
            onClick={() => {
              setEditName(playlist.name)
              setEditDescription(playlist.description || '')
              setEditing(true)
            }}
            className="group flex items-center gap-2 text-left"
          >
            <h1 className="text-3xl md:text-6xl font-black my-2">{playlist.name}</h1>
            <Pencil size={20} className="text-text-muted opacity-0 group-hover:opacity-100 shrink-0" />
          </button>
          {playlist.description && <p className="text-text-muted text-sm mb-1">{playlist.description}</p>}
          <p className="text-text-muted text-sm">{allTracks.length} songs</p>
        </div>
      </div>

      {editing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="bg-surface-elevated rounded-lg p-5 w-full max-w-sm">
            <h2 className="font-bold mb-4">Name & details</h2>
            <label className="block text-xs font-semibold text-text-muted mb-1">Name</label>
            <input
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="w-full bg-surface-highlight rounded px-3 py-2 text-sm outline-none mb-3"
              autoFocus
            />
            <label className="block text-xs font-semibold text-text-muted mb-1">Description</label>
            <textarea
              value={editDescription}
              onChange={(e) => setEditDescription(e.target.value)}
              placeholder="Add an optional description"
              rows={3}
              className="w-full bg-surface-highlight rounded px-3 py-2 text-sm outline-none mb-4 resize-none"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setEditing(false)}
                className="px-4 py-2 text-sm font-semibold rounded-full hover:bg-surface-hover"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  if (!editName.trim()) return
                  await updatePlaylistDetails(user.uid, id, {
                    name: editName.trim(),
                    description: editDescription.trim(),
                  })
                  setEditing(false)
                }}
                disabled={!editName.trim()}
                className="px-4 py-2 text-sm font-bold rounded-full bg-brand text-black disabled:opacity-50"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="p-4 md:p-6">
        <div className="flex items-center gap-4 mb-4 flex-wrap">
          {allTracks.length > 0 && (
            <>
              <button
                onClick={() => player.playTrack(allTracks[0], allTracks)}
                aria-label={`Play ${playlist.name}`}
                className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center hover:scale-105 hover:bg-brand-hover transition-transform"
              >
                <Play size={24} className="ml-1" />
              </button>
              <button
                onClick={playShuffled}
                aria-label="Shuffle play"
                aria-pressed={player.shuffle}
                title="Shuffle play"
                className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                  player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'
                }`}
              >
                <Shuffle size={22} />
              </button>
            </>
          )}

          {allTracks.length > 0 && (
            <button
              onClick={() => setSearchOpen((o) => !o)}
              aria-label="Find in playlist"
              aria-expanded={searchOpen}
              className={searchOpen ? 'text-brand' : 'text-text-muted hover:text-text'}
            >
              <Search size={20} />
            </button>
          )}

          {allTracks.length > 0 && (
            <div className="relative">
              <button
                onClick={() => setSortMenuOpen((o) => !o)}
                aria-label="Sort playlist"
                aria-expanded={sortMenuOpen}
                className="flex items-center gap-1 text-sm text-text-muted hover:text-text"
              >
                <ArrowUpDown size={16} />
                {SORT_OPTIONS.find((o) => o.key === sortKey).label}
              </button>
              {sortMenuOpen && (
                <div className="absolute left-0 top-full mt-2 w-48 bg-surface-elevated rounded-md shadow-card border border-border z-30 overflow-hidden">
                  {SORT_OPTIONS.map((opt) => (
                    <button
                      key={opt.key}
                      onClick={() => {
                        setSortKey(opt.key)
                        setSortMenuOpen(false)
                      }}
                      className={`w-full text-left px-3 py-2 text-sm hover:bg-surface-hover ${
                        sortKey === opt.key ? 'text-brand' : 'text-text'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          <button
            onClick={async () => {
              await deletePlaylist(user.uid, id)
              navigate('/')
            }}
            className="text-text-muted hover:text-red-400 text-sm flex items-center gap-1 ml-auto"
          >
            <Trash2 size={16} /> Delete playlist
          </button>
        </div>

        {searchOpen && (
          <div className="relative max-w-xs mb-4">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued" aria-hidden="true" />
            <input
              autoFocus
              value={findQuery}
              onChange={(e) => setFindQuery(e.target.value)}
              placeholder="Find in playlist"
              aria-label="Find in playlist"
              className="w-full bg-surface-highlight rounded-full pl-8 pr-8 py-1.5 text-xs outline-none"
            />
            {findQuery && (
              <button
                onClick={() => setFindQuery('')}
                aria-label="Clear search"
                className="absolute right-2 top-1/2 -translate-y-1/2 text-text-subdued hover:text-text"
              >
                <X size={14} />
              </button>
            )}
          </div>
        )}

        {allTracks.length === 0 && (
          <div className="text-center py-16">
            <Music size={40} className="mx-auto mb-3 text-text-subdued" aria-hidden="true" />
            <p className="text-text-muted text-sm">
              Search for songs and add them to this playlist from the "..." menu.
            </p>
          </div>
        )}

        {allTracks.length > 0 && visibleTracks.length === 0 && (
          <p className="text-text-muted text-sm">No songs in this playlist match "{findQuery}".</p>
        )}

        {visibleTracks.map((track, i) => {
          // Reorder controls only make sense in Custom order, against the
          // real underlying index — not the filtered/sorted display index.
          const realIndex = playlist.trackIds.indexOf(track.id)
          return (
            <div key={track.id} className="group flex items-center gap-1">
              <div className="flex-1 min-w-0">
                <TrackRowLite track={track} index={i} contextTracks={visibleTracks} player={player} />
              </div>
              {sortKey === 'custom' && !findQuery && (
                <>
                  <button
                    onClick={() => moveTrack(realIndex, realIndex - 1)}
                    disabled={realIndex === 0}
                    aria-label={`Move ${track.title} up`}
                    className="opacity-0 group-hover:opacity-100 disabled:opacity-0 p-1 text-text-muted hover:text-text"
                  >
                    <ArrowUp size={14} />
                  </button>
                  <button
                    onClick={() => moveTrack(realIndex, realIndex + 1)}
                    disabled={realIndex === allTracks.length - 1}
                    aria-label={`Move ${track.title} down`}
                    className="opacity-0 group-hover:opacity-100 disabled:opacity-0 p-1 text-text-muted hover:text-text"
                  >
                    <ArrowDown size={14} />
                  </button>
                </>
              )}
              <button
                onClick={() => removeTrackFromPlaylist(user.uid, id, track.id)}
                aria-label={`Remove ${track.title} from playlist`}
                className="opacity-0 group-hover:opacity-100 text-text-muted hover:text-red-400 p-1 mr-3"
              >
                <Trash2 size={16} />
              </button>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function TrackRowLite({ track, index, contextTracks, player }) {
  const isCurrent = player.currentTrack?.id === track.id
  return (
    <div
      onDoubleClick={() => player.playTrack(track, contextTracks)}
      className={`grid grid-cols-[24px_1fr_auto] items-center gap-4 px-4 py-2 rounded-md hover:bg-surface-hover cursor-default ${
        isCurrent ? 'text-brand' : 'text-text-muted'
      }`}
    >
      <span className="text-sm text-center">{index + 1}</span>
      <div className="flex items-center gap-3 min-w-0">
        <img src={track.thumbnail} alt="" className="w-10 h-10 rounded object-cover shrink-0" />
        <div className="min-w-0">
          <p className={`text-sm truncate ${isCurrent ? 'text-brand' : 'text-text'}`}>{track.title}</p>
          <p className="text-xs text-text-subdued truncate">{track.artist}</p>
        </div>
      </div>
      <div />
    </div>
  )
}
