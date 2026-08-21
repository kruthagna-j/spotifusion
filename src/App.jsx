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
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts'

export default function App() {
  useKeyboardShortcuts()

  return (
    <div className="h-screen flex flex-col bg-bg text-text overflow-hidden">
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
            </Routes>
          </main>
        </div>
      </div>

      <PlayerBar />
      <MobileNav />
    </div>
  )
}
