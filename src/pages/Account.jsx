import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, User, Settings, LogOut, Globe2, Mic2, ShieldCheck, Music2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'

function isMobileClient() {
  if (typeof window === 'undefined') return false
  return window.matchMedia?.('(max-width: 767px)').matches || /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)
}

export default function Account() {
  const { user, profile, signOut } = useAuth()
  const navigate = useNavigate()
  const mobile = isMobileClient()
  if (!user) return <div className="p-6 text-center"><h1 className="text-2xl font-black">You're not signed in.</h1><Link className="sf-primary-button mt-5" to="/login">Sign in</Link></div>
  return <div className="sf-account-page">
    <button onClick={() => navigate(-1)} className="sf-back-button"><ArrowLeft size={17}/> Back</button>
    <section className="sf-account-hero">
      {user.photoURL ? <img src={user.photoURL} alt="" className="sf-account-photo"/> : <div className="sf-account-photo sf-account-placeholder"><User size={36}/></div>}
      <div className="min-w-0"><p className="text-xs text-brand uppercase tracking-[.2em] font-black">Account</p><h1 className="text-3xl md:text-4xl font-black mt-1 truncate">{user.displayName || 'Spotifusion user'}</h1><p className="text-sm text-text-muted mt-1 truncate">{user.email}</p></div>
    </section>
    <div className="sf-account-links">
      {mobile && <Link to="/onboarding"><Mic2/><span><strong>Music preferences</strong><small>{profile?.favoriteArtists?.length || 0} favorite artists · {profile?.languages?.length || 0} languages</small></span></Link>}
      <Link to="/spotify-connect"><Music2/><span><strong>Spotify Connect</strong><small>Connect Spotify and choose a playback device</small></span></Link>
      <Link to="/settings"><Settings/><span><strong>Settings & privacy</strong><small>Playback, lyrics, storage and privacy</small></span></Link>
      {mobile && <div className="sf-account-info"><Globe2/><span><strong>Languages</strong><small>{profile?.languages?.length ? profile.languages.join(', ') : 'Not selected'}</small></span></div>}
      <div className="sf-account-info"><ShieldCheck/><span><strong>Secure account</strong><small>Authentication is handled by Firebase</small></span></div>
    </div>
    <button onClick={async () => { await signOut(); navigate('/login', { replace: true }) }} className="sf-danger-button w-full mt-4"><LogOut size={17}/> Log out</button>
  </div>
}
