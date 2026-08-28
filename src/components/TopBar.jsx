import { useState } from 'react'
import { useNavigate, useLocation, NavLink } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Search, Settings as SettingsIcon } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import ProfilePanel from '@/components/ProfilePanel'
import Logo from '@/components/Logo'

export default function TopBar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)

  return <header className="sticky top-0 z-20 flex items-center justify-between gap-4 px-4 md:px-6 py-3 bg-bg/90 backdrop-blur-md">
    <div className="flex items-center gap-6">
      <Logo withWordmark={false} className="md:hidden" />
      <div className="hidden md:flex items-center gap-1">
        <button onClick={() => navigate(-1)} aria-label="Go back" className="w-8 h-8 rounded-full bg-surface-elevated flex items-center justify-center"><ChevronLeft size={20}/></button>
        <button onClick={() => navigate(1)} aria-label="Go forward" className="w-8 h-8 rounded-full bg-surface-elevated flex items-center justify-center"><ChevronRight size={20}/></button>
      </div>
      {location.pathname !== '/search' && <button onClick={() => navigate('/search')} className="flex items-center gap-2 bg-surface-elevated rounded-full pl-3 pr-4 py-2 text-sm text-text-muted md:hidden"><Search size={16}/> Search</button>}
    </div>
    <div className="flex items-center gap-1.5 sm:gap-2 min-w-0">
      <NavLink to="/settings" aria-label="Settings" className="flex w-9 h-9 shrink-0 rounded-full items-center justify-center text-text-muted hover:text-text hover:bg-surface-elevated"><SettingsIcon size={19}/></NavLink>
      <div className="relative">
        {user ? <>
          <button onMouseDown={(e) => e.stopPropagation()} onClick={() => { if (window.matchMedia?.('(max-width: 767px)').matches) navigate('/account'); else setProfileOpen(v => !v) }} aria-label="Open account menu" aria-expanded={profileOpen} className="flex items-center gap-2 shrink-0 bg-surface-elevated rounded-full pl-1 pr-2 sm:pr-3 py-1 text-sm font-medium">
            {user.photoURL ? <img src={user.photoURL} alt="" className="w-7 h-7 rounded-full"/> : <div className="w-7 h-7 rounded-full bg-brand"/>}
            <span className="hidden sm:inline max-w-[120px] truncate">{user.displayName?.split(' ')[0]}</span>
          </button>
          {profileOpen && <ProfilePanel onClose={() => setProfileOpen(false)}/>} 
        </> : <button onClick={() => navigate('/login')} className="bg-white text-black font-bold text-xs sm:text-sm px-3 sm:px-6 py-2 rounded-full whitespace-nowrap"><span className="sm:hidden">Sign in</span><span className="hidden sm:inline">Sign in with Google</span></button>}
      </div>
    </div>
  </header>
}
