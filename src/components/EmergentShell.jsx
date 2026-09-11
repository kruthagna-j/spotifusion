import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Home, Search, Library, Settings, ChevronDown } from 'lucide-react'
import PlayerBar from '@/components/PlayerBar'
import { usePlayer } from '@/context/PlayerContext'

const tabs = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/search', label: 'Search', icon: Search },
  { to: '/library', label: 'Library', icon: Library },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export default function EmergentShell({ children }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { currentTrack } = usePlayer()
  const immersive = ['/player', '/lyrics'].includes(location.pathname)

  return (
    <div className={`emergent-shell ${immersive ? 'emergent-shell--immersive' : ''}`}>
      <div className="emergent-device">
        {!immersive && <div className="emergent-statusbar"><span>9:41</span><span>● ◔ ▰</span></div>}
        <main className="emergent-main">{children}</main>
        {!immersive && currentTrack && <PlayerBar />}
        {!immersive && (
          <nav className="emergent-bottom-nav" aria-label="Primary navigation">
            {tabs.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `emergent-bottom-tab ${isActive ? 'is-active' : ''}`}>
                <Icon size={19} />
                <span>{label}</span>
              </NavLink>
            ))}
          </nav>
        )}
      </div>
      {immersive && location.pathname === '/player' && (
        <button className="emergent-desktop-back" onClick={() => navigate(-1)} aria-label="Close player"><ChevronDown size={20} /></button>
      )}
    </div>
  )
}
