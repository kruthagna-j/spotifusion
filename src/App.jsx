import { Routes, Route, Link } from 'react-router-dom'
import Sidebar from '@/components/Sidebar'
import TopBar from '@/components/TopBar'
import PlayerBar from '@/components/PlayerBar'
import MobileNav from '@/components/MobileNav'
import Home from '@/pages/Home'
import Search from '@/pages/Search'
import LibraryMobile from '@/pages/LibraryMobile'
import LikedSongs from '@/pages/LikedSongs'
import Playlist from '@/pages/Playlist'
import LocalFiles from '@/pages/LocalFiles'
import Settings from '@/pages/Settings'
import RecentlyPlayed from '@/pages/RecentlyPlayed'
import Discover from '@/pages/Discover'
import Collection from '@/pages/Collection'
import NowPlayingRoute from '@/pages/NowPlayingRoute'
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'

export default function App() {
  useKeyboardShortcuts()
  const online = useOnlineStatus()

  return (
    <div className="h-screen flex flex-col bg-bg text-text overflow-hidden">
      {!online && (
        <div className="shrink-0 bg-yellow-600/90 text-black text-xs font-semibold text-center py-1.5 px-4">
          You're offline. Your downloaded/local songs are still available — online search and
          streaming need a connection.
        </div>
      )}
      <div className="flex flex-1 min-h-0">
        <Sidebar />

        <div className="flex-1 min-w-0 min-h-0 flex flex-col overflow-hidden">
          <TopBar />
          <main className="flex-1 min-h-0 overflow-y-auto scrollbar-none pb-28 md:pb-6">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/search" element={<Search />} />
              <Route path="/library" element={<LibraryMobile />} />
              <Route path="/liked-songs" element={<LikedSongs />} />
              <Route path="/playlist/:id" element={<Playlist />} />
              <Route path="/local-files" element={<LocalFiles />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/recently-played" element={<RecentlyPlayed />} />
              <Route path="/discover" element={<Discover />} />
              <Route path="/artist/:value" element={<Collection type="artist" />} />
              <Route path="/album/:value" element={<Collection type="album" />} />
              <Route path="/now-playing" element={<NowPlayingRoute />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </main>
        </div>
      </div>

      <PlayerBar />
      <MobileNav />
    </div>
  )
}

function NotFound() {
  return (
    <div className="min-h-full grid place-items-center p-8 text-center">
      <div className="max-w-md">
        <p className="text-brand text-xs font-black uppercase tracking-[.25em]">Spotifusion</p>
        <h1 className="text-5xl font-black mt-3">Page not found</h1>
        <p className="text-text-muted mt-3">That destination does not exist. Go back home and keep listening.</p>
        <Link to="/" className="inline-flex mt-6 bg-brand text-black font-black px-6 py-3 rounded-full">Back to Home</Link>
      </div>
    </div>
  )
}
