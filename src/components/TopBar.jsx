import { useState } from 'react'
import { useNavigate, useLocation, NavLink } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Search, Settings as SettingsIcon, Bell, SlidersHorizontal, Plus } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import ProfilePanel from '@/components/ProfilePanel'
import Logo from '@/components/Logo'

const titles = { '/search': 'Search', '/library': 'Your Library', '/equalizer': 'Equalizer', '/settings': 'Settings', '/liked-songs': 'Liked Songs', '/local-files': 'Local Music' }

export default function TopBar() {
  const navigate = useNavigate(); const location = useLocation(); const { user } = useAuth(); const [profileOpen, setProfileOpen] = useState(false)
  const mobileTitle = titles[location.pathname]
  const isHome = location.pathname === '/'
  return <header className="sticky top-0 z-20 flex items-center justify-between gap-4 px-4 md:px-6 py-3 bg-bg/90 backdrop-blur-md lg-topbar">
    <div className="flex items-center gap-6 min-w-0">
      <div className="hidden md:flex items-center gap-1">
        <button onClick={() => navigate(-1)} aria-label="Go back" className="w-8 h-8 rounded-full bg-surface-elevated flex items-center justify-center"><ChevronLeft size={20}/></button>
        <button onClick={() => navigate(1)} aria-label="Go forward" className="w-8 h-8 rounded-full bg-surface-elevated flex items-center justify-center"><ChevronRight size={20}/></button>
      </div>
      <div className="md:hidden flex items-center gap-2 min-w-0">
        {isHome ? <Logo withWordmark={true} /> : <><button onClick={() => navigate(-1)} className="lg-mobile-back" aria-label="Back"><ChevronLeft size={20}/></button><strong className="lg-mobile-title truncate">{mobileTitle || 'Spotifusion'}</strong></>}
      </div>
      {location.pathname !== '/search' && <button onClick={() => navigate('/search')} className="hidden md:flex items-center gap-2 bg-surface-elevated rounded-full pl-3 pr-4 py-2 text-sm text-text-muted"><Search size={16}/> Search</button>}
    </div>
    <div className="flex items-center gap-1.5 sm:gap-2 min-w-0">
      <button className="lg-mobile-action md:hidden" aria-label={isHome ? 'Notifications' : location.pathname === '/search' ? 'Filters' : location.pathname === '/library' ? 'Create playlist' : 'Options'}>
        {isHome ? <Bell size={19}/> : location.pathname === '/search' ? <SlidersHorizontal size={18}/> : location.pathname === '/library' ? <Plus size={20}/> : <SettingsIcon size={19}/>} 
      </button>
      <NavLink to="/settings" aria-label="Settings" className="hidden md:flex w-9 h-9 shrink-0 rounded-full items-center justify-center text-text-muted hover:text-text hover:bg-surface-elevated"><SettingsIcon size={19}/></NavLink>
      <div className="relative hidden md:block">
        {user ? <>
          <button onMouseDown={(e) => e.stopPropagation()} onClick={() => setProfileOpen(v => !v)} aria-label="Open account menu" aria-expanded={profileOpen} className="flex items-center gap-2 shrink-0 bg-surface-elevated rounded-full pl-1 pr-2 sm:pr-3 py-1 text-sm font-medium">
            {user.photoURL ? <img src={user.photoURL} alt="" className="w-7 h-7 rounded-full"/> : <div className="w-7 h-7 rounded-full bg-brand"/>}
            <span className="hidden sm:inline max-w-[120px] truncate">{user.displayName?.split(' ')[0]}</span>
          </button>
          {profileOpen && <ProfilePanel onClose={() => setProfileOpen(false)}/>} 
        </> : <button onClick={() => navigate('/login')} className="bg-white text-black font-bold text-xs sm:text-sm px-3 sm:px-6 py-2 rounded-full whitespace-nowrap">Sign in</button>}
      </div>
    </div>
  </header>
}
