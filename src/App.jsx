import { Routes, Route } from 'react-router-dom'
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts'
import Sidebar from '@/components/Sidebar'
import TopBar from '@/components/TopBar'
import MobileNav from '@/components/MobileNav'
import PlayerBar from '@/components/PlayerBar'
import Home from '@/pages/Home'
import Search from '@/pages/Search'
import LikedSongs from '@/pages/LikedSongs'
import Playlist from '@/pages/Playlist'
import LibraryMobile from '@/pages/LibraryMobile'

export default function App() {
  useKeyboardShortcuts()
  return (
    <div className="h-screen w-screen flex flex-col bg-bg text-text overflow-hidden">
      <div className="flex flex-1 min-h-0">
        <Sidebar />
        <main className="flex-1 min-w-0 overflow-y-auto bg-gradient-to-b from-surface-highlight/60 to-bg rounded-lg md:m-2 md:ml-0">
          <TopBar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/search" element={<Search />} />
            <Route path="/liked-songs" element={<LikedSongs />} />
            <Route path="/playlist/:id" element={<Playlist />} />
            <Route path="/library" element={<LibraryMobile />} />
          </Routes>
          <div className="h-4 md:h-0" />
        </main>
      </div>
      <PlayerBar />
      <MobileNav />
    </div>
  )
}
