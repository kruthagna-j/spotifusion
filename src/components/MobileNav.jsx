import { NavLink, useNavigate } from 'react-router-dom'
import { Home, Search, Library, Plus } from 'lucide-react'

export default function MobileNav() {
  const navigate = useNavigate()

  // "Create" jumps to Library with a flag that auto-opens the existing
  // new-playlist form there, rather than duplicating that logic here.
  function handleCreate() {
    navigate('/library', { state: { openCreate: true } })
  }

  return (
    <nav
      aria-label="Primary"
      className="md:hidden flex items-center justify-around border-t border-border bg-surface px-2 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom))]"
    >
      <Tab to="/" icon={<Home size={22} />} label="Home" />
      <Tab to="/search" icon={<Search size={22} />} label="Search" />
      <Tab to="/library" icon={<Library size={22} />} label="Library" />
      <button
        onClick={handleCreate}
        aria-label="Create a new playlist"
        className="flex flex-col items-center gap-1 py-1 px-3 text-[11px] font-medium text-text-subdued hover:text-text transition-colors"
      >
        <Plus size={22} aria-hidden="true" />
        <span>Create</span>
      </button>
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
