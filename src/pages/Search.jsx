import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Search as SearchIcon, WifiOff, Clock, X, SearchX, RotateCw, Trash2, ArrowRight, Disc3, UserRound, ListMusic, Radio, Music2, LoaderCircle } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { searchMusicPage } from '@/lib/search'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { getArtwork } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v2'
const STATE_KEY = 'spotifusion:search-state:v4'
const MAX_HISTORY = 100
const VISIBLE_HISTORY = 10
const RESULT_CACHE_PREFIX = 'spotifusion:search-results:v4:'
const RESULT_CACHE_TTL = 60 * 60 * 1000

const CATEGORIES = [
  { id: 'all', label: 'All', icon: SearchIcon },
  { id: 'songs', label: 'Songs', icon: Music2 },
  { id: 'albums', label: 'Albums', icon: Disc3 },
  { id: 'artists', label: 'Artists', icon: UserRound },
  { id: 'playlists', label: 'Playlists', icon: ListMusic },
  { id: 'jukebox', label: 'Jukebox', icon: Radio },
]

const TRENDING_SUGGESTIONS = ['Saiyaara', 'Arijit Singh', 'Anirudh Ravichander', 'A.R. Rahman', 'The Weeknd', 'Taylor Swift']

function normalizeQuery(value) { return String(value || '').trim().replace(/\s+/g, ' ') }
function cacheKey(query, category) { return `${RESULT_CACHE_PREFIX}${category}:${normalizeQuery(query).toLowerCase()}` }

function loadCache(query, category) {
  if (!query) return null
  try {
    const parsed = JSON.parse(sessionStorage.getItem(cacheKey(query, category)) || 'null')
    if (!parsed || Date.now() - parsed.time > RESULT_CACHE_TTL || !Array.isArray(parsed.results)) return null
    return parsed
  } catch { return null }
}
function saveCache(query, category, data) {
  if (!query || !Array.isArray(data?.results)) return
  try { sessionStorage.setItem(cacheKey(query, category), JSON.stringify({ time: Date.now(), results: data.results, batch: data.batch || 1, hasMore: !!data.hasMore })) } catch {}
}
function saveViewState(value) { try { sessionStorage.setItem(STATE_KEY, JSON.stringify(value)) } catch {} }
function loadHistory() {
  try {
    const value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    return Array.isArray(value) ? value.filter((x) => typeof x === 'string').map(normalizeQuery).filter((x) => x.length >= 2).slice(0, MAX_HISTORY) : []
  } catch { return [] }
}
function saveHistory(items) { try { localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, MAX_HISTORY))) } catch {} }
function addHistory(query) {
  const q = normalizeQuery(query)
  if (q.length < 2) return loadHistory()
  const next = [q, ...loadHistory().filter((x) => x.toLowerCase() !== q.toLowerCase())].slice(0, MAX_HISTORY)
  saveHistory(next); return next
}
function removeHistory(query) {
  const next = loadHistory().filter((x) => x.toLowerCase() !== normalizeQuery(query).toLowerCase())
  saveHistory(next); return next
}

function EntityCard({ item }) {
  const navigate = useNavigate()
  const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const label = type === 'artist' ? 'Artist' : type === 'album' ? 'Album' : type === 'jukebox' ? 'Jukebox' : 'Playlist'
  const image = getArtwork(item, 'large')
  const entityId = item.browseId || item.id
  const destination = type === 'artist' ? `/artist/${encodeURIComponent(entityId)}` : type === 'album' ? `/album/${encodeURIComponent(entityId)}` : (type === 'playlist' || type === 'jukebox') ? `/youtube-playlist/${encodeURIComponent(entityId)}` : null
  return <button type="button" disabled={!destination} onClick={() => destination && navigate(destination, { state: { name: item.title, artwork: image, browseId: entityId } })} className={`sf-entity-card group text-left min-w-0 ${destination ? 'cursor-pointer' : 'cursor-default'}`}>
    <div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">
      {image ? <img src={image} alt="" loading="lazy" decoding="async" className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full p-0.5' : ''}`} onError={(e) => { e.currentTarget.style.display = 'none' }} /> : <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued" /></div>}
    </div>
    <p className="font-bold text-sm truncate">{item.title || 'Untitled'}</p>
    <p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || label}</p>
    <span className="inline-flex items-center gap-1 mt-2 text-[10px] uppercase tracking-wider font-bold text-text-subdued"><Icon size={11}/> {label}</span>
  </button>
}

