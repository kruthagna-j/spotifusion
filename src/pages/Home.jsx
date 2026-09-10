import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronRight, FolderOpen, Heart, MoreVertical, Play, Plus, Search, Settings as SettingsIcon, Sparkles, Clock } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus, usePlaylistsStatus } from '@/hooks/useLibraryData'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'
import Logo from '@/components/Logo'

const CATEGORY_PILLS = ['All', 'Songs', 'Albums', 'Artists', 'Jukebox']
const FEATURED_ITEMS = [
  { id: 'f-1', title: 'Chill Vibes', subtitle: 'Playlist', gradient: 'linear-gradient(135deg,#0891b2,#2563eb)' },
  { id: 'f-2', title: 'Top 50', subtitle: 'YouTube', gradient: 'linear-gradient(135deg,#e11d48,#d97706)' },
  { id: 'f-3', title: 'Trending Now', subtitle: 'Playlist', gradient: 'linear-gradient(135deg,#7e22ce,#db2777)' },
]

export default function Home() {
  const navigate = useNavigate()
  const { user, signIn } = useAuth()
  const { data: recent = [] } = useRecentlyPlayedStatus(user?.uid, 8)
  const { data: playlists = [] } = usePlaylistsStatus(user?.uid)
  const [localSongs] = useLocalSongs()
  const player = usePlayer()
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [activeMenu, setActiveMenu] = useState(null)

  function selectCategory(category) {
    setSelectedCategory(category)
    if (category === 'Jukebox') navigate('/player')
    else if (category !== 'All') navigate(`/search?category=${category}`)
  }

  return (
    <div className="sf-emergent-home" data-testid="home-screen-view">
      <header className="sf-emergent-screen-header">
        <div><h1>Spotifusion</h1><p>Stream YouTube Music & Local Audio</p></div>
        <button type="button" onClick={() => navigate('/settings')} aria-label="Settings"><SettingsIcon size={19} /></button>
      </header>

      <button type="button" className="sf-emergent-search-banner" onClick={() => navigate('/search')}><Search size={18} /><span>Search songs, artists, albums...</span></button>

      <div className="sf-emergent-category-row" aria-label="Music categories">
        {CATEGORY_PILLS.map((category) => <button key={category} type="button" className={selectedCategory === category ? 'is-active' : ''} onClick={() => selectCategory(category)}>{category}</button>)}
      </div>

      <div className="sf-emergent-local-banner">
        <div className="sf-emergent-local-copy"><span className="sf-emergent-local-icon"><FolderOpen size={19} /></span><div><strong>Play Local Device Audio <small>No upload needed</small></strong><p>{localSongs.length ? `${localSongs.length} local track(s) ready` : 'Open MP3/WAV files on your device'}</p></div></div>
        <button type="button" onClick={() => navigate('/local-files')}>Open Files</button>
      </div>

      <section className="sf-emergent-home-section">
        <div className="sf-emergent-section-heading"><h2>Featured</h2><Link to="/playlists">See all <ChevronRight size={14} /></Link></div>
        <div className="sf-emergent-featured-row">{FEATURED_ITEMS.map((item) => <button key={item.id} type="button" className="sf-emergent-featured-card" style={{ background: item.gradient }} onClick={() => navigate('/playlists')}><span><strong>{item.title}</strong><small>{item.subtitle}</small></span></button>)}</div>
      </section>

      {(recent.length > 0 || playlists.length > 0 || localSongs.length > 0) && <section className="sf-emergent-home-section">
        <div className="sf-emergent-section-heading"><h2>Recently Played</h2><span>Jump back in</span></div>
        <div className="sf-emergent-track-list">{recent.slice(0, 6).map((track) => <EmergentTrack key={track.id} track={track} player={player} isCurrent={player.currentTrack?.id === track.id} menuOpen={activeMenu === track.id} onMenu={() => setActiveMenu(activeMenu === track.id ? null : track.id)} />)}</div>
      </section>}

      {user && recent.length === 0 && playlists.length === 0 && localSongs.length === 0 && <div className="sf-emergent-empty-home"><Clock size={28} /><p>Search for a song to start listening, or add local files — they’ll show up here.</p></div>}
      {!user && localSongs.length === 0 && <div className="sf-emergent-empty-home"><p>Or add your own audio files from the Local Files tab — no account needed.</p>{<button type="button" onClick={signIn} className="sf-emergent-inline-auth">Sign in to sync your library</button>}</div>}
    </div>
  )
}

function EmergentTrack({ track, player, isCurrent, menuOpen, onMenu }) {
  const [liked, setLiked] = useState(false)
  return <div className={`sf-emergent-track-row ${isCurrent ? 'is-current' : ''}`} onClick={() => player.playTrack(track)}>
    <div className="sf-emergent-track-art">{track.thumbnail && <img src={track.thumbnail} alt="" />}{isCurrent && <span>{player.isPlaying ? '•••' : <Play size={15} fill="currentColor" />}</span>}</div>
    <div className="sf-emergent-track-copy"><strong>{track.title}</strong><small>{track.artist}</small></div>
    <div className="sf-emergent-track-actions" onClick={(event) => event.stopPropagation()}><button type="button" onClick={() => setLiked(!liked)} className={liked ? 'is-liked' : ''} aria-label="Like track"><Heart size={16} fill={liked ? 'currentColor' : 'none'} /></button><button type="button" onClick={onMenu} aria-label="Track menu"><MoreVertical size={16} /></button>{menuOpen && <div className="sf-emergent-track-menu"><button type="button" onClick={() => player.enqueue(track)}><Plus size={14} /> Add to Queue</button><button type="button" onClick={() => player.openNowPlaying()}><Sparkles size={14} /> Open Player</button></div>}</div>
  </div>
}
