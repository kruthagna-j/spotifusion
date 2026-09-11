import { Routes, Route, useLocation } from 'react-router-dom'
import EmergentShell from '@/components/EmergentShell'
import Home from '@/pages/Home'
import Search from '@/pages/Search'
import LibraryMobile from '@/pages/LibraryMobile'
import LikedSongs from '@/pages/LikedSongs'
import Playlist from '@/pages/Playlist'
import Playlists from '@/pages/Playlists'
import LocalFiles from '@/pages/LocalFiles'
import Settings from '@/pages/Settings'
import RecentlyPlayed from '@/pages/RecentlyPlayed'
import Queue from '@/pages/Queue'
import Artist from '@/pages/Artist'
import Album from '@/pages/Album'
import Equalizer from '@/pages/Equalizer'
import NowPlayingRoute from '@/pages/NowPlayingRoute'
import Lyrics from '@/pages/Lyrics'
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts'

export default function App() {
  useKeyboardShortcuts()
  const location = useLocation()
  const immersive = ['/player', '/lyrics'].includes(location.pathname)

  return (
    <EmergentShell>
      <div className={immersive ? 'emergent-route emergent-route--immersive' : 'emergent-route'}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/search" element={<Search />} />
          <Route path="/library" element={<LibraryMobile />} />
          <Route path="/liked-songs" element={<LikedSongs />} />
          <Route path="/playlist/:id" element={<Playlist />} />
          <Route path="/playlists" element={<Playlists />} />
          <Route path="/local-files" element={<LocalFiles />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/recently-played" element={<RecentlyPlayed />} />
          <Route path="/queue" element={<Queue />} />
          <Route path="/artist/:name" element={<Artist />} />
          <Route path="/album/:name" element={<Album />} />
          <Route path="/equalizer" element={<Equalizer />} />
          <Route path="/player" element={<NowPlayingRoute />} />
          <Route path="/lyrics" element={<Lyrics />} />
        </Routes>
      </div>
    </EmergentShell>
  )
}