function EntityResults({ items }) {
  if (!items.length) return null
  return <div className="search-entity-grid">{items.map((item) => <EntityCard key={`${item.resultType}-${item.id}`} item={item}/>)}</div>
}

export default function Search() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const online = useOnlineStatus()
  const params = useMemo(() => new URLSearchParams(location.search), [location.search])
  const urlQuery = normalizeQuery(params.get('q'))
  const urlCategory = CATEGORIES.some((x) => x.id === params.get('category')) ? params.get('category') : 'all'
  const locationQuery = urlQuery || normalizeQuery(location.state?.query)

  const [query, setQuery] = useState(locationQuery)
  const [submittedQuery, setSubmittedQuery] = useState(locationQuery)
  const [category, setCategory] = useState(urlCategory)
  const [results, setResults] = useState(() => loadCache(locationQuery, urlCategory)?.results || [])
  const [batch, setBatch] = useState(() => loadCache(locationQuery, urlCategory)?.batch || 0)
  const [hasMore, setHasMore] = useState(() => !!loadCache(locationQuery, urlCategory)?.hasMore)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState(loadHistory)
  const [showAllHistory, setShowAllHistory] = useState(false)

  const abortRef = useRef(null)
  const serialRef = useRef(0)
  const loadingMoreRef = useRef(false)
  const sentinelRef = useRef(null)
  const resultsRef = useRef(results)
  const hasMoreRef = useRef(hasMore)
  const batchRef = useRef(batch)

  useEffect(() => { resultsRef.current = results }, [results])
  useEffect(() => { hasMoreRef.current = hasMore }, [hasMore])
  useEffect(() => { batchRef.current = batch }, [batch])

  const groups = useMemo(() => {
    const seen = new Set()
    const unique = results.filter((item) => {
      const key = `${item?.resultType || 'song'}:${item?.id || ''}`
      if (!item?.id || seen.has(key)) return false
      seen.add(key); return true
    })
    return {
      all: unique,
      songs: unique.filter((x) => x.resultType === 'song' || !x.resultType),
      albums: unique.filter((x) => x.resultType === 'album'),
      artists: unique.filter((x) => x.resultType === 'artist'),
      playlists: unique.filter((x) => x.resultType === 'playlist'),
      jukebox: unique.filter((x) => x.resultType === 'jukebox' || x.resultType === 'mix'),
    }
  }, [results])

  const loadPage = useCallback(async (qValue, categoryValue, pageNumber, replace = false) => {
    const q = normalizeQuery(qValue)
    if (!user || q.length < 2) return
    if (!navigator.onLine) { setError("You're offline — online search is unavailable right now."); return }
    if (pageNumber === 1) {
      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller
      serialRef.current += 1
      setLoading(true); setLoadingMore(false); setError(null)
      try {
        const page = await searchMusicPage(q, { category: categoryValue, batch: 1, signal: controller.signal })
        if (controller.signal.aborted) return
        const next = Array.isArray(page.results) ? page.results : []
        setResults(next); setBatch(1); setHasMore(!!page.hasMore && next.length > 0)
        saveCache(q, categoryValue, { ...page, results: next, batch: 1 })
        saveViewState({ query: q, category: categoryValue, results: next, batch: 1, hasMore: !!page.hasMore })
      } catch (err) {
        if (err?.name === 'AbortError') return
        setResults([]); setBatch(0); setHasMore(false); setError(err?.message || 'Unable to search right now. Please try again.')
      } finally { if (abortRef.current === controller) setLoading(false) }
      return
    }
    if (loadingMoreRef.current || !hasMoreRef.current) return
    loadingMoreRef.current = true; setLoadingMore(true); setError(null)
    const controller = new AbortController(); abortRef.current = controller
    const serial = serialRef.current
    try {
      const page = await searchMusicPage(q, { category: categoryValue, batch: pageNumber, signal: controller.signal })
      if (controller.signal.aborted || serial !== serialRef.current) return
      const incoming = Array.isArray(page.results) ? page.results : []
      const existing = new Set(resultsRef.current.map((x) => `${x?.resultType || 'song'}:${x?.id || ''}`))
      const fresh = incoming.filter((x) => { const key = `${x?.resultType || 'song'}:${x?.id || ''}`; if (!x?.id || existing.has(key)) return false; existing.add(key); return true })
      const merged = [...resultsRef.current, ...fresh]
      if (!fresh.length) { setHasMore(false); return }
      setResults(merged); setBatch(pageNumber); setHasMore(!!page.hasMore)
      saveCache(q, categoryValue, { ...page, results: merged, batch: pageNumber })
      saveViewState({ query: q, category: categoryValue, results: merged, batch: pageNumber, hasMore: !!page.hasMore })
    } catch (err) { if (err?.name !== 'AbortError') setError(err?.message || 'Unable to load more results.') }
    finally { loadingMoreRef.current = false; setLoadingMore(false) }
  }, [user])

  // URL is the source of truth for back/forward/deep-link navigation.
  useEffect(() => {
    const nextQ = locationQuery
    const changed = nextQ !== submittedQuery || urlCategory !== category
    if (!changed) return
    const cached = loadCache(nextQ, urlCategory)
    abortRef.current?.abort(); serialRef.current += 1
    setQuery(nextQ); setSubmittedQuery(nextQ); setCategory(urlCategory); setResults(cached?.results || []); setBatch(cached?.batch || 0); setHasMore(!!cached?.hasMore); setError(null)
    if (nextQ && !cached?.results?.length) loadPage(nextQ, urlCategory, 1, true)
  }, [locationQuery, urlCategory, submittedQuery, category, loadPage])

  useEffect(() => {
    if (!user || !submittedQuery || batch > 0) return
    loadPage(submittedQuery, category, 1, true)
  }, [user, submittedQuery, category, batch, loadPage])

  useEffect(() => () => abortRef.current?.abort(), [])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node || !submittedQuery || !hasMore) return undefined
    const root = node.closest('main') || null
    const observer = new IntersectionObserver((entries) => {
      if (!entries[0]?.isIntersecting || loadingMoreRef.current || !hasMoreRef.current) return
      loadPage(submittedQuery, category, batchRef.current + 1)
    }, { root, rootMargin: '900px 0px', threshold: 0 })
    observer.observe(node)
    return () => observer.disconnect()
  }, [submittedQuery, category, hasMore, loadPage])

  const suggestions = useMemo(() => {
    const q = normalizeQuery(query).toLowerCase()
    if (!q) return TRENDING_SUGGESTIONS
    return [...history, ...TRENDING_SUGGESTIONS].filter((x, i, a) => x.toLowerCase().includes(q) && a.findIndex((y) => y.toLowerCase() === x.toLowerCase()) === i).slice(0, 6)
  }, [query, history])
  const visibleHistory = showAllHistory ? history : history.slice(0, VISIBLE_HISTORY)

  const submitSearch = useCallback((value = query, nextCategory = category) => {
    const q = normalizeQuery(value)
    if (q.length < 2) { setError('Enter at least 2 characters to search.'); return }
    setHistory(addHistory(q)); setQuery(q); setSubmittedQuery(q); setResults([]); setBatch(0); setHasMore(false); setError(null)
    navigate(`/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(nextCategory)}`)
  }, [query, category, navigate])

  function selectCategory(next) {
    if (next === category) return
    const q = submittedQuery
    setCategory(next); setResults([]); setBatch(0); setHasMore(false); setError(null)
    if (!q) { navigate(`/search?category=${next}`, { replace: true }); return }
    navigate(`/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(next)}`, { replace: true })
    const cached = loadCache(q, next)
    if (cached?.results?.length) { setResults(cached.results); setBatch(cached.batch || 1); setHasMore(!!cached.hasMore) }
    else loadPage(q, next, 1, true)
  }

  function clearSearch() {
    abortRef.current?.abort(); serialRef.current += 1; setQuery(''); setSubmittedQuery(''); setResults([]); setBatch(0); setHasMore(false); setError(null); setLoading(false); setLoadingMore(false); navigate('/search', { replace: true })
  }

  if (!user) return <div className="p-4 md:p-6 flex flex-col items-center text-center pt-20"><SearchIcon size={40} className="text-text-subdued mb-4"/><h1 className="text-xl font-bold mb-2">Sign in to search</h1><p className="text-text-muted text-sm max-w-sm mb-6">Searching and streaming online songs requires a free account.</p><button onClick={() => navigate('/login')} className="sf-primary-button">Sign in with Google</button></div>

  const selectedLabel = CATEGORIES.find((x) => x.id === category)?.label || 'All'
  const visible = groups[category] || []

  return <div className="p-4 md:p-6 max-w-[1500px] mx-auto pb-32 min-w-0">
    <form onSubmit={(e) => { e.preventDefault(); submitSearch() }} className="relative max-w-3xl mb-4">
      <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black"/>
      <input autoFocus value={query} onChange={(e) => { setQuery(e.target.value); setError(null) }} placeholder="What do you want to play?" aria-label="Search music" className="w-full bg-white text-black placeholder-gray-600 rounded-full pl-11 pr-28 py-3.5 text-sm font-medium outline-none focus:ring-2 focus:ring-brand"/>
      {query && <button type="button" onClick={clearSearch} aria-label="Clear search" className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 p-2"><X size={16}/></button>}
      <button type="submit" disabled={normalizeQuery(query).length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button>
    </form>

    <div className="search-category-tabs" role="tablist" aria-label="Search category">{CATEGORIES.map(({ id, label, icon: Icon }) => <button key={id} type="button" role="tab" aria-selected={category === id} onClick={() => selectCategory(id)} className={`search-category-tab ${category === id ? 'is-active' : ''}`}><Icon size={16}/><span>{label}</span></button>)}</div>
    {!online && <div className="flex items-center gap-2 text-sm text-yellow-500 bg-yellow-500/10 rounded-xl px-3 py-2 mb-4 max-w-3xl"><WifiOff size={16}/> You're offline — online search is unavailable right now.</div>}

    {query.length >= 1 && suggestions.length > 0 && !loading && <div className="max-w-3xl mb-6"><p className="text-xs text-text-subdued uppercase tracking-widest font-bold mb-2">Suggestions</p><div className="flex flex-wrap gap-2">{suggestions.map((item) => <button key={item} type="button" onClick={() => submitSearch(item)} className="sf-history-chip"><SearchIcon size={12}/><span className="truncate max-w-[220px]">{item}</span></button>)}</div></div>}
    {!query && history.length > 0 && <section className="mb-8 max-w-4xl"><div className="flex items-center justify-between mb-3"><div><p className="text-sm font-semibold">Recent searches</p><p className="text-xs text-text-subdued mt-1">{history.length} saved</p></div><button type="button" onClick={() => { localStorage.removeItem(HISTORY_KEY); setHistory([]); setShowAllHistory(false) }} className="sf-ghost-button"><Trash2 size={14}/> Clear all</button></div><div className="flex flex-wrap gap-2">{visibleHistory.map((item) => <div key={item} className="sf-history-chip"><button type="button" onClick={() => submitSearch(item)} className="flex items-center gap-2 min-w-0"><Clock size={12}/><span className="truncate max-w-[220px]">{item}</span></button><button type="button" onClick={() => setHistory(removeHistory(item))} aria-label={`Remove ${item}`}><X size={12}/></button></div>)}</div>{history.length > VISIBLE_HISTORY && <button type="button" onClick={() => setShowAllHistory((x) => !x)} className="mt-3 text-xs text-brand flex items-center gap-1">{showAllHistory ? 'Show less' : `Show all ${history.length}`}<ArrowRight size={12}/></button>}</section>}

    {loading && <SkeletonRowList count={8}/>} 
    {error && !loading && <div className="flex items-center justify-between gap-3 bg-red-500/10 text-red-400 rounded-xl px-4 py-3 mb-4 max-w-3xl"><span className="text-sm">{error}</span>{submittedQuery && <button type="button" onClick={() => loadPage(submittedQuery, category, 1, true)} className="sf-ghost-button"><RotateCw size={14}/> Retry</button>}</div>}

    {!loading && !error && submittedQuery && batch > 0 && visible.length === 0 && <div className="text-center py-16"><SearchX size={40} className="mx-auto mb-3 text-text-subdued"/><p className="text-text-muted text-sm">No {selectedLabel.toLowerCase()} results found for “{submittedQuery}”.</p></div>}

    {visible.length > 0 && <section><div className="flex items-end justify-between mb-4 gap-3"><div><p className="text-xs text-brand uppercase tracking-widest font-bold">Search results</p><h2 className="text-2xl font-black mt-1">{selectedLabel}</h2></div><span className="text-xs text-text-subdued">{visible.length} loaded</span></div>
      {category === 'songs' ? <div className="flex flex-col">{groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs}/>)}</div> : category === 'all' ? <><EntityResults items={groups.artists}/><EntityResults items={groups.albums}/><EntityResults items={groups.playlists}/><EntityResults items={groups.jukebox}/>{groups.songs.length > 0 && <div className="mt-8 flex flex-col">{groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs}/>)}</div>}</> : <EntityResults items={visible}/>} 
    </section>}

    {submittedQuery && batch > 0 && <div ref={sentinelRef} className="search-load-more" aria-live="polite">{loadingMore && <><LoaderCircle size={18} className="animate-spin"/><span className="sr-only">Loading more results</span></>}</div>}
    {!query && history.length === 0 && <div className="py-14 text-center text-text-subdued text-sm">Search for a song, artist, album, playlist or jukebox.</div>}
  </div>
}
