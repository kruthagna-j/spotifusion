import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Heart, Library, Music2, Play, Search, Settings2, Sparkles, Clock3, TrendingUp, Disc3 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus, usePlaylistsStatus, useLikedSongsStatus } from '@/hooks/useLibraryData'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'
import { getDiscover } from '@/lib/musicApi'
import { getArtwork, artworkSrcSet } from '@/lib/artwork'

function TrackCard({ track, tracks, player }) {
  return <button onClick={() => player.playTrack(track, tracks)} className="group text-left min-w-0 w-full">
    <div className="relative aspect-square rounded-xl md:rounded-2xl overflow-hidden bg-surface-highlight mb-3">
      {getArtwork(track, 'medium') ? <img src={getArtwork(track, 'medium')} srcSet={artworkSrcSet(track) || undefined} sizes="(max-width: 640px) 42vw, (max-width: 1024px) 25vw, 180px" alt="" loading="lazy" decoding="async" className="w-full h-full object-cover" /> : <div className="w-full h-full grid place-items-center"><Music2 size={32}/></div>}
      <span className="absolute right-2 bottom-2 w-10 h-10 rounded-full bg-brand text-black grid place-items-center opacity-0 group-hover:opacity-100 group-focus-visible:opacity-100 transition-all shadow-lg"><Play size={17} fill="currentColor" /></span>
    </div>
    <p className="font-bold text-sm truncate">{track.title}</p>
    <p className="text-xs text-text-muted truncate mt-1">{track.artist || 'Unknown artist'}</p>
  </button>
}

function Quick({ to, icon: Icon, title, text }) {
  return <Link to={to} className="sf-panel p-4 md:p-5 group hover:bg-white/[.07] transition min-w-0"><div className="w-10 h-10 md:w-11 md:h-11 rounded-xl bg-brand/15 text-brand grid place-items-center mb-3 md:mb-4"><Icon size={20}/></div><h3 className="font-black truncate">{title}</h3><p className="text-xs text-text-muted mt-1 line-clamp-2">{text}</p><ArrowRight size={15} className="mt-4 text-text-subdued group-hover:text-white"/></Link>
}

function MusicSection({ section, player }) {
  const tracks = section?.tracks || []
  if (!tracks.length) return null
  return <section className="min-w-0">
    <div className="flex items-end justify-between mb-4"><div><p className="text-xs text-brand uppercase tracking-widest font-black">{section.label || 'Music'}</p><h2 className="text-xl md:text-2xl font-black mt-1">{section.title}</h2>{section.subtitle && <p className="text-xs text-text-subdued mt-1">{section.subtitle}</p>}</div><Link to="/discover" className="text-sm font-bold text-text-muted hover:text-white">See all</Link></div>
    <div className="music-card-row">{tracks.map((track) => <TrackCard key={`${section.id}-${track.id}`} track={track} tracks={tracks} player={player}/>)}</div>
  </section>
}

