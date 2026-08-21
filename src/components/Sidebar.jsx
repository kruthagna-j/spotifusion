import { NavLink } from 'react-router-dom'
import { Home, Search, Library, Plus, Heart, ListMusic, HardDrive } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'

const navItem =
  'flex items-center gap-4 px-3 py-2 rounded-md text-sm font-semibold transition-colors'

export default function Sidebar() {
  const { user } = useAuth()
  const playlists = usePlaylists(user?.uid)

  return (
    <aside className="hidden md:flex md:flex-col w-[280px] shrink-0 h-full gap-2 p-2">
      {/* Primary nav */}
      <nav className="bg-surface rounded-lg p-2">
        <NavItem to="/" icon={<Home size={22} />} label="Home" />
        <NavItem to="/search" icon={<Search size={22} />} label="Search" />
        <NavItem to="/local-files" icon={<HardDrive size={22} />} label="Local Files" />
      </nav>

      {/* Library */}
      <div className="bg-surface rounded-lg flex-1 flex flex-col min-h-0">
        <div className="flex items-center justify-between px-4 pt-4 pb-2">
          <div className="flex items-center gap-3 text-text-muted hover:text-text cursor-pointer transition-colors">
            <Library size={22} />
            <span className="font-semibold text-sm">Your Library</span>
          </div>
          <button
            className="p-1.5 rounded-full text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
            title="Create playlist"
          >
            <Plus size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto scrollbar-none px-2 pb-4 flex flex-col gap-1">
          <LibraryRow
            to="/liked-songs"
            gradient="from-indigo-400 to-white"
            icon={<Heart size={16} fill="white" className="text-white" />}
            title="Liked Songs"
            subtitle="Playlist"
          />
          {playlists.map((p) => (
            <LibraryRow
              key={p.id}
              to={`/playlist/${p.id}`}
              icon={<ListMusic size={16} className="text-text-subdued" />}
              title={p.name}
              subtitle={`Playlist • ${p.trackIds?.length || 0} songs`}
            />
          ))}
          {!user && (
            <p className="text-xs text-text-subdued px-2 pt-2">
              Sign in to create playlists and see your library.
            </p>
          )}
        </div>
      </div>
    </aside>
  )
}

function NavItem({ to, icon, label }) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      className={({ isActive }) =>
        `${navItem} ${isActive ? 'text-text' : 'text-text-muted hover:text-text'}`
      }
    >
      {icon}
      {label}
    </NavLink>
  )
}

function LibraryRow({ to, icon, title, subtitle, gradient }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-2 py-2 rounded-md hover:bg-surface-hover transition-colors ${
          isActive ? 'bg-surface-hover' : ''
        }`
      }
    >
      <div
        className={`w-12 h-12 rounded-md shrink-0 flex items-center justify-center ${
          gradient ? `bg-gradient-to-br ${gradient}` : 'bg-surface-highlight'
        }`}
      >
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-sm text-text truncate">{title}</p>
        <p className="text-xs text-text-subdued truncate">{subtitle}</p>
      </div>
    </NavLink>
  )
}
