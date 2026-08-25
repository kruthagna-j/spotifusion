import { Play, Clock3, Music2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus } from '@/hooks/useLibraryData'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'
import { Link } from 'react-router-dom'

export default function RecentlyPlayed() {
  const { user, signIn } = useAuth()
  const { data: tracks, loading } = useRecentlyPlayedStatus(user?.uid, 50)
  const player = usePlayer()
  if (!user) return <EmptySignIn signIn={signIn} />
  return <div className="p-4 md:p-7 max-w-6xl mx-auto">
    <div className="flex items-end justify-between mb-7">
      <div>
        <p className="text-xs uppercase tracking-[.2em] text-text-subdued font-bold">History</p>
        <h1 className="text-3xl md:text-4xl font-black mt-1">Recently Played</h1>
        <p className="text-sm text-text-muted mt-2">Jump back into the music you played recently.</p>
      </div>
      {tracks.length > 0 && <button onClick={() => player.playTrack(tracks[0], tracks)} className="hidden sm:flex items-center gap-2 bg-brand text-black font-black px-5 py-3 rounded-full"><Play size={18} fill="currentColor"/> Play latest</button>}
    </div>
    {loading ? <div className="space-y-2">{Array.from({length:8}).map((_,i)=><div className="h-14 rounded-xl bg-white/5 animate-pulse" key={i}/>)}</div>
      : tracks.length ? <div className="space-y-1">{tracks.map((t,i)=><TrackRow key={t.id} track={t} index={i} contextTracks={tracks}/>)}</div>
      : <div className="sf-panel p-12 text-center"><Clock3 className="mx-auto mb-4 text-text-subdued" size={40}/><h2 className="font-bold text-lg">Nothing here yet</h2><p className="text-sm text-text-muted mt-2">Play a song and it will appear here.</p><Link to="/search" className="inline-flex mt-5 bg-white text-black px-5 py-2.5 rounded-full font-bold text-sm">Find music</Link></div>}
  </div>
}
function EmptySignIn({signIn}) { return <div className="p-8 text-center max-w-md mx-auto mt-16"><Music2 className="mx-auto mb-4 text-text-subdued" size={44}/><h1 className="text-2xl font-black">Sign in to see your history</h1><p className="text-sm text-text-muted mt-2">Your recently played music is synced to your Spotifusion account.</p><button onClick={signIn} className="mt-6 bg-brand text-black font-bold px-6 py-3 rounded-full">Sign in with Google</button></div> }