export default function Home() {
  const { user, signIn } = useAuth()
  const player = usePlayer()
  const { data: recent = [], loading: recentLoading } = useRecentlyPlayedStatus(user?.uid, 12)
  const { data: playlists = [] } = usePlaylistsStatus(user?.uid)
  const { data: liked = [] } = useLikedSongsStatus(user?.uid)
  const [localSongs] = useLocalSongs()
  const [discover, setDiscover] = useState({ sections: [] })
  const [discoverLoading, setDiscoverLoading] = useState(false)

  useEffect(() => {
    if (!user || !navigator.onLine) return
    let cancelled = false
    setDiscoverLoading(true)
    getDiscover().then((value) => { if (!cancelled) setDiscover(value || { sections: [] }) }).catch(() => {}).finally(() => { if (!cancelled) setDiscoverLoading(false) })
    return () => { cancelled = true }
  }, [user])

  const mixes = useMemo(() => recent.length ? recent : liked.length ? liked : localSongs, [recent, liked, localSongs])
  const personalized = useMemo(() => {
    const seen = new Set()
    return [...liked, ...recent].filter((track) => track?.id && !seen.has(track.id) && seen.add(track.id)).slice(0, 12)
  }, [liked, recent])

  return <div className="p-4 md:p-7 max-w-[1500px] mx-auto space-y-8 md:space-y-10 overflow-x-hidden">
    <section className="sf-hero p-6 md:p-12 overflow-hidden relative">
      <div className="absolute -right-20 -top-24 w-80 h-80 rounded-full bg-brand/20 blur-3xl" />
      <div className="relative max-w-3xl"><p className="text-brand text-xs font-black uppercase tracking-[.25em]">Music without limits</p><h1 className="text-3xl md:text-6xl font-black tracking-tight mt-3">Everything you love.<br/><span className="text-brand">One place.</span></h1><p className="text-text-muted mt-4 md:mt-5 max-w-2xl leading-7 text-sm md:text-base">Online music, your local collection, playlists, favorites, lyrics and a full-featured player in one responsive experience.</p><div className="flex flex-wrap gap-2.5 mt-6 md:mt-7"><Link to="/search" className="sf-primary-button"><Search size={17}/> Search music</Link>{user ? <Link to="/library" className="sf-secondary-button"><Library size={17}/> Your Library</Link> : <button onClick={signIn} className="sf-secondary-button">Sign in with Google</button>}</div></div>
    </section>

    <section><div className="mb-4"><p className="text-xs text-brand font-black uppercase tracking-widest">Quick access</p><h2 className="text-xl md:text-2xl font-black mt-1">Your Spotifusion</h2></div><div className="grid grid-cols-2 md:grid-cols-4 gap-3"><Quick to="/search" icon={Search} title="Search" text="Songs, artists and albums"/><Quick to="/liked-songs" icon={Heart} title="Liked Songs" text={`${liked.length} saved tracks`}/><Quick to="/local-files" icon={Music2} title="Local Music" text={`${localSongs.length} tracks on device`}/><Quick to="/settings" icon={Settings2} title="Settings" text="Playback, audio and privacy"/></div></section>

    <section><div className="flex items-end justify-between mb-4"><div><p className="text-xs text-text-subdued uppercase tracking-widest font-bold">Keep listening</p><h2 className="text-xl md:text-2xl font-black mt-1">{recent.length ? 'Recently played' : 'Start listening'}</h2></div><Link to="/recently-played" className="text-sm font-bold text-text-muted hover:text-white">See all</Link></div>{recentLoading ? <div className="music-card-row">{Array.from({ length: 6 }, (_, i) => <div key={i} className="skeleton aspect-square rounded-2xl min-w-[150px]"/>)}</div> : mixes.length ? <div className="music-card-row">{mixes.slice(0, 12).map((track) => <TrackCard key={track.id} track={track} tracks={mixes} player={player}/>)}</div> : <div className="sf-panel p-10 text-center text-text-muted">Search for your first song and it will appear here.</div>}</section>

    {personalized.length > 0 && <MusicSection section={{ id: 'because-you-listened', label: 'For you', title: 'Because you listened to these', subtitle: 'A quick mix from your recent activity', tracks: personalized }} player={player}/>}
    {discoverLoading && <section><div className="music-card-row">{Array.from({ length: 5 }, (_, i) => <div key={i} className="skeleton aspect-square rounded-2xl min-w-[150px]"/>)}</div></section>}
    {discover.sections?.map((section) => <MusicSection key={section.id} section={section} player={player}/>) }

    <section><div className="flex items-center justify-between mb-4"><div><p className="text-xs text-text-subdued uppercase tracking-widest font-bold">Explore</p><h2 className="text-xl md:text-2xl font-black mt-1">More to discover</h2></div><Link to="/discover" className="text-sm font-bold text-text-muted hover:text-white">Discover</Link></div><div className="grid grid-cols-1 md:grid-cols-3 gap-3"><Quick to="/discover" icon={Sparkles} title="Discover Music" text="Explore trending and popular music"/><Quick to="/library" icon={Library} title={`${playlists.length} Playlists`} text="Build and organize your collection"/><Quick to="/recently-played" icon={Clock3} title="Listening history" text="Return to tracks you played recently"/></div></section>
  </div>
}
