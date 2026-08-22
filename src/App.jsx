import { Routes, Route } from 'react-router-dom'
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

        <div className="flex-1 flex flex-col min-h-0 overflow-y-auto scrollbar-none">
          <TopBar />
          <main className="flex-1 pb-4">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/search" element={<Search />} />
              <Route path="/library" element={<LibraryMobile />} />
              <Route path="/liked-songs" element={<LikedSongs />} />
              <Route path="/playlist/:id" element={<Playlist />} />
              <Route path="/local-files" element={<LocalFiles />} />
              <Route path="/settings" element={<Settings />} />
            </Routes>
          </main>
        </div>
      </div>

      <PlayerBar />
      <MobileNav />
    </div>
  )
}
