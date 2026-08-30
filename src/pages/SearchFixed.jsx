import { useEffect, useMemo, useRef, useState } from 'react'
import { Search as SearchIcon, WifiOff, Clock, X, SearchX, RotateCw, Trash2, ArrowRight, Disc3, UserRound, ListMusic, Radio, Music2, LoaderCircle } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { searchMusicPage } from '@/lib/search'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { getArtwork } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v2'
const CACHE_PREFIX = 'spotifusion:search-results:v5:'
const CACHE_TTL = 60 * 60 * 1000
const MAX_HISTORY = 100
const CATEGORIES = [
  { id: 'all', label: 'All', icon: SearchIcon },
  { id: 'songs', label: 'Songs', icon: Music2 },
  { id: 'albums', label: 'Albums', icon: Disc3 },
  { id: 'artists', label: 'Artists', icon: UserRound },
  { id: 'playlists', label: 'Playlists', icon: ListMusic },
  { id: 'jukebox', label: 'Jukebox', icon: Radio },
]
const TRENDING = ['Saiyaara', 'Arijit Singh', 'Anirudh Ravichander', 'A.R. Rahman', 'The Weeknd', 'Taylor Swift']

const clean = (v) => String(v || '').trim().replace(/\s+/g, ' ')
const key = (q, c) => `${CACHE_PREFIX}${c}:${clean(q).toLowerCase()}`
function readCache(q, c) {
  if (!q) return null
  try { const x = JSON.parse(sessionStorage.getItem(key(q, c)) || 'null'); return x && Date.now() - x.time < CACHE_TTL ? x : null } catch { return null }
}
function writeCache(q, c, data) { try { sessionStorage.setItem(key(q, c), JSON.stringify({ ...data, time: Date.now() })) } catch {} }
function readHistory() { try { const x = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]'); return Array.isArray(x) ? x : [] } catch { return [] } }
function addHistory(q) { const next = [q, ...readHistory().filter(x => x.toLowerCase() !== q.toLowerCase())].slice(0, MAX_HISTORY); try { localStorage.setItem(HISTORY_KEY, JSON.stringify(next)) } catch {}; return next }

function EntityCard({ item }) {
  const navigate = useNavigate()
  const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const label = type === 'artist' ? 'Artist' : type === 'album' ? 'Album' : type === 'jukebox' ? 'Jukebox' : 'Playlist'
  const id = item.browseId || item.id
  const image = getArtwork(item, 'large')
  const path = type === 'artist' ? `/artist/${encodeURIComponent(id)}` : type === 'album' ? `/album/${encodeURIComponent(id)}` : `/youtube-playlist/${encodeURIComponent(id)}`
  return <button type="button" onClick={() => navigate(path, { state: { name: item.title, artwork: image, browseId: id } })} className="sf-entity-card group text-left min-w-0">
    <div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">{image ? <img src={image} alt="" loading="lazy" className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full' : ''}`} onError={e => { e.currentTarget.style.display = 'none' }} /> : <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued" /></div>}</div>
    <p className="font-bold text-sm truncate">{item.title || 'Untitled'}</p>
    <p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || label}</p>
    <span className="inline-flex items-center gap-1 mt-2 text-[10px] uppercase tracking-wider font-bold text-text-subdued"><Icon size={11}/>{label}</span>
  </button>
}

