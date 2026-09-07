import { NavLink } from 'react-router-dom'
import { Home, Search, Heart, ListMusic, Library, Plus, Music, Settings2 } from "lucide-react"
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import Logo from '@/components/Logo'

const nav = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/search', label: 'Search', icon: Search },
]

export default function Sidebar() {
  const { user } = useAuth()
  const playlists = usePlaylists(user?.uid)

  return (
    <aside className="hidden md:flex md:flex-col w-[300px] shrink-0 h-full p-2 gap-2">
      <div className="bg-surface rounded-[12px] p-2">
        <div className="px-3 py-3 mb-1"><Logo /></div>
        <nav className="space-y-1" aria-label="Main navigation">
          {nav.map(({ to, label, icon: Icon, end }) => (
            <NavItem key={to} to={to} label={label} icon={<Icon size={21} />} end={end} />
          ))}
        </nav>
      </div>

      <div className="bg-surface rounded-[12px] flex-1 flex flex-col min-h-0">
        <div className="flex items-center justify-between px-4 pt-4 pb-2">
          <NavLink to="/library" className="flex items-center gap-3 text-text-muted hover:text-text">
            <Library size={20} />
            <span className="font-semibold text-sm">Your Library</span>
          </NavLink>
          <button className="p-2 rounded-full hover:bg-surface-hover" title="Create playlist" aria-label="Create playlist">
            <Plus size={18} />
          </button>
        </div>

        <div className="px-2 flex-1 overflow-y-auto scrollbar-none space-y-1 pb-3">
          <NavItem to="/liked-songs" label="Liked Songs" icon={<Heart size={17} fill="currentColor" />} />
          <NavItem to="/local-files" label="Local Music" icon={<Music size={17} />} />
          {playlists.map((p) => (
            <NavItem key={p.id} to={`/playlist/${p.id}`} label={p.name} subtitle={`${p.trackIds?.length || 0} songs`} icon={<ListMusic size={17} />} />
          ))}
          {!user && <p className="text-xs text-text-subdued px-3 pt-3">Sign in to create playlists and sync your library.</p>}
        </div>

        <div className="border-t border-border p-2">
          <NavItem to="/settings" label="Settings" icon={<Settings2 size={18} />} />
        </div>
      </div>
    </aside>
  )
}

function NavItem({ to, label, icon, subtitle, end = false }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex items-center gap-3 px-3 py-2.5 rounded-[9px] transition-colors ${isActive ? 'bg-surface-highlight text-text' : 'text-text-muted hover:text-text hover:bg-surface-hover'}`
      }
    >
      <span className="shrink-0">{icon}</span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-medium truncate">{label}</span>
        {subtitle && <span className="block text-[11px] text-text-subdued truncate">{subtitle}</span>}
      </span>
    </NavLink>
  )
}
