import { useEffect, useMemo, useRef, useState, useCallback } from 'react'
import {
  Search as SearchIcon,
  WifiOff,
  Clock,
  X,
  SearchX,
  RotateCw,
  Trash2,
  ArrowRight,
  Disc3,
  UserRound,
  ListMusic,
  Radio,
  Music2,
  LoaderCircle,
} from 'lucide-react'
import { searchMusicPage } from '@/lib/search'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { useLocation, useNavigate } from 'react-router-dom'
import { getArtwork } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v2'
const MAX_HISTORY = 100
const VISIBLE_HISTORY = 10
const RESULT_CACHE_PREFIX = 'spotifusion:search-results:v3:'
const RESULT_CACHE_TTL = 30 * 60 * 1000

function searchCacheKey(query, category) {
  return `${RESULT_CACHE_PREFIX}${category}:${normalizeQuery(query).toLowerCase()}`
}

function loadResultCache(query, category) {
  if (!query) return null
  try {
    const raw = sessionStorage.getItem(searchCacheKey(query, category))
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || Date.now() - parsed.time > RESULT_CACHE_TTL || !Array.isArray(parsed.results)) {
      sessionStorage.removeItem(searchCacheKey(query, category))
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function saveResultCache(query, category, payload) {
  if (!query || !payload || !Array.isArray(payload.results)) return
  try {
    sessionStorage.setItem(searchCacheKey(query, category), JSON.stringify({
      time: Date.now(),
      results: payload.results,
      batch: payload.batch || 1,
      hasMore: Boolean(payload.hasMore),
    }))
  } catch {
    // Session storage is best-effort; search still works without it.
  }
}

const CATEGORIES = [
  { id: 'all', label: 'All', icon: SearchIcon },
  { id: 'songs', label: 'Songs', icon: Music2 },
  { id: 'albums', label: 'Albums', icon: Disc3 },
  { id: 'artists', label: 'Artists', icon: UserRound },
  { id: 'playlists', label: 'Playlists', icon: ListMusic },
  { id: 'jukebox', label: 'Jukebox', icon: Radio },
]

const TRENDING_SUGGESTIONS = [
  'Saiyaara',
  'Arijit Singh',
  'Anirudh Ravichander',
  'A.R. Rahman',
  'The Weeknd',
  'Taylor Swift',
]

function normalizeQuery(value) {
  return value.trim().replace(/\s+/g, ' ')
}

function loadHistory() {
  try {
    const stored = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    if (!Array.isArray(stored)) return []
    return stored
      .filter((x) => typeof x === 'string')
      .map(normalizeQuery)
      .filter((x) => x.length >= 2)
      .slice(0, MAX_HISTORY)
  } catch {
    return []
  }
}

function saveHistory(items) {
  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, MAX_HISTORY))) } catch {}
}

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

function EntityCard({ item }) {
  const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const label = type === 'artist' ? 'Artist' : type === 'album' ? 'Album' : type === 'jukebox' ? 'Jukebox' : 'Playlist'
  const image = getArtwork(item, 'large')
  const navigate = useNavigate()
  const entityId = item.browseId || item.id
  const destination = type === 'artist'
    ? `/artist/${encodeURIComponent(entityId)}`
    : type === 'album'
      ? `/album/${encodeURIComponent(entityId)}`
      : type === 'playlist' || type === 'jukebox'
        ? `/youtube-playlist/${encodeURIComponent(entityId)}`
        : null
  return (
    <button type="button" onClick={() => destination && navigate(destination, { state: { name: item.title, artwork: image, browseId: item.browseId || item.id } })} className={`sf-entity-card group text-left min-w-0 ${destination ? 'cursor-pointer' : 'cursor-default'}`}>
      <div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">
        {image ? (
          <img
            src={image}
            alt=""
            loading="lazy"
            decoding="async"
            className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full p-0.5' : ''}`}
          />
        ) : (
          <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued" /></div>
        )}
      </div>
      <p className="font-bold text-sm truncate">{item.title}</p>
      <p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || label}</p>
      <span className="inline-flex items-center gap-1 mt-2 text-[10px] uppercase tracking-wider font-bold text-text-subdued"><Icon size={11} /> {label}</span>
    </button>
  )
}

function EntityResults({ items }) {
  if (!items.length) return null
  return (
    <div className="search-entity-grid">
      {items.map((item) => <EntityCard key={`${item.resultType}-${item.id}`} item={item} />)}
    </div>
  )
}