export default function SearchFixed() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const online = useOnlineStatus()
  const params = useMemo(() => new URLSearchParams(location.search), [location.search])
  const urlQuery = clean(params.get('q'))
  const urlCategory = CATEGORIES.some(x => x.id === params.get('category')) ? params.get('category') : 'all'
  const locationQuery = urlQuery || clean(location.state?.query)
  const initial = readCache(locationQuery, urlCategory)
  const [query, setQuery] = useState(locationQuery)
  const [submitted, setSubmitted] = useState(locationQuery)
  const [category, setCategory] = useState(urlCategory)
  const [results, setResults] = useState(initial?.results || [])
  const [page, setPage] = useState(initial?.batch || 0)
  const [hasMore, setHasMore] = useState(Boolean(initial?.hasMore))
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [history, setHistory] = useState(readHistory)
  const abortRef = useRef(null)
  const requestId = useRef(0)
  const loadingMoreRef = useRef(false)
  const sentinelRef = useRef(null)
  const latest = useRef({ results, page, hasMore })
  useEffect(() => { latest.current = { results, page, hasMore } }, [results, page, hasMore])

  const groups = useMemo(() => {
    const seen = new Set()
    const unique = results.filter(x => { const k = `${x?.resultType || 'song'}:${x?.id || ''}`; if (!x?.id || seen.has(k)) return false; seen.add(k); return true })
    return {
      all: unique,
      songs: unique.filter(x => x.resultType === 'song' || !x.resultType),
      albums: unique.filter(x => x.resultType === 'album'),
      artists: unique.filter(x => x.resultType === 'artist'),
      playlists: unique.filter(x => x.resultType === 'playlist'),
      jukebox: unique.filter(x => x.resultType === 'jukebox' || x.resultType === 'mix'),
    }
  }, [results])

  async function load(qValue, categoryValue, batch = 1, replace = false) {
    const q = clean(qValue)
    if (!user || q.length < 2 || !navigator.onLine) return
    if (batch === 1) {
      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller
      const id = ++requestId.current
      setLoading(true); setError('')
      try {
        const data = await searchMusicPage(q, { category: categoryValue, batch: 1, signal: controller.signal })
        if (controller.signal.aborted || id !== requestId.current) return
        const next = Array.isArray(data.results) ? data.results : []
        setResults(next); setPage(1); setHasMore(Boolean(data.hasMore && next.length))
        writeCache(q, categoryValue, { results: next, batch: 1, hasMore: Boolean(data.hasMore && next.length) })
      } catch (e) { if (e?.name !== 'AbortError') { setResults([]); setPage(0); setHasMore(false); setError(e?.message || 'Unable to search right now.') } }
      finally { if (abortRef.current === controller) setLoading(false) }
      return
    }
    if (loadingMoreRef.current || !latest.current.hasMore) return
    loadingMoreRef.current = true; setLoadingMore(true)
    try {
      const data = await searchMusicPage(q, { category: categoryValue, batch, signal: undefined })
      const incoming = Array.isArray(data.results) ? data.results : []
      const seen = new Set(latest.current.results.map(x => `${x?.resultType || 'song'}:${x?.id || ''}`))
      const fresh = incoming.filter(x => { const k = `${x?.resultType || 'song'}:${x?.id || ''}`; if (!x?.id || seen.has(k)) return false; seen.add(k); return true })
      if (!fresh.length) { setHasMore(false); return }
      const merged = [...latest.current.results, ...fresh]
      setResults(merged); setPage(batch); setHasMore(Boolean(data.hasMore)); writeCache(q, categoryValue, { results: merged, batch, hasMore: Boolean(data.hasMore) })
    } catch (e) { setError(e?.message || 'Unable to load more results.') }
    finally { loadingMoreRef.current = false; setLoadingMore(false) }
  }

  // Only the URL changes the search identity. It never aborts an already valid request.
  useEffect(() => {
    const q = locationQuery
    const categoryChanged = urlCategory !== category
    const queryChanged = q !== submitted
    if (!categoryChanged && !queryChanged) return
    const cached = readCache(q, urlCategory)
    abortRef.current?.abort()
    setQuery(q); setSubmitted(q); setCategory(urlCategory); setResults(cached?.results || []); setPage(cached?.batch || 0); setHasMore(Boolean(cached?.hasMore)); setError('')
    if (q && !cached?.results?.length) load(q, urlCategory, 1, true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [locationQuery, urlCategory])

  // Initial/deep-link search. This is intentionally separate from URL syncing.
  useEffect(() => {
    if (user && submitted.length >= 2 && page === 0) load(submitted, category, 1, true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, submitted, category, page])

  useEffect(() => () => abortRef.current?.abort(), [])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node || !submitted || !hasMore) return
    const observer = new IntersectionObserver(entries => { if (entries[0]?.isIntersecting) load(submitted, category, latest.current.page + 1) }, { root: document.querySelector('main'), rootMargin: '1000px 0px' })
    observer.observe(node)
    return () => observer.disconnect()
  }, [submitted, category, hasMore])

  function submit(value = query, nextCategory = category) {
    const q = clean(value)
    if (q.length < 2) { setError('Enter at least 2 characters to search.'); return }
    setHistory(addHistory(q)); setQuery(q); setSubmitted(q); setResults([]); setPage(0); setHasMore(false); setError('')
    navigate(`/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(nextCategory)}`)
  }
  function chooseCategory(next) {
    if (next === category) return
    const q = submitted
    setCategory(next); setResults([]); setPage(0); setHasMore(false); setError('')
    navigate(q ? `/search?q=${encodeURIComponent(q)}&category=${next}` : `/search?category=${next}`, { replace: true })
    const cached = readCache(q, next)
    if (cached?.results?.length) { setResults(cached.results); setPage(cached.batch || 1); setHasMore(Boolean(cached.hasMore)) }
    else if (q) load(q, next, 1, true)
  }
  function clear() { abortRef.current?.abort(); ++requestId.current; setQuery(''); setSubmitted(''); setResults([]); setPage(0); setHasMore(false); setError(''); navigate('/search', { replace: true }) }

  if (!user) return <div className="p-6 text-center pt-20"><SearchIcon size={42} className="mx-auto mb-4 text-text-subdued"/><h1 className="text-xl font-bold">Sign in to search</h1><p className="text-sm text-text-muted mt-2 mb-6">Sign in to search and stream music.</p><button onClick={() => navigate('/login')} className="sf-primary-button">Sign in</button></div>

  const visible = groups[category] || []
  const label = CATEGORIES.find(x => x.id === category)?.label || 'All'
  const suggestions = query ? [...history, ...TRENDING].filter((x, i, a) => x.toLowerCase().includes(query.toLowerCase()) && a.findIndex(y => y.toLowerCase() === x.toLowerCase()) === i).slice(0, 6) : []

  return <div className="p-4 md:p-6 max-w-[1500px] mx-auto pb-36 min-w-0">
    <form onSubmit={e => { e.preventDefault(); submit() }} className="relative max-w-3xl mb-4">
      <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black"/>
      <input autoFocus value={query} onChange={e => { setQuery(e.target.value); setError('') }} placeholder="What do you want to play?" className="w-full bg-white text-black rounded-full pl-11 pr-28 py-3.5 outline-none"/>
      {query && <button type="button" onClick={clear} className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 p-2"><X size={16}/></button>}
      <button type="submit" disabled={query.trim().length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button>
    </form>
    <div className="search-category-tabs" role="tablist">{CATEGORIES.map(({id,label,icon:Icon}) => <button key={id} type="button" onClick={() => chooseCategory(id)} className={`search-category-tab ${category === id ? 'is-active' : ''}`}><Icon size={16}/>{label}</button>)}</div>
    {!online && <div className="flex gap-2 items-center text-sm text-yellow-500 bg-yellow-500/10 rounded-xl p-3 mb-4"><WifiOff size={16}/> You're offline.</div>}
    {suggestions.length > 0 && !loading && <div className="flex flex-wrap gap-2 mb-5">{suggestions.map(x => <button key={x} type="button" onClick={() => submit(x)} className="sf-history-chip"><SearchIcon size={12}/>{x}</button>)}</div>}
    {history.length > 0 && !query && <div className="mb-6"><div className="flex justify-between mb-2"><span className="text-sm font-semibold">Recent searches</span><button onClick={() => { localStorage.removeItem(HISTORY_KEY); setHistory([]) }} className="sf-ghost-button"><Trash2 size={14}/> Clear</button></div><div className="flex flex-wrap gap-2">{history.slice(0,10).map(x => <button key={x} onClick={() => submit(x)} className="sf-history-chip"><Clock size={12}/>{x}</button>)}</div></div>}
    {loading && <SkeletonRowList count={8}/>} 
    {error && !loading && <div className="flex justify-between items-center gap-3 bg-red-500/10 text-red-300 rounded-xl p-3 mb-4 max-w-3xl"><span className="text-sm">{error}</span><button onClick={() => load(submitted, category, 1, true)} className="sf-ghost-button"><RotateCw size={14}/> Retry</button></div>}
    {!loading && !error && submitted && page > 0 && !visible.length && <div className="text-center py-16 text-text-muted"><SearchX size={40} className="mx-auto mb-3"/><p>No {label.toLowerCase()} results for “{submitted}”.</p></div>}
    {visible.length > 0 && <section><div className="flex items-end justify-between mb-4"><div><p className="text-xs text-brand uppercase tracking-widest font-bold">Search results</p><h2 className="text-2xl font-black">{label}</h2></div><span className="text-xs text-text-subdued">{visible.length} loaded</span></div>
      {category === 'songs' && <div className="space-y-1">{groups.songs.map((t,i) => <TrackRow key={t.id} track={t} index={i} contextTracks={groups.songs}/>)}</div>}
      {category === 'all' && <>{groups.artists.length > 0 && <><h3 className="text-lg font-bold mb-3">Artists</h3><div className="search-entity-grid mb-8">{groups.artists.map(x => <EntityCard key={x.id} item={x}/>)}</div></>}{groups.albums.length > 0 && <><h3 className="text-lg font-bold mb-3">Albums</h3><div className="search-entity-grid mb-8">{groups.albums.map(x => <EntityCard key={x.id} item={x}/>)}</div></>}{groups.playlists.length > 0 && <><h3 className="text-lg font-bold mb-3">Playlists</h3><div className="search-entity-grid mb-8">{groups.playlists.map(x => <EntityCard key={x.id} item={x}/>)}</div></>}{groups.songs.length > 0 && <><h3 className="text-lg font-bold mb-3">Songs</h3><div className="space-y-1">{groups.songs.map((t,i) => <TrackRow key={t.id} track={t} index={i} contextTracks={groups.songs}/>)}</div></>}</>}
      {['albums','artists','playlists','jukebox'].includes(category) && <div className="search-entity-grid">{visible.map(x => <EntityCard key={x.id} item={x}/>)}</div>}
    </section>}
    {submitted && page > 0 && <div ref={sentinelRef} className="search-load-more h-16 flex items-center justify-center" aria-live="polite">{loadingMore && <LoaderCircle size={18} className="animate-spin"/>}</div>}
  </div>
}
