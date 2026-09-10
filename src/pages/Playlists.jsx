import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ListMusic, Play, Plus, Search } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import { createPlaylist } from '@/lib/library'
import { usePlayer } from '@/context/PlayerContext'

export default function Playlists() {
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const player = usePlayer()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const visible = playlists.filter((playlist) => playlist.name.toLowerCase().includes(query.trim().toLowerCase()))

  async function handleCreate() {
    if (!user || !name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    setCreating(false)
    setName('')
    navigate(`/playlist/${id}`)
  }

  return (
    <div className="sf-playlists-page p-4 sm:p-6">
      <div className="flex items-center justify-between gap-3 mb-5">
        <div><p className="sf-eyebrow">Your collection</p><h1 className="text-2xl font-black tracking-tight">Playlists</h1></div>
        {user && <button className="sf-round-action" onClick={() => setCreating(true)} aria-label="Create playlist"><Plus size={20} /></button>}
      </div>
      {!user && <div className="sf-reference-card text-center"><p className="text-sm text-text-muted mb-4">Sign in to create and sync playlists.</p><button onClick={signIn} className="sf-primary-button">Sign in with Google</button></div>}
      {user && <>
        <label className="sf-reference-search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search playlists" aria-label="Search playlists" /></label>
        {creating && <div className="sf-create-row"><input autoFocus value={name} onChange={(event) => setName(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && handleCreate()} placeholder="Playlist name" /><button onClick={handleCreate} className="sf-primary-button">Create</button></div>}
        <div className="space-y-2">
          {visible.map((playlist) => <Link key={playlist.id} to={`/playlist/${playlist.id}`} className="sf-playlist-card"><span className="sf-library-icon"><ListMusic size={20} /></span><span className="min-w-0 flex-1"><strong>{playlist.name}</strong><small>Playlist • {playlist.trackIds?.length || 0} songs</small></span>{playlist.trackIds?.length > 0 && <button type="button" className="sf-list-play" aria-label={`Play ${playlist.name}`} onClick={(event) => { event.preventDefault(); const tracks = Object.values(playlist.tracks || {}); if (tracks[0]) player.playTrack(tracks[0], tracks) }}><Play size={14} fill="currentColor" /></button>}</Link>)}
          {!visible.length && <div className="sf-empty-card"><ListMusic size={30} /><p>No playlists yet.</p></div>}
        </div>
      </>}
    </div>
  )
}
