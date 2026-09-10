import { Routes, Route, useLocation } from 'react-router-dom'
import Sidebar from '@/components/Sidebar'
import TopBar from '@/components/TopBar'
import PlayerBar from '@/components/PlayerBar'
import MobileNav from '@/components/MobileNav'
import AuxPane from '@/components/AuxPane'
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
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { useAuxPane } from '@/context/AuxPaneContext'
import { usePlayer } from '@/context/PlayerContext'

const IMMERSIVE_ROUTES = ['/player', '/lyrics']

export default function App() {
  useKeyboardShortcuts()
  const location = useLocation()
  const online = useOnlineStatus()
  const { auxOpen } = useAuxPane()
  const { currentTrack } = usePlayer()
  const immersive = IMMERSIVE_ROUTES.includes(location.pathname)

  return (
    <div className={`sf-app-shell ${immersive ? 'sf-app-shell--immersive' : ''}`}>
      {!online && !immersive && (
        <div className="sf-offline-banner">
          You're offline. Your downloaded/local songs are still available — online search and streaming need a connection.
        </div>
      )}
      <div className="sf-shell-body">
        {!immersive && <Sidebar />}
        <div className="sf-content-column">
          {!immersive && <TopBar />}
          <main className={`sf-route-main ${immersive ? 'sf-route-main--immersive' : ''}`}>
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
          </main>
        </div>
        {!immersive && currentTrack && auxOpen && <AuxPane />}
      </div>
      {!immersive && <PlayerBar />}
      {!immersive && <MobileNav />}
    </div>
  )
}
