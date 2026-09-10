import { useState } from 'react'
import { ChevronRight, FolderOpen, Heart, ListMusic, Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists, useLikedSongs } from '@/hooks/useLibraryData'
import { createPlaylist } from '@/lib/library'
import { useLocalSongs } from '@/lib/localMusicDb'

const LIBRARY_TABS = ['Playlists', 'Artists', 'Albums', 'Songs', 'Local Files']

export default function LibraryMobile() {
  const navigate = useNavigate()
  const { user, signIn } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const likedSongs = useLikedSongs(user?.uid)
  const [localSongs] = useLocalSongs()
  const [activeTab, setActiveTab] = useState('Playlists')
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  async function handleCreate(event) {
    event.preventDefault()
    if (!user || !name.trim()) return
    const id = await createPlaylist(user.uid, name.trim())
    setCreating(false)
    setName('')
    navigate(`/playlist/${id}`)
  }

  if (!user) return <div className="sf-emergent-library"><div className="sf-emergent-screen-header"><div><h1>Library</h1><p>Your personal collection</p></div><button type="button" onClick={signIn} aria-label="Sign in"><Plus size={19} /></button></div><div className="sf-emergent-auth-card"><p>Sign in to create playlists, like songs, and sync your library.</p><button type="button" onClick={signIn}>Sign in with Google</button></div><LibraryTabs activeTab={activeTab} setActiveTab={setActiveTab} navigate={navigate} /></div>

  return <div className="sf-emergent-library">
    <div className="sf-emergent-screen-header"><div><h1>Library</h1><p>Your personal collection</p></div><button type="button" onClick={() => setCreating(true)} aria-label="Create playlist"><Plus size={19} /></button></div>
    <LibraryTabs activeTab={activeTab} setActiveTab={setActiveTab} navigate={navigate} />
    {creating && <form className="sf-emergent-create-box" onSubmit={handleCreate}><strong>Create New Playlist</strong><input autoFocus value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Midnight Beats 2026" /><div><button type="button" onClick={() => setCreating(false)}>Cancel</button><button type="submit">Create</button></div></form>}
    <div className="sf-emergent-library-list">
      <Link to="/liked-songs" className="sf-emergent-library-row sf-emergent-library-row--liked"><span className="sf-emergent-library-icon"><Heart size={21} fill="currentColor" /></span><span><strong>Liked Songs</strong><small>{likedSongs.length} songs</small></span><ChevronRight size={18} /></Link>
      <button type="button" className="sf-emergent-library-row" onClick={() => navigate('/local-files')}><span className="sf-emergent-library-icon sf-emergent-library-icon--local"><FolderOpen size={21} /></span><span><strong>Device Audio Files</strong><small>{localSongs.length} tracks</small></span><ChevronRight size={18} /></button>
      {playlists.map((playlist) => <Link key={playlist.id} to={`/playlist/${playlist.id}`} className="sf-emergent-library-row"><span className="sf-emergent-library-icon"><ListMusic size={21} /></span><span><strong>{playlist.name}</strong><small>{playlist.trackIds?.length || 0} songs</small></span><ChevronRight size={18} /></Link>)}
    </div>
    <button type="button" className="sf-emergent-create-pill" onClick={() => setCreating(true)}><Plus size={18} /> + Create Playlist</button>
  </div>
}

function LibraryTabs({ activeTab, setActiveTab, navigate }) {
  return <div className="sf-emergent-library-tabs">{LIBRARY_TABS.map((tab) => <button key={tab} type="button" className={activeTab === tab ? 'is-active' : ''} onClick={() => { setActiveTab(tab); if (tab === 'Local Files') navigate('/local-files') }}>{tab}</button>)}</div>
}
