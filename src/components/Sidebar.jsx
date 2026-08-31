import { NavLink, useNavigate } from 'react-router-dom'
import { Home, Search, Heart, ListMusic, Library, Music, Settings2, Plus, Clock3, Sparkles } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlaylists } from '@/hooks/useLibraryData'
import Logo from '@/components/Logo'

const primary = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/search', label: 'Search', icon: Search },
]

export default function Sidebar() {
  const { user } = useAuth()
  const playlists = usePlaylists(user?.uid)
  const navigate = useNavigate()

  return (
    <aside className="hidden md:flex md:flex-col w-[272px] shrink-0 h-full p-3 gap-3">
      <div className="sf-panel p-3">
        <div className="px-2 py-3 mb-2"><Logo /></div>
        <nav className="space-y-1" aria-label="Main navigation">
          {primary.map(({ to, label, icon: Icon, end }) => <NavItem key={to} to={to} label={label} icon={<Icon size={21} />} end={end} />)}
        </nav>
      </div>

      <div className="sf-panel flex-1 flex flex-col min-h-0 overflow-hidden">
        <div className="flex items-center justify-between px-4 pt-4 pb-3">
          <NavLink to="/library" className="flex items-center gap-3 text-text hover:text-white">
            <Library size={20} />
            <span className="font-bold text-sm">Your Library</span>
          </NavLink>
          <button onClick={() => navigate('/library', { state: { openCreate: true } })} className="icon-button" title="Create playlist" aria-label="Create playlist"><Plus size={18} /></button>
        </div>

        <div className="px-2 pb-2 flex gap-2 overflow-x-auto scrollbar-none">
          <NavLink to="/liked-songs" className="sf-chip"><Heart size={14} fill="currentColor" /> Liked</NavLink>
          <NavLink to="/local-files" className="sf-chip"><Music size={14} /> Local</NavLink>
        </div>

        <div className="px-2 flex-1 overflow-y-auto scrollbar-none space-y-1 pb-3">
          <NavItem to="/liked-songs" label="Liked Songs" icon={<Heart size={17} fill="currentColor" />} />
          <NavItem to="/local-files" label="Local Music" icon={<Music size={17} />} />
          <NavItem to="/library" label="All Playlists" icon={<ListMusic size={17} />} />
          <NavItem to="/recently-played" label="Recently Played" icon={<Clock3 size={17} />} />
          <NavItem to="/discover" label="Discover Music" icon={<Sparkles size={17} />} />
          {playlists.map((p) => <NavItem key={p.id} to={`/playlist/${p.id}`} label={p.name} subtitle={`${p.trackIds?.length || 0} songs`} icon={<ListMusic size={17} />} />)}
          {!user && <p className="text-xs text-text-subdued px-3 pt-4 leading-5">Sign in to create playlists and sync your library.</p>}
        </div>

        <div className="border-t border-border p-2">
          <NavItem to="/settings" label="Settings" icon={<Settings2 size={18} />} />
        </div>
      </div>
    </aside>
  )
}

function NavItem({ to, label, icon, subtitle, end = false }) {
  return <NavLink to={to} end={end} className={({ isActive }) => `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all ${isActive ? 'bg-brand/10 text-brand shadow-sm' : 'text-text-muted hover:text-text hover:bg-white/5'}`}>
    <span className="shrink-0">{icon}</span>
    <span className="min-w-0 flex-1"><span className="block text-sm font-semibold truncate">{label}</span>{subtitle && <span className="block text-[11px] text-text-subdued truncate">{subtitle}</span>}</span>
  </NavLink>
}