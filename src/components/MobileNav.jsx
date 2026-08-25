import { NavLink, useNavigate } from 'react-router-dom'
import { Home, Search, Library, ListMusic, Settings2 } from 'lucide-react'

export default function MobileNav() {
  return <nav aria-label="Primary" className="md:hidden fixed bottom-0 inset-x-0 z-50 flex items-center justify-around border-t border-white/10 bg-[#0b0b0b]/95 backdrop-blur-xl px-2 pt-2 pb-[calc(.55rem+env(safe-area-inset-bottom))]">
    <Tab to="/" icon={<Home size={21} />} label="Home" />
    <Tab to="/search" icon={<Search size={21} />} label="Search" />
    <Tab to="/library" icon={<Library size={21} />} label="Library" />
    <Tab to="/now-playing" icon={<ListMusic size={21} />} label="Player" />
    <Tab to="/settings" icon={<Settings2 size={21} />} label="Settings" />
  </nav>
}
function Tab({ to, icon, label }) { return <NavLink to={to} end={to === '/'} className={({isActive}) => `flex flex-col items-center gap-1 py-1 px-4 text-[10px] font-bold ${isActive ? 'text-brand' : 'text-text-subdued'}`}>{icon}<span>{label}</span></NavLink> }
