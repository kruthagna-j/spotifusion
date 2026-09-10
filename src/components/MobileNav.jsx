import { NavLink } from 'react-router-dom'
import { Home, Search, Library, Sliders } from 'lucide-react'

export default function MobileNav() {
  return (
    <nav aria-label="Primary" className="sf-mobile-nav">
      <Tab to="/" icon={<Home size={20} />} label="Home" />
      <Tab to="/search" icon={<Search size={20} />} label="Search" />
      <Tab to="/library" icon={<Library size={20} />} label="Library" />
      <Tab to="/equalizer" icon={<Sliders size={20} />} label="Equalizer" />
    </nav>
  )
}

function Tab({ to, icon, label }) {
  return (
    <NavLink to={to} end={to === '/'} className={({ isActive }) => `sf-mobile-nav__tab ${isActive ? 'is-active' : ''}`}>
      {({ isActive }) => (
        <>
          <span className="sf-mobile-nav__icon" aria-hidden="true">{icon}</span>
          <span aria-current={isActive ? 'page' : undefined}>{label}</span>
        </>
      )}
    </NavLink>
  )
}
