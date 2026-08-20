import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Play, Trash2 } from 'lucide-react'
import { doc, onSnapshot } from 'firebase/firestore'
import { db } from '@/lib/firebase'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import { removeTrackFromPlaylist, deletePlaylist } from '@/lib/library'
import { useNavigate } from 'react-router-dom'

export default function Playlist() {
  const { id } = useParams()
  const { user } = useAuth()
  const player = usePlayer()
  const navigate = useNavigate()
  const [playlist, setPlaylist] = useState(null)

  useEffect(() => {
    if (!user) return
    return onSnapshot(doc(db, `users/${user.uid}/playlists/${id}`), (snap) => {
      setPlaylist(snap.exists() ? { id: snap.id, ...snap.data() } : null)
    })
  }, [user, id])

  if (!playlist) {
    return <div className="p-6 text-text-muted">Playlist not found.</div>
  }

  const tracks = (playlist.trackIds || []).map((tid) => playlist.tracks?.[tid]).filter(Boolean)

  return (
    <div>
      <div className="flex items-end gap-6 p-6 bg-gradient-to-b from-surface-elevated to-transparent">
        <div className="w-48 h-48 rounded-md shadow-2xl bg-surface-highlight flex items-center justify-center text-5xl shrink-0">
          🎵
        </div>
        <div>
          <p className="text-xs font-bold uppercase">Playlist</p>
          <h1 className="text-4xl md:text-6xl font-black my-2">{playlist.name}</h1>
          {playlist.description && <p className="text-text-muted text-sm mb-1">{playlist.description}</p>}
          <p className="text-text-muted text-sm">{tracks.length} songs</p>
        </div>
      </div>

      <div className="p-4 md:p-6">
        <div className="flex items-center gap-4 mb-6">
          {tracks.length > 0 && (
            <button
              onClick={() => player.playTrack(tracks[0], tracks)}
              className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center hover:scale-105 transition-transform"
            >
              <Play size={24} className="ml-1" />
            </button>
          )}
          <button
            onClick={async () => {
              await deletePlaylist(user.uid, id)
              navigate('/')
            }}
            className="text-text-muted hover:text-red-400 text-sm flex items-center gap-1"
          >
            <Trash2 size={16} /> Delete playlist
          </button>
        </div>

        {tracks.length === 0 && (
          <p className="text-text-muted text-sm">
            Search for songs and add them to this playlist from the "..." menu.
          </p>
        )}

        {tracks.map((track, i) => (
          <div key={track.id} className="group flex items-center gap-4">
            <div className="flex-1">
              <TrackRowLite track={track} index={i} contextTracks={tracks} player={player} />
            </div>
            <button
              onClick={() => removeTrackFromPlaylist(user.uid, id, track.id)}
              className="opacity-0 group-hover:opacity-100 text-text-muted hover:text-red-400 pr-4"
            >
              <Trash2 size={16} />
            </button>
          </div>
        ))}
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
