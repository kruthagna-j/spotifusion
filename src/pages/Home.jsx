import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, SlidersHorizontal, Play, MoreVertical, ChevronRight, FolderOpen, Heart, Plus } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus, usePlaylistsStatus } from '@/hooks/useLibraryData'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'

const CATEGORIES = ['All', 'Songs', 'Albums', 'Artists', 'Jukebox']

export default function Home() {
  const { user, signIn } = useAuth()
  const { data: recent = [], loading: recentLoading } = useRecentlyPlayedStatus(user?.uid, 8)
  const { data: playlists = [], loading: playlistsLoading } = usePlaylistsStatus(user?.uid)
  const [localSongs] = useLocalSongs()
  const player = usePlayer()
  const [category, setCategory] = useState('All')
  const [menuId, setMenuId] = useState(null)

  const tracks = recent.length ? recent : localSongs
  const play = (track, list = tracks) => player.playTrack(track, list)

  return (
    <div className="emergent-home" data-testid="home-screen-view">
      <header className="emergent-screen-header">
        <div><h1>Spotifusion</h1><p>Your music, your way</p></div>
        <Link to="/settings" className="emergent-header-button" aria-label="Settings"><SlidersHorizontal size={18} /></Link>
      </header>
      <Link to="/search" className="emergent-search-banner"><Search size={18} /><span>Search songs, artists, albums...</span></Link>
      <div className="emergent-pill-row">
        {CATEGORIES.map((item) => <button key={item} className={category === item ? 'is-active' : ''} onClick={() => item === 'Jukebox' ? player.openNowPlaying() : setCategory(item)}>{item}</button>)}
      </div>
      <section className="emergent-local-banner">
        <div className="emergent-local-copy"><div className="emergent-local-icon"><FolderOpen size={20} /></div><div><strong>Play Local Device Audio <small>No upload needed</small></strong><p>{localSongs.length ? `${localSongs.length} local tracks ready` : 'Open MP3/WAV files on your device'}</p></div></div>
        <Link to="/local-files" className="emergent-action-button">Open Files</Link>
      </section>
      <section className="emergent-section">
        <div className="emergent-section-heading"><h2>Featured playlists</h2><Link to="/playlists">See all <ChevronRight size={14} /></Link></div>
        <div className="emergent-card-row">
          {playlistsLoading && <div className="emergent-skeleton-card" />}
          {!playlistsLoading && playlists.length === 0 && <Link to="/library" className="emergent-feature-card emergent-feature-card--empty"><Plus size={24} /><strong>Create your first playlist</strong></Link>}
          {playlists.slice(0, 6).map((playlist) => <Link key={playlist.id} to={`/playlist/${playlist.id}`} className="emergent-feature-card"><div className="emergent-feature-art">♫</div><strong>{playlist.name}</strong><small>{playlist.trackIds?.length || 0} songs</small></Link>)}
        </div>
      </section>
      <section className="emergent-section">
        <div className="emergent-section-heading"><h2>Recently played</h2><span>{recentLoading ? 'Loading...' : 'Jump back in'}</span></div>
        <div className="emergent-track-list">
          {tracks.slice(0, 8).map((track) => <div key={track.id} className={`emergent-track-row ${player.currentTrack?.id === track.id ? 'is-current' : ''}`} onClick={() => play(track)}><button className="emergent-track-art" aria-label={`Play ${track.title}`}><img src={track.thumbnail} alt="" /><Play size={15} fill="currentColor" /></button><div className="emergent-track-copy"><strong>{track.title}</strong><small>{track.artist}</small></div><button className="emergent-icon-action" onClick={(event) => { event.stopPropagation(); player.toggleLikeTrack(track) }} aria-label="Like track"><Heart size={16} /></button><button className="emergent-icon-action" onClick={(event) => { event.stopPropagation(); setMenuId(menuId === track.id ? null : track.id) }} aria-label="Track menu"><MoreVertical size={16} /></button>{menuId === track.id && <div className="emergent-track-menu"><button onClick={() => player.openAddToPlaylist(track)}><Plus size={14} /> Add to playlist</button><Link to="/lyrics" onClick={() => play(track)}><span>♪</span> View lyrics</Link></div>}</div>)}
          {!tracks.length && <div className="emergent-empty-card">Search for music or add local audio to start listening.</div>}
        </div>
      </section>
      {!user && <section className="emergent-auth-card"><div><strong>Build your library</strong><p>Sign in to save liked songs, playlists, and listening history.</p></div><button onClick={signIn}>Sign in</button></section>}
    </div>
  )
}
