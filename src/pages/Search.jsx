import { useEffect, useMemo, useState, useCallback, useRef } from 'react'
import { Search as SearchIcon, WifiOff, Clock, X, SearchX, RotateCw, Trash2, ArrowRight, Disc3, UserRound, ListMusic, Radio, Play } from 'lucide-react'
import { searchMusic } from '@/lib/musicApi'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { useLocation } from 'react-router-dom'
import { getArtwork, artworkSrcSet } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v2'
const MAX_HISTORY = 100
const VISIBLE_HISTORY = 100
const TRENDING_SUGGESTIONS = ['Saiyaara', 'Arijit Singh', 'Anirudh Ravichander', 'A.R. Rahman', 'The Weeknd', 'Taylor Swift']

function normalizeQuery(value) { return value.trim().replace(/\s+/g, ' ') }
function loadHistory() {
  try {
    const stored = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    if (!Array.isArray(stored)) return []
    return stored.filter((x) => typeof x === 'string').map(normalizeQuery).filter((x) => x.length >= 2).slice(0, MAX_HISTORY)
  } catch { return [] }
}
function saveHistory(items) { try { localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, MAX_HISTORY))) } catch {} }
function addHistory(query) {
  const q = normalizeQuery(query)
  if (q.length < 2) return loadHistory()
  const next = [q, ...loadHistory().filter((x) => x.toLowerCase() !== q.toLowerCase())].slice(0, MAX_HISTORY)
  saveHistory(next)
  return next
}
function removeHistory(query) {
  const next = loadHistory().filter((x) => x.toLowerCase() !== normalizeQuery(query).toLowerCase())
  saveHistory(next)
  return next
}


function EntityCard({ item, onOpen }) {
  const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const label = type === 'artist' ? 'Artist' : type === 'album' ? 'Album' : type === 'jukebox' ? 'Jukebox / Mix' : 'Playlist'
  const image = getArtwork(item, 'medium')
  return (
    <button type="button" onClick={() => onOpen(item)} className="sf-entity-card group text-left min-w-0">
      <div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">
        {image ? <img src={image} srcSet={artworkSrcSet(item)} sizes="(max-width: 640px) 42vw, (max-width: 1024px) 22vw, 190px" alt="" loading="lazy" decoding="async" className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full p-0.5' : ''}`} /> : <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued" /></div>}
        <span className="absolute right-2 bottom-2 w-10 h-10 rounded-full bg-brand text-black grid place-items-center opacity-0 group-hover:opacity-100 group-focus-visible:opacity-100 transition-all shadow-lg"><Play size={17} fill="currentColor" /></span>
      </div>
      <p className="font-bold text-sm truncate">{item.title}</p>
      <p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || label}</p>
      <span className="inline-flex items-center gap-1 mt-2 text-[10px] uppercase tracking-wider font-bold text-text-subdued"><Icon size={11} /> {label}</span>
    </button>
  )
}

function EntitySection({ title, items, onOpen }) {
  if (!items.length) return null
  return (
    <section className="mb-8">
      <div className="flex items-end justify-between mb-4">
        <div><p className="text-xs text-brand uppercase tracking-widest font-black">Explore</p><h2 className="text-xl md:text-2xl font-black mt-1">{title}</h2></div>
        <span className="text-xs text-text-subdued">{items.length}</span>
      </div>
      <div className="search-entity-grid">{items.map((item) => <EntityCard key={`${item.resultType}-${item.id}`} item={item} onOpen={onOpen} />)}</div>
    </section>
  )
}

