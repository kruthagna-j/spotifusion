import { Link } from 'react-router-dom'
import { ArrowLeft, Music2 } from 'lucide-react'
import SpotifyConnectSection from '@/components/SpotifyConnectSection'

export default function SpotifyConnect() {
  return <div className="p-4 md:p-7 max-w-3xl mx-auto pb-40">
    <Link to="/account" className="sf-back-button"><ArrowLeft size={17}/> Back</Link>
    <div className="mt-6 mb-7">
      <div className="flex items-center gap-3"><span className="sf-settings-heading-icon"><Music2 size={19}/></span><div><p className="text-xs text-brand uppercase tracking-[.2em] font-black">Spotify</p><h1 className="text-3xl md:text-4xl font-black">Spotify Connect</h1></div></div>
      <p className="text-sm text-text-muted mt-3">Connect your Spotify account and choose the Spotify device that should receive playback.</p>
    </div>
    <section className="sf-settings-section"><div className="sf-settings-content"><SpotifyConnectSection /></div></section>
  </div>
}