export default function Search() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const online = useOnlineStatus()
  const params = new URLSearchParams(location.search)
  const urlQuery = normalizeQuery(params.get('q') || '')
  const urlCategory = CATEGORIES.some((item) => item.id === params.get('category')) ? params.get('category') : 'all'
  const initial = urlQuery || normalizeQuery(location.state?.query || '')
  const initialCache = loadResultCache(initial, urlCategory)

  const [query, setQuery] = useState(initial)
  const [submittedQuery, setSubmittedQuery] = useState(initial)
  const [category, setCategory] = useState(urlCategory)
  const [results, setResults] = useState(initialCache?.results || [])
  const [batch, setBatch] = useState(initialCache?.batch || 0)
  const [hasMore, setHasMore] = useState(Boolean(initialCache?.hasMore))
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState(loadHistory)
  const [showAllHistory, setShowAllHistory] = useState(false)

  const abortRef = useRef(null)
  const requestSerial = useRef(0)
  const sentinelRef = useRef(null)
  const loadingMoreRef = useRef(false)
  const resultsRef = useRef([])
  const hasMoreRef = useRef(false)

  const visibleHistory = useMemo(
    () => showAllHistory ? history : history.slice(0, VISIBLE_HISTORY),
    [history, showAllHistory],
  )

  const suggestions = useMemo(() => {
    const q = normalizeQuery(query).toLowerCase()
    if (!q) return TRENDING_SUGGESTIONS
    const source = [...history, ...TRENDING_SUGGESTIONS]
    return source.filter((item, index, list) =>
      item.toLowerCase().includes(q) &&
      list.findIndex((x) => x.toLowerCase() === item.toLowerCase()) === index,
    ).slice(0, 6)
  }, [query, history])

  useEffect(() => { resultsRef.current = results }, [results])
  useEffect(() => { hasMoreRef.current = hasMore }, [hasMore])

  const groups = useMemo(() => {
    const seen = new Set()
    const unique = results.filter((item) => {
      const key = `${item?.resultType || 'song'}:${item?.id || ''}`
      if (!item?.id || seen.has(key)) return false
      seen.add(key)
      return true
    })
    return {
      all: unique,
      songs: unique.filter((item) => item.resultType === 'song' || !item.resultType),
      albums: unique.filter((item) => item.resultType === 'album'),
      artists: unique.filter((item) => item.resultType === 'artist'),
      playlists: unique.filter((item) => item.resultType === 'playlist'),
      jukebox: unique.filter((item) => item.resultType === 'jukebox' || item.resultType === 'mix'),
    }
  }, [results])

  const loadBatch = useCallback(async (rawQuery, selectedCategory, nextBatch, replace = false) => {
    const q = normalizeQuery(rawQuery)
    if (!user || q.length < 2) return
    if (!navigator.onLine) {
      setError("You're offline — online search is unavailable right now.")
      return
    }

    if (nextBatch === 1) {
      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller
      requestSerial.current += 1
      setLoading(true)
      setLoadingMore(false)
      setError(null)
      try {
        const page = await searchMusicPage(q, { category: selectedCategory, batch: 1, signal: controller.signal })
        if (controller.signal.aborted) return
        const nextResults = Array.isArray(page.results) ? page.results : []
        setResults(nextResults)
        setBatch(1)
        setHasMore(Boolean(page.hasMore) && nextResults.length > 0)
        saveResultCache(q, selectedCategory, { ...page, results: nextResults, batch: 1 })
      } catch (err) {
        if (err?.name === 'AbortError') return
        setResults([])
        setBatch(0)
        setHasMore(false)
        setError(err?.message || 'Unable to search right now. Please try again.')
      } finally {
        if (abortRef.current === controller) setLoading(false)
      }
      return
    }

    if (loadingMoreRef.current || !hasMoreRef.current) return
    loadingMoreRef.current = true
    setLoadingMore(true)
    setError(null)
    const controller = new AbortController()
    abortRef.current = controller
    const serial = requestSerial.current
    try {
      const page = await searchMusicPage(q, { category: selectedCategory, batch: nextBatch, signal: controller.signal })
      if (controller.signal.aborted || serial !== requestSerial.current) return
      const incoming = Array.isArray(page.results) ? page.results : []
      const existing = new Set(resultsRef.current.map((item) => `${item?.resultType || 'song'}:${item?.id || ''}`))
      const fresh = incoming.filter((item) => {
        const key = `${item?.resultType || 'song'}:${item?.id || ''}`
        if (!item?.id || existing.has(key)) return false
        existing.add(key)
        return true
      })
      if (fresh.length > 0) {
        const merged = [...resultsRef.current, ...fresh]
        setResults(merged)
        setBatch(nextBatch)
        setHasMore(Boolean(page.hasMore))
        saveResultCache(q, selectedCategory, { ...page, results: merged, batch: nextBatch })
      } else {
        // The provider has no new continuation/results. Stop the observer so
        // we don't hammer the backend with identical empty pages.
        setHasMore(false)
      }
    } catch (err) {
      if (err?.name !== 'AbortError') setError(err?.message || 'Unable to load more results.')
    } finally {
      loadingMoreRef.current = false
      setLoadingMore(false)
    }
  }, [user])

  useEffect(() => {
    // Browser back/forward and deep links must restore the search instead of
    // leaving an empty page. Search state is represented in the URL and the
    // recent result cache makes returning from an album/playlist instant.
    const nextQuery = urlQuery || normalizeQuery(location.state?.query || '')
    if (nextQuery !== submittedQuery || urlCategory !== category) {
      const cached = loadResultCache(nextQuery, urlCategory)
      setQuery(nextQuery)
      setSubmittedQuery(nextQuery)
      setCategory(urlCategory)
      setResults(cached?.results || [])
      setBatch(cached?.batch || 0)
      setHasMore(Boolean(cached?.hasMore))
    }
  }, [urlQuery, urlCategory, location.state, submittedQuery, category])

  useEffect(() => {
    if (!user || !submittedQuery) return
    if (batch > 0 && results.length > 0) return
    loadBatch(submittedQuery, category, 1, true)
  }, [submittedQuery, category, user, loadBatch])

  useEffect(() => () => abortRef.current?.abort(), [])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node || !submittedQuery || !hasMore) return undefined
    const observer = new IntersectionObserver((entries) => {
      if (!entries[0]?.isIntersecting || loadingMoreRef.current) return
      loadBatch(submittedQuery, category, batch + 1)
    }, { rootMargin: '700px 0px' })
    observer.observe(node)
    return () => observer.disconnect()
  }, [submittedQuery, category, batch, hasMore, loadBatch])

  const submitSearch = useCallback((value = query) => {
    const q = normalizeQuery(value)
    if (q.length < 2) {
      setError('Enter at least 2 characters to search.')
      return
    }
    setHistory(addHistory(q))
    setSubmittedQuery(q)
    setResults([])
    setBatch(0)
    setHasMore(false)
    setError(null)
    navigate(`/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}`)
  }, [query, category, navigate])

  const clearSearch = () => {
    abortRef.current?.abort()
    requestSerial.current += 1
    setQuery('')
    setSubmittedQuery('')
    setResults([])
    setBatch(0)
    setHasMore(false)
    setError(null)
    setLoading(false)
    setLoadingMore(false)
    navigate('/search')
  }

  const selectCategory = (nextCategory) => {
    if (nextCategory === category) return
    setCategory(nextCategory)
    setResults([])
    setBatch(0)
    setHasMore(false)
    setError(null)
    const params = new URLSearchParams()
    if (submittedQuery) params.set('q', submittedQuery)
    params.set('category', nextCategory)
    navigate(`/search?${params.toString()}`)
  }

  const clearHistory = () => {
    localStorage.removeItem(HISTORY_KEY)
    setHistory([])
    setShowAllHistory(false)
  }

  if (!user) {
    return (
      <div className="p-4 md:p-6 flex flex-col items-center text-center pt-20">
        <SearchIcon size={40} className="text-text-subdued mb-4" />
        <h1 className="text-xl font-bold mb-2">Sign in to search</h1>
        <p className="text-text-muted text-sm max-w-sm mb-6">Searching and streaming online songs requires a free account.</p>
        <button onClick={signIn} className="sf-primary-button">Sign in with Google</button>
      </div>
    )
  }

  const selectedCategoryLabel = CATEGORIES.find((item) => item.id === category)?.label || 'All'

  return (
    <div className="p-4 md:p-6 max-w-[1500px] mx-auto pb-32">
      <form onSubmit={(event) => { event.preventDefault(); submitSearch() }} className="relative max-w-3xl mb-4">
        <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black" />
        <input
          autoFocus
          value={query}
          onChange={(event) => { setQuery(event.target.value); setError(null) }}
          placeholder="What do you want to play?"
          aria-label="Search for songs, artists, albums, playlists, or mixes"
          className="w-full bg-white text-black placeholder-gray-600 rounded-full pl-11 pr-28 py-3.5 text-sm font-medium outline-none focus:ring-2 focus:ring-brand"
        />
        {query && <button type="button" onClick={clearSearch} aria-label="Clear search" className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 hover:text-black p-2"><X size={16} /></button>}
        <button type="submit" disabled={normalizeQuery(query).length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button>
      </form>

      <div className="search-category-tabs" role="tablist" aria-label="Search category">
        {CATEGORIES.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={category === id}
            onClick={() => selectCategory(id)}
            className={`search-category-tab ${category === id ? 'is-active' : ''}`}
          >
            <Icon size={16} />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {!online && <div className="flex items-center gap-2 text-sm text-yellow-500 bg-yellow-500/10 rounded-xl px-3 py-2 mb-4 max-w-3xl"><WifiOff size={16} /> You're offline — online search is unavailable right now.</div>}

      {query.length >= 1 && suggestions.length > 0 && !loading && (
        <div className="max-w-3xl mb-6">
          <p className="text-xs text-text-subdued uppercase tracking-widest font-bold mb-2">Suggestions</p>
          <div className="flex flex-wrap gap-2">
            {suggestions.map((item) => (
              <button key={item} type="button" onClick={() => { setQuery(item); submitSearch(item) }} className="sf-history-chip">
                <SearchIcon size={12} /><span className="truncate max-w-[220px]">{item}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {!query && history.length > 0 && (
        <section className="mb-8 max-w-4xl">
          <div className="flex items-center justify-between mb-3">
            <div><p className="text-sm font-semibold">Recent searches</p><p className="text-xs text-text-subdued mt-1">{history.length} saved</p></div>
            <button type="button" onClick={clearHistory} className="sf-ghost-button"><Trash2 size={14} /> Clear all</button>
          </div>
          <div className="flex flex-wrap gap-2">
            {visibleHistory.map((item) => (
              <div key={item} className="sf-history-chip">
                <button type="button" onClick={() => { setQuery(item); submitSearch(item) }} className="flex items-center gap-2 min-w-0"><Clock size={12} /><span className="truncate max-w-[220px]">{item}</span></button>
                <button type="button" onClick={() => setHistory(removeHistory(item))} aria-label={`Remove ${item}`}><X size={12} /></button>
              </div>
            ))}
          </div>
          {history.length > VISIBLE_HISTORY && <button type="button" onClick={() => setShowAllHistory((value) => !value)} className="mt-3 text-xs text-brand flex items-center gap-1">{showAllHistory ? 'Show less' : `Show all ${history.length}`}<ArrowRight size={12} /></button>}
        </section>
      )}

      {loading && <SkeletonRowList count={8} />}
      {error && !loading && <div className="flex items-center justify-between gap-3 bg-red-500/10 text-red-400 rounded-xl px-4 py-3 mb-4 max-w-3xl"><span className="text-sm">{error}</span>{submittedQuery && <button type="button" onClick={() => loadBatch(submittedQuery, category, 1)} className="sf-ghost-button"><RotateCw size={14} /> Retry</button>}</div>}

      {!loading && !error && submittedQuery && groups[category].length === 0 && batch > 0 && (
        <div className="text-center py-16"><SearchX size={40} className="mx-auto mb-3 text-text-subdued" /><p className="text-text-muted text-sm">No {selectedCategoryLabel.toLowerCase()} results found for “{submittedQuery}”.</p></div>
      )}

      {groups[category].length > 0 && (
        <section>
          <div className="flex items-end justify-between mb-4">
            <div>
              <p className="text-xs text-brand uppercase tracking-widest font-bold">Search results</p>
              <h2 className="text-2xl font-black mt-1">{selectedCategoryLabel}</h2>
            </div>
            <span className="text-xs text-text-subdued">Batch {Math.max(1, batch)} · {groups[category].length} loaded</span>
          </div>

          {category === 'songs' ? (
            <div className="flex flex-col">
              {groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs} />)}
            </div>
          ) : category === 'all' ? (
            <>
              <EntityResults items={groups.artists} />
              <EntityResults items={groups.albums} />
              <EntityResults items={groups.playlists} />
              <EntityResults items={groups.jukebox} />
              {groups.songs.length > 0 && <div className="mt-8 flex flex-col">{groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs} />)}</div>}
            </>
          ) : (
            <EntityResults items={groups[category]} />
          )}
        </section>
      )}

      {submittedQuery && batch > 0 && (
        <div ref={sentinelRef} className="search-load-more" aria-live="polite">
          {loadingMore ? (
            <><LoaderCircle size={18} className="animate-spin" /><span>Loading batch {batch + 1}…</span></>
          ) : hasMore ? (
            <span>Scroll to load batch {batch + 1}</span>
          ) : (
            <span>End of available {selectedCategoryLabel.toLowerCase()} results</span>
          )}
        </div>
      )}

      {!query && history.length === 0 && <div className="py-14 text-center text-text-subdued text-sm">Search for a song, artist, album, playlist or jukebox.</div>}
    </div>
  )
}
