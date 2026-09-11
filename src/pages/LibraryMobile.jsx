import { useState } from 'react'
import { Plus, Heart, ListMusic, FolderOpen, Music2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists, useLikedSongs } from '@/hooks/useLibraryData'
import { createPlaylist } from '@/lib/library'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'
import { useNavigate, Link } from 'react-router-dom'

const TABS = ['Playlists', 'Liked Songs', 'Local Files']

export default function LibraryMobile() {
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const liked = useLikedSongs(user?.uid)
  const [localSongs] = useLocalSongs()
  const player = usePlayer()
  const navigate = useNavigate()
  const [tab, setTab] = useState('Playlists')
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  async function handleCreate() {
    if (!user || !name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    setCreating(false); setName(''); navigate(`/playlist/${id}`)
  }

  return <div className="emergent-library">
    <header className="emergent-screen-header"><div><h1>Your Library</h1><p>Everything you saved, in one place</p></div>{user && <button className="emergent-header-button" onClick={() => setCreating(true)} aria-label="Create playlist"><Plus size={20} /></button>}</header>
    <div className="emergent-library-tabs">{TABS.map((item) => <button key={item} className={tab === item ? 'is-active' : ''} onClick={() => setTab(item)}>{item}</button>)}</div>
    {!user && <section className="emergent-auth-card"><div><strong>Sign in to build your library</strong><p>Save liked songs and create playlists across devices.</p></div><button onClick={signIn}>Sign in</button></section>}
    {creating && <div className="emergent-create-row"><input autoFocus value={name} onChange={(event) => setName(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && handleCreate()} placeholder="Playlist name" /><button onClick={handleCreate}>Create</button></div>}
    {tab === 'Playlists' && <div className="emergent-library-list"><Link to="/liked-songs" className="emergent-library-special"><span className="emergent-library-icon emergent-library-icon--liked"><Heart size={19} fill="currentColor" /></span><span><strong>Liked Songs</strong><small>{liked.length} saved tracks</small></span></Link>{playlists.map((playlist) => <Link key={playlist.id} to={`/playlist/${playlist.id}`} className="emergent-library-row"><span className="emergent-library-icon"><ListMusic size={19} /></span><span><strong>{playlist.name}</strong><small>{playlist.trackIds?.length || 0} songs</small></span></Link>)}{!playlists.length && <div className="emergent-empty-card"><Music2 size={24} /><span>Create a playlist to organize your music.</span></div>}</div>}
    {tab === 'Liked Songs' && <div className="emergent-library-list">{liked.map((track) => <div key={track.id} className="emergent-library-row" onClick={() => player.playTrack(track, liked)}><span className="emergent-library-track-art"><img src={track.thumbnail} alt="" /></span><span><strong>{track.title}</strong><small>{track.artist}</small></span></div>)}{!liked.length && <div className="emergent-empty-card"><Heart size={24} /><span>Liked songs will appear here.</span></div>}</div>}
    {tab === 'Local Files' && <div className="emergent-library-list"><Link to="/local-files" className="emergent-library-special"><span className="emergent-library-icon"><FolderOpen size={19} /></span><span><strong>Device Audio Files</strong><small>{localSongs.length} tracks on this device</small></span></Link>{localSongs.map((track) => <div key={track.id} className="emergent-library-row" onClick={() => player.playTrack(track, localSongs)}><span className="emergent-library-track-art"><img src={track.thumbnail} alt="" /></span><span><strong>{track.title}</strong><small>{track.artist}</small></span></div>)}{!localSongs.length && <div className="emergent-empty-card"><FolderOpen size={24} /><span>Open local audio files to start listening.</span></div>}</div>}
  </div>
}
