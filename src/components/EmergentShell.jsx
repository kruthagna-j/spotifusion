import { useLocation, useNavigate } from 'react-router-dom'
import { Bell, Lock, Maximize2, Music2, Smartphone } from 'lucide-react'
import PlayerBar from '@/components/PlayerBar'
import MobileNav from '@/components/MobileNav'
import { usePlayer } from '@/context/PlayerContext'

const QUICK_ROUTES = [
  ['/', 'Home'], ['search', 'Search'], ['library', 'Library'], ['player', 'Player'],
  ['equalizer', 'Equalizer'], ['settings', 'Settings'], ['playlists', 'Playlists'],
  ['lyrics', 'Lyrics'], ['local-files', 'Local Files'],
]

export default function EmergentShell({ children }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { currentTrack } = usePlayer()
  const splash = location.pathname === '/splash'
  const playerRoute = location.pathname === '/player'
  const lyricsRoute = location.pathname === '/lyrics'

  return <div className="emergent-phone-simulator">
    <header className="emergent-simulator-bar">
      <div className="emergent-brand-lockup"><div className="emergent-brand-icon"><Music2 size={17} /></div><div><strong>Spotifusion <small>Mobile 2.0</small></strong><span>Your Music, Your Way</span></div></div>
      <div className="emergent-simulator-actions"><button onClick={() => {}} title="Preview Android lock screen"><Lock size={14} /> Lock Screen</button><button onClick={() => {}} title="Preview notification shade"><Bell size={14} /> Notifications</button><button onClick={() => {}} title="Phone frame"><Smartphone size={14} /> Phone Frame</button></div>
    </header>
    <nav className="emergent-quick-nav"><b>12 Screens:</b>{QUICK_ROUTES.map(([path, label]) => <button key={path} className={location.pathname === (path === '/' ? '/' : `/${path}`) ? 'is-active' : ''} onClick={() => navigate(path === '/' ? '/' : `/${path}`)}>{label}</button>)}</nav>
    <div className="emergent-phone-frame">
      {!splash && <div className="emergent-device-status"><span>{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span><span>● ◔ ▰</span></div>}
      <main className={`emergent-device-main ${playerRoute || lyricsRoute ? 'is-immersive' : ''}`}>{children}</main>
      {!splash && !playerRoute && !lyricsRoute && <div className="emergent-persistent-bottom">{currentTrack && <PlayerBar />}<MobileNav /></div>}
    </div>
    {playerRoute && <button className="emergent-frame-close" onClick={() => navigate(-1)} aria-label="Close player"><Maximize2 size={16} /></button>}
  </div>
}