export default function Search() {
  const { user, signIn } = useAuth()
  const location = useLocation()
  const online = useOnlineStatus()
  const initial = normalizeQuery(location.state?.query || '')
  const [query, setQuery] = useState(initial)
  const [submittedQuery, setSubmittedQuery] = useState(initial)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState(loadHistory)
  const [showAllHistory, setShowAllHistory] = useState(false)
  const abortRef = useRef(null)
  const requestId = useRef(0)

  const visibleHistory = useMemo(() => showAllHistory ? history : history.slice(0, VISIBLE_HISTORY), [history, showAllHistory])
  const suggestions = useMemo(() => {
    const q = normalizeQuery(query).toLowerCase()
    if (!q) return TRENDING_SUGGESTIONS
    return [...history, ...TRENDING_SUGGESTIONS].filter((item, index, list) => item.toLowerCase().includes(q) && list.findIndex((x) => x.toLowerCase() === item.toLowerCase()) === index).slice(0, 6)
  }, [query, history])
  const normalizedResults = useMemo(() => {
    const seen = new Set()
    return (Array.isArray(results) ? results : []).filter((item) => item?.id && !seen.has(item.id) && seen.add(item.id))
  }, [results])
  const searchGroups = useMemo(() => {
    const songs = normalizedResults.filter((item) => item.resultType === 'song' || !item.resultType)
    const artists = normalizedResults.filter((item) => item.resultType === 'artist')
    const albums = normalizedResults.filter((item) => item.resultType === 'album')
    const playlists = normalizedResults.filter((item) => item.resultType === 'playlist')
    const jukeboxes = normalizedResults.filter((item) => item.resultType === 'jukebox' || item.resultType === 'mix')
    return { songs, artists, albums, playlists, jukeboxes }
  }, [normalizedResults])

  const runSearch = useCallback(async (rawQuery) => {
    const q = normalizeQuery(rawQuery)
    if (!user || q.length < 2) { setResults([]); setLoading(false); return }
    if (!navigator.onLine) { setError("You're offline — online search is unavailable right now."); setResults([]); return }
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    const id = ++requestId.current
    setLoading(true); setError(null)
    try {
      const tracks = await searchMusic(q, { signal: controller.signal })
      if (controller.signal.aborted || id !== requestId.current) return
      setResults(tracks)
    } catch (err) {
      if (err?.name === 'AbortError' || controller.signal.aborted || id !== requestId.current) return
      setError(err?.message || 'Unable to search right now. Please try again.')
      setResults([])
    } finally {
      if (abortRef.current === controller && id === requestId.current) setLoading(false)
    }
  }, [user])

  useEffect(() => { if (user && submittedQuery) runSearch(submittedQuery) }, [submittedQuery, runSearch, user])
  useEffect(() => () => abortRef.current?.abort(), [])

  const submitSearch = useCallback(() => {
    const q = normalizeQuery(query)
    if (q.length < 2) { setError('Enter at least 2 characters to search.'); return }
    setHistory(addHistory(q))
    setSubmittedQuery(q)
    setError(null)
  }, [query])

  const clearSearch = () => { abortRef.current?.abort(); setQuery(''); setSubmittedQuery(''); setResults([]); setError(null); setLoading(false) }
  const clearHistory = () => { localStorage.removeItem(HISTORY_KEY); setHistory([]); setShowAllHistory(false) }

  if (!user) return (
    <div className="p-4 md:p-6 flex flex-col items-center text-center pt-20">
      <SearchIcon size={40} className="text-text-subdued mb-4" />
      <h1 className="text-xl font-bold mb-2">Sign in to search</h1>
      <p className="text-text-muted text-sm max-w-sm mb-6">Searching and streaming online songs requires a free account.</p>
      <button onClick={signIn} className="sf-primary-button">Sign in with Google</button>
    </div>
  )

  return (
    <div className="p-4 md:p-6 max-w-[1500px] mx-auto">
      <form onSubmit={(e) => { e.preventDefault(); submitSearch() }} className="relative max-w-3xl mb-6">
        <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black" />
        <input autoFocus value={query} onChange={(e) => { setQuery(e.target.value); setError(null) }} placeholder="What do you want to play?" aria-label="Search for songs, artists, or albums" className="w-full bg-white text-black placeholder-gray-600 rounded-full pl-11 pr-28 py-3.5 text-sm font-medium outline-none focus:ring-2 focus:ring-brand" />
        {query && <button type="button" onClick={clearSearch} aria-label="Clear search" className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 hover:text-black p-2"><X size={16} /></button>}
        <button type="submit" disabled={normalizeQuery(query).length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button>
      </form>

      {!online && <div className="flex items-center gap-2 text-sm text-yellow-500 bg-yellow-500/10 rounded-xl px-3 py-2 mb-4 max-w-3xl"><WifiOff size={16} /> You're offline — online search is unavailable right now.</div>}

      {query.length >= 1 && suggestions.length > 0 && !loading && <div className="max-w-3xl mb-6"><p className="text-xs text-text-subdued uppercase tracking-widest font-bold mb-2">Suggestions</p><div className="flex flex-wrap gap-2">{suggestions.map((item) => <button key={item} onClick={() => { setQuery(item); setSubmittedQuery(item) }} className="sf-history-chip"><SearchIcon size={12}/><span className="truncate max-w-[220px]">{item}</span></button>)}</div></div>}

      {!query && history.length > 0 && <section className="mb-8 max-w-4xl">
        <div className="flex items-center justify-between mb-3"><div><p className="text-sm font-semibold">Recent searches</p><p className="text-xs text-text-subdued mt-1">{history.length} saved</p></div><button onClick={clearHistory} className="sf-ghost-button"><Trash2 size={14}/> Clear all</button></div>
        <div className="flex flex-wrap gap-2">{visibleHistory.map((item) => <div key={item} className="sf-history-chip"><button onClick={() => { setQuery(item); setSubmittedQuery(item) }} className="flex items-center gap-2 min-w-0"><Clock size={12}/><span className="truncate max-w-[220px]">{item}</span></button><button onClick={() => setHistory(removeHistory(item))} aria-label={`Remove ${item}`}><X size={12}/></button></div>)}</div>
        {history.length > VISIBLE_HISTORY && <button onClick={() => setShowAllHistory((v) => !v)} className="mt-3 text-xs text-brand flex items-center gap-1">{showAllHistory ? 'Show less' : `Show all ${history.length}`}<ArrowRight size={12}/></button>}
      </section>}

      {loading && <SkeletonRowList count={8} />}
      {error && !loading && <div className="flex items-center justify-between gap-3 bg-red-500/10 text-red-400 rounded-xl px-4 py-3 mb-4 max-w-3xl"><span className="text-sm">{error}</span>{submittedQuery && <button onClick={() => runSearch(submittedQuery)} className="sf-ghost-button"><RotateCw size={14}/> Retry</button>}</div>}
      {!loading && !error && submittedQuery && normalizedResults.length === 0 && <div className="text-center py-16"><SearchX size={40} className="mx-auto mb-3 text-text-subdued"/><p className="text-text-muted text-sm">No results found for “{submittedQuery}”.</p></div>}
      {normalizedResults.length > 0 && <>
        <EntitySection title="Artists" items={searchGroups.artists.slice(0, 12)} onOpen={(item) => { setQuery(item.title); setSubmittedQuery(item.title) }} />
        <EntitySection title="Albums" items={searchGroups.albums.slice(0, 12)} onOpen={(item) => { setQuery(item.title); setSubmittedQuery(item.title) }} />
        <EntitySection title="Playlists" items={searchGroups.playlists.slice(0, 12)} onOpen={(item) => { setQuery(item.title); setSubmittedQuery(item.title) }} />
        <EntitySection title="Jukebox & Mixes" items={searchGroups.jukeboxes.slice(0, 12)} onOpen={(item) => { setQuery(item.title); setSubmittedQuery(item.title) }} />
        {searchGroups.songs.length > 0 && <section><div className="flex items-end justify-between mb-3"><div><p className="text-xs text-brand uppercase tracking-widest font-bold">Search</p><h2 className="text-2xl font-black">Songs</h2></div><span className="text-xs text-text-subdued">{searchGroups.songs.length} results</span></div><div className="flex flex-col">{searchGroups.songs.map((track, i) => <TrackRow key={track.id} track={track} index={i} contextTracks={searchGroups.songs} />)}</div></section>}
      </>}
      {!query && history.length === 0 && <div className="py-14 text-center text-text-subdued text-sm">Search for a song, artist, album or playlist.</div>}
    </div>
  )
}
