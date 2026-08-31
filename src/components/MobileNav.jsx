import { NavLink, useLocation } from 'react-router-dom'
import { Home, Search, Library, SlidersHorizontal, Settings2 } from 'lucide-react'

const items = [
  { to: '/', icon: Home, label: 'Home', end: true },
  { to: '/search', icon: Search, label: 'Search' },
  { to: '/library', icon: Library, label: 'Library' },
  { to: '/equalizer', icon: SlidersHorizontal, label: 'Equalizer' },
  { to: '/settings', icon: Settings2, label: 'Settings' },
]

export default function MobileNav() {
  const location = useLocation()
  return <nav aria-label="Primary" className="mobile-bottom-nav md:hidden">
    <div className="mobile-bottom-nav-inner">
      {items.map(({ to, icon: Icon, label, end }) => {
        const active = end ? location.pathname === to : location.pathname === to || location.pathname.startsWith(`${to}/`)
        return <NavLink key={to} to={to} end={end} className={`mobile-tab ${active ? 'is-active' : ''}`}>
          <span className="mobile-tab-icon"><Icon size={19}/></span><span>{label}</span>
        </NavLink>
      })}
    </div>
  </nav>
}
