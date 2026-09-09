import { NavLink } from 'react-router-dom'
import { Home, Search, Library, Sliders } from 'lucide-react'

export default function MobileNav() {
  return (
    <nav
      aria-label="Primary"
      className="md:hidden flex items-center justify-around border-t border-border bg-surface px-2 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom))]"
    >
      <Tab to="/" icon={<Home size={22} />} label="Home" />
      <Tab to="/search" icon={<Search size={22} />} label="Search" />
      <Tab to="/library" icon={<Library size={22} />} label="Library" />
      <Tab to="/equalizer" icon={<Sliders size={22} />} label="Equalizer" />
    </nav>
  )
}

function Tab({ to, icon, label }) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      className={({ isActive }) =>
        `flex flex-col items-center gap-1 py-1 px-3 text-[11px] font-medium ${
          isActive ? 'text-text' : 'text-text-subdued'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <span aria-hidden="true">{icon}</span>
          <span aria-current={isActive ? 'page' : undefined}>{label}</span>
        </>
      )}
    </NavLink>
  )
}
